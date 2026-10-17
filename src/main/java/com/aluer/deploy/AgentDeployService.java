package com.aluer.deploy;

import com.aluer.config.ServerGuardConfig;
import com.aluer.monitor.ServerDirectoryResolver;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Agent 插件自动注入服务。
 *
 * <p>ServerGuard 采用「外部引擎 + 游戏内 Agent」双体架构：引擎运行在宿主机，
 * Agent（{@code com.aluer.plugin.AluerPlugin}）作为 Bukkit 插件运行在游戏服务器内。
 * 传统部署要求人工把 Agent JAR 复制到服务器 {@code plugins/} 目录，本服务将其自动化。</p>
 *
 * <p><b>为什么需要重新打包</b>：引擎产物是 Spring Boot fat jar，其类位于
 * {@code BOOT-INF/classes/} 下，而 Bukkit 的插件类加载器只识别 jar 根目录下的标准类结构，
 * 因此 fat jar 无法直接被服务器加载。本服务在运行时把 Agent 及其最小依赖闭包
 * （{@code com.aluer.plugin / platform / agent / model} 包）重新打包成标准结构的插件 JAR。</p>
 *
 * <p><b>依赖说明</b>：Agent 依赖的 gson、slf4j-api、Bukkit/Adventure API 均由
 * 游戏服务器运行时提供，无需打包进插件 JAR。</p>
 */
@Component
public class AgentDeployService {

    private static final Logger logger = LoggerFactory.getLogger(AgentDeployService.class);

    /** Agent 插件描述文件 */
    private static final String PLUGIN_YML = "plugin.yml";

    /** Agent 插件默认配置文件（server-url 等运行时参数） */
    private static final String CONFIG_YML = "config.yml";

    /** Agent 最小依赖闭包：需要打包进插件 JAR 的类路径模式 */
    private static final String[] AGENT_CLASS_PATTERNS = {
            "classpath*:com/aluer/plugin/**/*.class",
            "classpath*:com/aluer/platform/**/*.class",
            "classpath*:com/aluer/agent/**/*.class",
            "classpath*:com/aluer/model/**/*.class",
    };

    private final ServerGuardConfig config;
    private final ServerDirectoryResolver directoryResolver;

    /** 周期补注入执行器（守护线程，避免阻塞引擎正常关闭） */
    private final ScheduledExecutorService retryScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "agent-deploy-retry");
                t.setDaemon(true);
                return t;
            });

    /** 无参构造（测试兼容） */
    public AgentDeployService() {
        this(new ServerGuardConfig());
    }

    @Autowired
    public AgentDeployService(ServerGuardConfig config, ServerDirectoryResolver directoryResolver) {
        this.config = config;
        this.directoryResolver = directoryResolver;
    }

    /** 仅注入配置的构造（测试兼容）：使用独立的目录解析器 */
    public AgentDeployService(ServerGuardConfig config) {
        this(config, new ServerDirectoryResolver(config));
    }

    /**
     * 引擎启动时自动注入（若配置启用）。
     *
     * <p>仅做「复制插件 JAR 到 plugins 目录」这一无损动作，不主动重启服务器；
     * 工作目录不存在时仅记录日志，不影响引擎正常启动。</p>
     */
    @PostConstruct
    public void autoDeploy() {
        if (!config.getMinecraft().isAutoInject()) {
            logger.info("Agent 自动注入已禁用（serverguard.minecraft.auto-inject=false）");
            return;
        }
        DeployResult result = deploy();
        if (result.isSuccess()) {
            logger.info("Agent 插件已自动注入到服务端: {}（重启服务端后生效）", result.getTargetPath());
        } else {
            logger.info("Agent 自动注入未执行: {}", result.getMessage());
        }

        // 启动周期补注入：首轮注入失败（典型场景是引擎先于服务端启动、进程定位器
        // 找不到工作目录）时，后续仍有机会在服务端运行后补上，避免 Agent 永久缺失。
        long interval = Math.max(1, config.getMinecraft().getCheckIntervalSeconds());
        retryScheduler.scheduleWithFixedDelay(this::retryDeploy, interval, interval, TimeUnit.SECONDS);
    }

    /**
     * 周期补注入：解决「引擎先于服务端启动」的时序错位。
     *
     * <p>引擎启动时服务端可能尚未运行，{@link #autoDeploy()} 首轮注入会因无法定位
     * 工作目录而失败；此后若服务端才启动，引擎不会再自动注入，Agent 插件就永远缺失。
     * 本方法按 {@code check-interval-seconds} 周期检查：当服务端已运行且 plugins/ 下
     * 尚无 Agent 插件时补注入一次，其余情况（服务端未运行 / 已注入）静默跳过。</p>
     */
    private void retryDeploy() {
        try {
            Path workingDir = directoryResolver.resolveWorkingDir();
            if (!needsDeploy(workingDir, config.getMinecraft().getAgentJarName())) {
                // 服务端未运行 或 Agent 已就位，静默跳过本轮
                return;
            }
            DeployResult result = deployTo(workingDir, config.getMinecraft().getAgentJarName());
            if (result.isSuccess()) {
                logger.info("Agent 插件已补注入到服务端: {}（重启服务端后生效）", result.getTargetPath());
            } else {
                logger.warn("Agent 插件补注入失败: {}", result.getMessage());
            }
        } catch (Exception e) {
            // 周期任务不能因单次异常而终止整个调度
            logger.warn("Agent 周期补注入检查异常: {}", e.getMessage());
        }
    }

    /**
     * 判断是否需要（补）注入 Agent 插件（纯逻辑，便于测试）。
     *
     * <p>工作目录为 null（服务端尚未运行）或 plugins/ 下已有 Agent JAR 时返回 false，
     * 周期补注入据此静默跳过；仅当服务端已运行且 Agent 缺失时返回 true。</p>
     */
    static boolean needsDeploy(Path workingDir, String agentJarName) {
        if (workingDir == null) {
            return false;
        }
        if (agentJarName == null || agentJarName.isBlank()) {
            return false;
        }
        return !Files.isRegularFile(workingDir.resolve("plugins").resolve(agentJarName));
    }

    /**
     * 按当前配置注入 Agent 插件到 Minecraft 服务端 plugins 目录。
     *
     * <p>服务端工作目录通过 {@link ServerDirectoryResolver} 自动寻找：
     * 显式配置优先，否则回退到进程定位器发现的真实目录，避免写死路径。</p>
     */
    public DeployResult deploy() {
        Path workingDir = directoryResolver.resolveWorkingDir();
        String jarName = config.getMinecraft().getAgentJarName();
        if (workingDir == null) {
            return DeployResult.failure("无法定位服务端工作目录（进程未运行且未配置 working-dir）");
        }
        return deployTo(workingDir, jarName);
    }

    /**
     * 将 Agent 插件 JAR 部署到指定工作目录的 plugins/ 子目录。
     *
     * @param workingDir   Minecraft 服务端工作目录（含 plugins/ 子目录）
     * @param agentJarName 目标插件 JAR 文件名
     * @return 部署结果
     */
    public DeployResult deployTo(Path workingDir, String agentJarName) {
        if (workingDir == null || !Files.isDirectory(workingDir)) {
            return DeployResult.failure("Minecraft 工作目录不存在: " + workingDir);
        }
        if (agentJarName == null || agentJarName.isBlank()) {
            return DeployResult.failure("Agent JAR 文件名为空");
        }
        try {
            Path pluginsDir = workingDir.resolve("plugins");
            Files.createDirectories(pluginsDir);
            Path target = pluginsDir.resolve(agentJarName);
            buildAgentJar(target);
            return DeployResult.success(target);
        } catch (Exception e) {
            logger.error("Agent 插件注入失败", e);
            return DeployResult.failure("注入失败: " + e.getMessage());
        }
    }

    // ─── Agent JAR 打包（可测试） ─────────────────────

    /**
     * 从当前 classpath 收集 Agent 相关类，生成标准结构的插件 JAR。
     *
     * <p>产物包含 {@code plugin.yml} 与 Agent 最小依赖闭包的全部 {@code .class} 文件，
     * 类路径保持 jar 根目录下的标准布局，可被 Bukkit 插件类加载器直接加载。</p>
     */
    static void buildAgentJar(Path outputJar) throws IOException {
        List<String> classNames = collectAgentClassNames();

        // 先写临时文件，成功后原子替换，避免写入不完整的 JAR
        Path tmp = outputJar.resolveSibling(outputJar.getFileName() + ".tmp");
        Files.deleteIfExists(tmp);
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(tmp))) {
            // 插件描述文件
            byte[] pluginYml = readClasspathResource(PLUGIN_YML);
            if (pluginYml != null) {
                jos.putNextEntry(new JarEntry(PLUGIN_YML));
                jos.write(pluginYml);
                jos.closeEntry();
            }
            // 插件默认配置：AluerPlugin.onEnable 调用 saveDefaultConfig() 需从 JAR 读取 config.yml，
            // 缺该资源会抛 IllegalArgumentException 导致插件启用失败，故必须随 JAR 一并打包。
            byte[] configYml = readClasspathResource(CONFIG_YML);
            if (configYml != null) {
                jos.putNextEntry(new JarEntry(CONFIG_YML));
                jos.write(configYml);
                jos.closeEntry();
            }
            // 类文件
            for (String className : classNames) {
                byte[] bytes = readClasspathResource(className);
                if (bytes == null) {
                    continue;
                }
                jos.putNextEntry(new JarEntry(className));
                jos.write(bytes);
                jos.closeEntry();
            }
        }
        Files.move(tmp, outputJar, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 枚举 Agent 依赖闭包内的全部类资源路径（含内部类，排除测试类）。
     */
    static List<String> collectAgentClassNames() {
        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver(AgentDeployService.class.getClassLoader());
        List<String> names = new ArrayList<>();
        for (String pattern : AGENT_CLASS_PATTERNS) {
            try {
                for (Resource resource : resolver.getResources(pattern)) {
                    String rel = classpathRelativePath(resource);
                    if (rel == null || !rel.endsWith(".class")) {
                        continue;
                    }
                    // 排除测试类，避免把 JUnit 测试类打包进插件 JAR
                    if (rel.endsWith("Test.class") || rel.endsWith("Tests.class")) {
                        continue;
                    }
                    names.add(rel);
                }
            } catch (IOException e) {
                logger.warn("枚举 Agent 类失败 {}: {}", pattern, e.getMessage());
            }
        }
        return names;
    }

    /**
     * 从资源 URL 提取 classpath 相对路径（如 {@code com/aluer/plugin/AluerPlugin.class}）。
     *
     * <p>兼容目录（{@code target/classes}）与 Spring Boot fat jar（{@code BOOT-INF/classes!}）两种布局，
     * 统一通过定位 {@code com/aluer/} 前缀截取相对路径。</p>
     */
    static String classpathRelativePath(Resource resource) {
        try {
            String url = resource.getURL().toString();
            int idx = url.indexOf("com/aluer/");
            return idx >= 0 ? url.substring(idx) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static byte[] readClasspathResource(String path) {
        try (InputStream in = AgentDeployService.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    // ─── 结果类 ────────────────────────────────────────

    /** Agent 注入结果 */
    public static class DeployResult {
        private final boolean success;
        private final String targetPath;
        private final String message;

        private DeployResult(boolean success, String targetPath, String message) {
            this.success = success;
            this.targetPath = targetPath;
            this.message = message;
        }

        public static DeployResult success(Path target) {
            return new DeployResult(true, target.toString(), "已部署到 " + target);
        }

        public static DeployResult failure(String message) {
            return new DeployResult(false, null, message);
        }

        public boolean isSuccess() { return success; }
        public String getTargetPath() { return targetPath; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return success ? message : "注入失败: " + message;
        }
    }
}
