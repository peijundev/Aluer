package com.aluer.monitor;

import com.aluer.config.ServerGuardConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 服务端真实目录解析器。
 *
 * <p>解决「目录路径写死」问题：传统配置把 Minecraft 服务端工作目录固定为
 * {@code /opt/minecraft}，一旦服务端部署在外挂盘、自定义目录或以重命名启动器运行，
 * 注入与日志监控就会因路径不匹配而失效。</p>
 *
 * <p>本类统一解析「服务端真实工作目录」与「日志文件路径」，优先级如下：</p>
 * <ol>
 *   <li><b>显式配置</b>：{@code working-dir} / {@code log-path} 非空时尊重用户意图。</li>
 *   <li><b>自动寻找</b>：通过 {@link MinecraftProcessDetector} 定位运行中的服务端进程，
 *       读取其真实工作目录（{@code /proc/<pid>/cwd}），不写死任何路径。</li>
 *   <li><b>返回 null</b>：既无配置又无运行进程时，由调用方按「不可用」降级处理。</li>
 * </ol>
 */
@Component
public class ServerDirectoryResolver {

    private final ServerGuardConfig config;

    /** 无参构造（测试兼容）：使用默认配置，仅自动寻找路径 */
    public ServerDirectoryResolver() {
        this(new ServerGuardConfig());
    }

    @Autowired
    public ServerDirectoryResolver(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 解析服务端真实工作目录。
     *
     * <p>显式配置 {@code working-dir} 优先；否则回退到进程定位器发现的进程 cwd；
     * 两者都不可用时返回 null。</p>
     *
     * @return 服务端工作目录，无法定位返回 null
     */
    public Path resolveWorkingDir() {
        // 1. 显式配置优先：用户通过环境变量/yml 固定了目录时尊重其意图
        String configured = config.getMinecraft().getWorkingDir();
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        // 2. 自动寻找：从运行中的服务端进程读取真实工作目录，不写死路径
        String cwd = MinecraftProcessDetector.findProcessInfo(config)
                .map(MinecraftProcessDetector.ProcessInfo::getWorkingDirectory)
                .filter(s -> s != null && !s.isBlank())
                .orElse(null);
        if (cwd != null) {
            return Path.of(cwd);
        }
        return null;
    }

    /**
     * 解析服务端日志文件路径（{@code latest.log}）。
     *
     * <p>显式配置 {@code log-path} 优先；否则自动推导为「真实工作目录/logs/latest.log」；
     * 都不可用时返回 null。</p>
     *
     * @return 日志文件路径，无法定位返回 null
     */
    public Path resolveLogPath() {
        // 1. 显式配置优先
        String configured = config.getMonitor().getLogPath();
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        // 2. 自动推导：真实工作目录 + 标准日志子路径
        Path workingDir = resolveWorkingDir();
        return workingDir == null ? null : workingDir.resolve("logs").resolve("latest.log");
    }
}
