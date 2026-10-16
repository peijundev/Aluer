package com.aluer.anticheat.combat;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AntiFastHealServiceTest {

    @Test
    void normalRegenIntervalIsClean() {
        AntiFastHealService s = new AntiFastHealService();
        s.checkRegen("p", 10.0, Instant.ofEpochMilli(0));
        // 4 秒后回血，间隔正常
        assertTrue(s.checkRegen("p", 10.5, Instant.ofEpochMilli(4000)).isClean());
    }

    @Test
    void fastRegenIsSuspiciousThenFlagged() {
        AntiFastHealService s = new AntiFastHealService();
        s.checkRegen("p", 10.0, Instant.ofEpochMilli(0));
        assertTrue(s.checkRegen("p", 10.5, Instant.ofEpochMilli(1000)).isClean());      // 首次回血
        assertTrue(s.checkRegen("p", 11.0, Instant.ofEpochMilli(2000)).isSuspicious()); // 违规 1
        assertTrue(s.checkRegen("p", 11.5, Instant.ofEpochMilli(3000)).isSuspicious()); // 违规 2
        assertTrue(s.checkRegen("p", 12.0, Instant.ofEpochMilli(4000)).isFlagged());    // 违规 3
    }

    @Test
    void healthLossIsClean() {
        AntiFastHealService s = new AntiFastHealService();
        s.checkRegen("p", 10.0, Instant.ofEpochMilli(0));
        // 生命值下降（掉血），不检测
        assertTrue(s.checkRegen("p", 9.0, Instant.ofEpochMilli(1000)).isClean());
    }

    @Test
    void normalIntervalResetsViolations() {
        AntiFastHealService s = new AntiFastHealService();
        s.checkRegen("p", 10.0, Instant.ofEpochMilli(0));
        s.checkRegen("p", 10.5, Instant.ofEpochMilli(1000)); // 快速 1
        s.checkRegen("p", 11.0, Instant.ofEpochMilli(2000)); // 快速 2
        // 正常间隔回血重置计数
        assertTrue(s.checkRegen("p", 11.5, Instant.ofEpochMilli(6000)).isClean());
    }
}
