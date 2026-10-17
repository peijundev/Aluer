package com.aluer.deploy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AgentDeployServiceTest {

    @Test
    void buildAgentJarGeneratesValidPluginJar(@TempDir Path tempDir) throws IOException {
        Path jar = tempDir.resolve("serverguard-agent.jar");
        AgentDeployService.buildAgentJar(jar);

        assertTrue(Files.exists(jar));
        try (JarFile jf = new JarFile(jar.toFile())) {
            Set<String> entries = jf.stream()
                    .map(JarEntry::getName)
                    .collect(Collectors.toSet());

            // 插件描述文件
            assertTrue(entries.contains("plugin.yml"), "应包含 plugin.yml");
            // 插件默认配置（saveDefaultConfig 依赖该资源）
            assertTrue(entries.contains("config.yml"), "应包含 config.yml");
            // Agent 入口与核心类
            assertTrue(entries.contains("com/aluer/plugin/AluerPlugin.class"));
            assertTrue(entries.contains("com/aluer/plugin/bridge/AgentWebSocketClient.class"));
            assertTrue(entries.contains("com/aluer/plugin/bridge/InternalCommandExecutor.class"));
            assertTrue(entries.contains("com/aluer/plugin/bridge/DataBridge.class"));
            // 平台兼容层
            assertTrue(entries.contains("com/aluer/platform/BukkitCompat.class"));
            assertTrue(entries.contains("com/aluer/platform/PlatformDetector.class"));
            // 通信协议与模型
            assertTrue(entries.contains("com/aluer/agent/AgentMessage.class"));
            assertTrue(entries.contains("com/aluer/model/AlertEvent.class"));
            assertTrue(entries.contains("com/aluer/model/MetricsData.class"));
        }
    }

    @Test
    void buildAgentJarExcludesTestClasses(@TempDir Path tempDir) throws IOException {
        Path jar = tempDir.resolve("serverguard-agent.jar");
        AgentDeployService.buildAgentJar(jar);

        try (JarFile jf = new JarFile(jar.toFile())) {
            Set<String> entries = jf.stream()
                    .map(JarEntry::getName)
                    .collect(Collectors.toSet());
            assertFalse(entries.stream().anyMatch(e -> e.endsWith("Test.class")),
                    "插件 JAR 不应包含测试类");
        }
    }

    @Test
    void collectAgentClassNamesContainsCoreClassesAndNoTests() {
        List<String> names = AgentDeployService.collectAgentClassNames();

        assertTrue(names.contains("com/aluer/plugin/AluerPlugin.class"));
        assertTrue(names.contains("com/aluer/platform/BukkitCompat.class"));
        assertTrue(names.contains("com/aluer/agent/AgentMessage.class"));
        assertTrue(names.contains("com/aluer/model/AlertEvent.class"));
        assertFalse(names.stream().anyMatch(n -> n.endsWith("Test.class")),
                "不应包含测试类");
        assertFalse(names.isEmpty());
    }

    @Test
    void deployToCreatesPluginsDirAndJar(@TempDir Path tempDir) {
        AgentDeployService service = new AgentDeployService();
        AgentDeployService.DeployResult result = service.deployTo(tempDir, "serverguard-agent.jar");

        assertTrue(result.isSuccess());
        assertTrue(Files.exists(tempDir.resolve("plugins/serverguard-agent.jar")));
        assertTrue(result.getTargetPath().contains("serverguard-agent.jar"));
    }

    @Test
    void deployToFailsWhenWorkingDirMissing(@TempDir Path tempDir) {
        AgentDeployService service = new AgentDeployService();
        Path missing = tempDir.resolve("not-exists");
        AgentDeployService.DeployResult result = service.deployTo(missing, "serverguard-agent.jar");

        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());
    }

    @Test
    void deployToFailsWhenJarNameBlank(@TempDir Path tempDir) {
        AgentDeployService service = new AgentDeployService();
        AgentDeployService.DeployResult result = service.deployTo(tempDir, "");

        assertFalse(result.isSuccess());
    }
}
