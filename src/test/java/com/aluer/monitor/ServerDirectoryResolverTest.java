package com.aluer.monitor;

import com.aluer.config.ServerGuardConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ServerDirectoryResolverTest {

    @Test
    void resolveWorkingDirPrefersExplicitConfig(@TempDir Path tempDir) {
        // 显式配置 working-dir 时优先使用，不依赖运行中的进程
        ServerGuardConfig config = new ServerGuardConfig();
        config.getMinecraft().setWorkingDir(tempDir.toString());
        ServerDirectoryResolver resolver = new ServerDirectoryResolver(config);
        assertEquals(tempDir.toAbsolutePath(), resolver.resolveWorkingDir().toAbsolutePath());
    }

    @Test
    void resolveLogPathPrefersExplicitConfig(@TempDir Path tempDir) {
        // 显式配置 log-path 时优先使用
        ServerGuardConfig config = new ServerGuardConfig();
        config.getMonitor().setLogPath(tempDir.resolve("custom.log").toString());
        ServerDirectoryResolver resolver = new ServerDirectoryResolver(config);
        assertEquals(tempDir.resolve("custom.log").toAbsolutePath(),
                resolver.resolveLogPath().toAbsolutePath());
    }

    @Test
    void resolveLogPathDerivesFromWorkingDir(@TempDir Path tempDir) {
        // 显式 working-dir + 空 log-path → 自动推导 workingDir/logs/latest.log
        ServerGuardConfig config = new ServerGuardConfig();
        config.getMinecraft().setWorkingDir(tempDir.toString());
        ServerDirectoryResolver resolver = new ServerDirectoryResolver(config);
        assertEquals(tempDir.resolve("logs").resolve("latest.log").toAbsolutePath(),
                resolver.resolveLogPath().toAbsolutePath());
    }
}
