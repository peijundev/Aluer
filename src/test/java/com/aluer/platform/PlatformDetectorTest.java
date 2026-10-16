package com.aluer.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatformDetectorTest {

    @Test
    void detectsPaperFromVersionString() {
        assertEquals(PlatformType.PAPER,
                PlatformDetector.detectType("git-Paper-1.21.1 (MC: 1.21.1)", ""));
    }

    @Test
    void detectsSpigotFromVersionString() {
        assertEquals(PlatformType.SPIGOT,
                PlatformDetector.detectType("git-Spigot-1.12.2 (MC: 1.12.2)", ""));
    }

    @Test
    void detectsVanillaFromPackageName() {
        assertEquals(PlatformType.VANILLA,
                PlatformDetector.detectType("", "org.bukkit.craftbukkit.v1_20_R1"));
    }

    @Test
    void detectsPurpurBeforePaper() {
        // Purpur 版本字符串同时含 "purpur" 与 "paper"，应优先判定为 Purpur
        assertEquals(PlatformType.PURPUR,
                PlatformDetector.detectType("git-Purpur-1.21.1 (MC: 1.21.1)", ""));
    }

    @Test
    void detectsFoliaBeforePaper() {
        assertEquals(PlatformType.FOLIA,
                PlatformDetector.detectType("git-Folia-1.21.1 (MC: 1.21.1)", ""));
    }

    @Test
    void detectsGlowstone() {
        assertEquals(PlatformType.GLOWSTONE,
                PlatformDetector.detectType("Glowstone 1.20.1", ""));
    }

    @Test
    void unknownForUnrecognizedInput() {
        assertEquals(PlatformType.UNKNOWN, PlatformDetector.detectType("", ""));
        assertEquals(PlatformType.UNKNOWN, PlatformDetector.detectType("mystery-server", ""));
    }

    @Test
    void detectsProxyAndModdedPlatformsFromClassName() {
        assertEquals(PlatformType.VELOCITY, PlatformDetector.detectFromClassName("com.velocitypowered.api.proxy.ProxyServer"));
        assertEquals(PlatformType.WATERFALL, PlatformDetector.detectFromClassName("io.github.waterfallmc.waterfall.ProxyServer"));
        assertEquals(PlatformType.BUNGEECORD, PlatformDetector.detectFromClassName("net.md_5.bungee.api.ProxyServer"));
        assertEquals(PlatformType.NEOFORGE, PlatformDetector.detectFromClassName("net.neoforged.fml.ModLoader"));
        assertEquals(PlatformType.FABRIC, PlatformDetector.detectFromClassName("net.fabricmc.api.ModInitializer"));
        assertEquals(PlatformType.SPONGE, PlatformDetector.detectFromClassName("org.spongepowered.api.Server"));
        assertEquals(PlatformType.FORGE, PlatformDetector.detectFromClassName("net.minecraftforge.common.MinecraftForge"));
    }

    @Test
    void platformTypeCategoryFlags() {
        assertTrue(PlatformType.PAPER.isBukkitFamily());
        assertTrue(PlatformType.SPIGOT.isBukkitFamily());
        assertTrue(PlatformType.FOLIA.isBukkitFamily());
        assertFalse(PlatformType.PAPER.isProxy());
        assertFalse(PlatformType.PAPER.isModded());

        assertTrue(PlatformType.BUNGEECORD.isProxy());
        assertTrue(PlatformType.VELOCITY.isProxy());
        assertFalse(PlatformType.BUNGEECORD.isBukkitFamily());

        assertTrue(PlatformType.FORGE.isModded());
        assertTrue(PlatformType.FABRIC.isModded());
        assertFalse(PlatformType.FORGE.isBukkitFamily());
    }

    @Test
    void detectReturnsSupportedBukkitPlatformInTestEnvironment() {
        // 测试环境无真实 Bukkit 服务器，detect() 应安全回退而非抛异常
        ServerPlatform platform = PlatformDetector.detect();
        assertNotNull(platform);
        assertNotNull(platform.platformName());
        assertNotNull(platform.rawVersion());
    }
}
