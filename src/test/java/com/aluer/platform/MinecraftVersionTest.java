package com.aluer.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftVersionTest {

    @Test
    void parsesPlainVersion() {
        MinecraftVersion v = MinecraftVersion.parse("1.12.2");
        assertNotNull(v);
        assertEquals(1, v.getMajor());
        assertEquals(12, v.getMinor());
        assertEquals(2, v.getPatch());
    }

    @Test
    void parsesVersionFromServerString() {
        // 典型 Paper 服务端版本字符串
        MinecraftVersion v = MinecraftVersion.parse("git-Paper-1.21.1 (MC: 1.21.1)");
        assertNotNull(v);
        assertEquals(1, v.getMajor());
        assertEquals(21, v.getMinor());
        assertEquals(1, v.getPatch());
    }

    @Test
    void parsesTwoSegmentVersion() {
        MinecraftVersion v = MinecraftVersion.parse("1.21");
        assertNotNull(v);
        assertEquals(0, v.getPatch());
    }

    @Test
    void returnsNullForUnparseable() {
        assertNull(MinecraftVersion.parse(""));
        assertNull(MinecraftVersion.parse(null));
        assertNull(MinecraftVersion.parse("no-version-here"));
    }

    @Test
    void comparesVersionsCorrectly() {
        assertTrue(MinecraftVersion.parse("1.21.1").isAtLeast(1, 12, 2));
        assertTrue(MinecraftVersion.parse("1.12.2").isAtLeast(1, 12, 2));
        assertFalse(MinecraftVersion.parse("1.12.1").isAtLeast(1, 12, 2));
        assertTrue(MinecraftVersion.parse("1.20").isAtLeast(1, 16, 5));
    }

    @Test
    void supportedRangeIsFromOneTwelveTwo() {
        assertTrue(MinecraftVersion.parse("1.12.2").isSupported());
        assertTrue(MinecraftVersion.parse("1.21.1").isSupported());
        assertFalse(MinecraftVersion.parse("1.8.9").isSupported());
        assertFalse(MinecraftVersion.parse("1.7.10").isSupported());
    }

    @Test
    void adventureAvailableFromOneSixteenFive() {
        assertTrue(MinecraftVersion.parse("1.16.5").hasAdventure());
        assertTrue(MinecraftVersion.parse("1.21.1").hasAdventure());
        assertFalse(MinecraftVersion.parse("1.12.2").hasAdventure());
    }

    @Test
    void equalityAndHashCode() {
        assertEquals(MinecraftVersion.parse("1.12.2"), new MinecraftVersion(1, 12, 2));
        assertEquals(MinecraftVersion.parse("1.12.2").hashCode(), new MinecraftVersion(1, 12, 2).hashCode());
        assertNotEquals(MinecraftVersion.parse("1.12.2"), MinecraftVersion.parse("1.12.3"));
    }
}
