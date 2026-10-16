package com.aluer.deploy;

import com.aluer.config.ServerGuardConfig;
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

    /** Agent 最小依赖闭包：需要打包进插件 JAR 的类路径模式 */
    private static final String[] AGENT_CLASS_PATTERNS = {
            "classpath*:com/aluer/plugin/**/*.class",
            "classpath*:com/aluer/platform/**/*.class",
            "classpath*:com/aluer/agent/**/*.class",
            "classpath*:com/aluer/model/**/*.class",
    };

    private final ServerGuardConfig config;

    /** 无参构造（测试兼容） */
    public AgentDeployService() {
        this(new ServerGuardConfig());
    }

    @Autowired
    public AgentDeployService(ServerGuardConfig config) {
        this.config = config;
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
    }

    /**
     * 按当前配置注入 Agent 插件到 Minecraft 服务端 plugins 目录。
     */
    public DeployResult deploy() {
        String workingDir = config.getMinecraft().getWorkingDir();
        String jarName = config.getMinecraft().getAgentJarName();
        return deployTo(Path.of(workingDir), jarName);
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
