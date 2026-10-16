package com.aluer.monitor;

import org.junit.jupiter.api.Test;

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
}
