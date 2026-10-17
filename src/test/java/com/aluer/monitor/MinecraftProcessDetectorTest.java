package com.aluer.monitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftProcessDetectorTest {

    // ─── normalizeJarName ──────────────────────────────

    @Test
    void normalizeStripsBuildNumber() {
        assertEquals("paper-1.21.11", MinecraftProcessDetector.normalizeJarName("paper-1.21.11-113.jar"));
    }

    @Test
    void normalizeStripsJarExtension() {
        assertEquals("paper-1.21.11", MinecraftProcessDetector.normalizeJarName("paper-1.21.11.jar"));
    }

    @Test
    void normalizeStripsPath() {
        assertEquals("paper-1.21.11",
                MinecraftProcessDetector.normalizeJarName("/opt/minecraft/paper-1.21.11-113.jar"));
    }

    @Test
    void normalizeKeepsVersionWithDots() {
        // 版本号含点（1.12.2）不应被当作 build 号剥离
        assertEquals("spigot-1.12.2", MinecraftProcessDetector.normalizeJarName("spigot-1.12.2.jar"));
    }

    @Test
    void normalizeHandlesNullOrBlank() {
        assertEquals("", MinecraftProcessDetector.normalizeJarName(null));
        assertEquals("", MinecraftProcessDetector.normalizeJarName(""));
    }

    // ─── jarNamesMatch ─────────────────────────────────

    @Test
    void jarNamesMatchIgnoresBuildNumber() {
        // 配置去掉了 build 号，真实进程带 build 号，应判定相等（核心修复点）
        assertTrue(MinecraftProcessDetector.jarNamesMatch("paper-1.21.11.jar", "paper-1.21.11-113.jar"));
    }

    @Test
    void jarNamesMatchExactAndWithPath() {
        assertTrue(MinecraftProcessDetector.jarNamesMatch("paper-1.21.11.jar", "paper-1.21.11.jar"));
        assertTrue(MinecraftProcessDetector.jarNamesMatch("/opt/minecraft/paper-1.21.11.jar", "paper-1.21.11-113.jar"));
    }

    @Test
    void jarNamesMatchRejectsDifferentVersionOrVendor() {
        assertFalse(MinecraftProcessDetector.jarNamesMatch("paper-1.21.11.jar", "paper-1.20.4.jar"));
        assertFalse(MinecraftProcessDetector.jarNamesMatch("paper-1.21.11.jar", "spigot-1.21.11.jar"));
        assertFalse(MinecraftProcessDetector.jarNamesMatch("", "paper.jar"));
        assertFalse(MinecraftProcessDetector.jarNamesMatch(null, "paper.jar"));
    }

    // ─── extractJarName ────────────────────────────────

    @Test
    void extractJarFromJarArgumentWithPath() {
        assertEquals("paper-1.21.11-113.jar",
                MinecraftProcessDetector.extractJarName("java -Xms4G -jar /opt/minecraft/paper-1.21.11-113.jar nogui"));
    }

    @Test
    void extractJarFromSimpleJarArgument() {
        assertEquals("paper.jar", MinecraftProcessDetector.extractJarName("java -jar paper.jar"));
    }

    @Test
    void extractJarReturnsNullWithoutJarArgument() {
        assertNull(MinecraftProcessDetector.extractJarName("java -cp app.jar Main"));
        assertNull(MinecraftProcessDetector.extractJarName(null));
        assertNull(MinecraftProcessDetector.extractJarName(""));
    }

    // ─── commandLineMatches ────────────────────────────

    @Test
    void commandLineMatchesByProcessName() {
        assertTrue(MinecraftProcessDetector.commandLineMatches(
                "java -jar paper.jar", "paper", "paper-1.21.11.jar"));
    }

    @Test
    void commandLineMatchesByExactJarFile() {
        assertTrue(MinecraftProcessDetector.commandLineMatches(
                "java -jar paper-1.21.11.jar nogui", "minecraft", "paper-1.21.11.jar"));
    }

    @Test
    void commandLineMatchesBuildNumberMismatch() {
        // 核心修复点：配置无 build 号，命令行有 build 号，仍应匹配
        assertTrue(MinecraftProcessDetector.commandLineMatches(
                "java -Xms4G -Xmx4G -jar paper-1.21.11-113.jar nogui",
                "minecraft",
                "paper-1.21.11.jar"));
    }

    @Test
    void commandLineMatchesRejectsUnrelatedProcess() {
        assertFalse(MinecraftProcessDetector.commandLineMatches(
                "java -jar velocity-3.3.0.jar", "minecraft", "paper-1.21.11.jar"));
    }

    @Test
    void commandLineMatchesRejectsBlankCommandLine() {
        assertFalse(MinecraftProcessDetector.commandLineMatches("", "minecraft", "paper.jar"));
        assertFalse(MinecraftProcessDetector.commandLineMatches(null, "minecraft", "paper.jar"));
    }

    // ─── looksLikeMinecraftServer（通用识别） ──────────

    @Test
    void looksLikeMinecraftServerRecognizesKnownCores() {
        assertTrue(MinecraftProcessDetector.looksLikeMinecraftServer("java -jar paper-1.21.1-116.jar nogui"));
        assertTrue(MinecraftProcessDetector.looksLikeMinecraftServer("java -jar spigot-1.12.2.jar nogui"));
        assertTrue(MinecraftProcessDetector.looksLikeMinecraftServer("java -jar purpur-1.21.1.jar"));
        assertTrue(MinecraftProcessDetector.looksLikeMinecraftServer("java -jar folia-1.21.1.jar"));
        assertTrue(MinecraftProcessDetector.looksLikeMinecraftServer("java -jar fabric-server-launch.jar"));
        assertTrue(MinecraftProcessDetector.looksLikeMinecraftServer("java @args -jar forge-1.20.1-shim.jar"));
    }

    @Test
    void looksLikeMinecraftServerRejectsNonMinecraft() {
        assertFalse(MinecraftProcessDetector.looksLikeMinecraftServer("java -jar velocity-3.3.0.jar"));
        assertFalse(MinecraftProcessDetector.looksLikeMinecraftServer("java -jar myapp.jar"));
        assertFalse(MinecraftProcessDetector.looksLikeMinecraftServer("java Main"));
        assertFalse(MinecraftProcessDetector.looksLikeMinecraftServer(null));
        assertFalse(MinecraftProcessDetector.looksLikeMinecraftServer(""));
    }

    // ─── 空关键词通用识别 ──────────────────────────────

    @Test
    void commandLineMatchesWithBlankKeywordsUsesGenericDetection() {
        // 未配置任何关键词时，按已知核心名自动识别
        assertTrue(MinecraftProcessDetector.commandLineMatches(
                "java -jar paper-1.21.1-116.jar nogui", "", ""));
        assertTrue(MinecraftProcessDetector.commandLineMatches(
                "java -jar fabric-server-launch.jar", null, null));
    }

    @Test
    void commandLineMatchesWithBlankKeywordsRejectsProxy() {
        assertFalse(MinecraftProcessDetector.commandLineMatches(
                "java -jar velocity-3.3.0.jar", "", ""));
    }

    @Test
    void commandLineMatchesWithExplicitKeywordDoesNotUseGenericDetection() {
        // 显式配置了关键词（velocity），即使命令行是 paper 服务器，也应精确匹配失败而不兜底通用识别
        assertFalse(MinecraftProcessDetector.commandLineMatches(
                "java -jar paper-1.21.1.jar nogui", "velocity", "velocity-3.3.0.jar"));
    }

    // ─── 工作目录特征文件兜底（自动发现兜底层） ────────

    @Test
    void hasMinecraftServerFilesDetectsMarkerFile(@TempDir Path tempDir) throws Exception {
        // 空目录不应识别
        assertFalse(MinecraftProcessDetector.hasMinecraftServerFiles(tempDir));
        // 出现 server.properties 即识别为 Minecraft 服务器
        Files.writeString(tempDir.resolve("server.properties"), "server-port=25565\n");
        assertTrue(MinecraftProcessDetector.hasMinecraftServerFiles(tempDir));
    }

    @Test
    void hasMinecraftServerFilesDetectsNestedLatestLog(@TempDir Path tempDir) throws Exception {
        Files.createDirectories(tempDir.resolve("logs"));
        Files.writeString(tempDir.resolve("logs/latest.log"), "[Server thread/INFO]: Done");
        assertTrue(MinecraftProcessDetector.hasMinecraftServerFiles(tempDir));
    }

    @Test
    void hasMinecraftServerFilesDetectsFabricLoaderLog(@TempDir Path tempDir) throws Exception {
        // 混合端（Fabric/NeoForge/Youer）常以 fabricloader.log 作为启动痕迹
        Files.writeString(tempDir.resolve("fabricloader.log"), "Loading");
        assertTrue(MinecraftProcessDetector.hasMinecraftServerFiles(tempDir));
    }

    @Test
    void hasMinecraftServerFilesRejectsNull() {
        assertFalse(MinecraftProcessDetector.hasMinecraftServerFiles(null));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void resolveWorkingDirectoryReturnsPathForCurrentProcess() {
        String cwd = MinecraftProcessDetector.resolveWorkingDirectory(ProcessHandle.current());
        assertNotNull(cwd);
        assertFalse(cwd.isBlank());
    }
}
