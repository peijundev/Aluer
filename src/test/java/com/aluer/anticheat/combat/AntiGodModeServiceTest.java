package com.aluer.anticheat.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AntiGodModeServiceTest {

    @Test
    void normalDamageIsClean() {
        AntiGodModeService s = new AntiGodModeService();
        s.checkDamage("p", 2.0, 20.0); // 首次记录
        assertTrue(s.checkDamage("p", 2.0, 18.0).isClean()); // 正常掉血
    }

    @Test
    void noHealthLossIsSuspiciousThenFlagged() {
        AntiGodModeService s = new AntiGodModeService();
        s.checkDamage("p", 2.0, 20.0); // 首次
        assertTrue(s.checkDamage("p", 2.0, 20.0).isSuspicious()); // 受击不掉血 1
        assertTrue(s.checkDamage("p", 2.0, 20.0).isSuspicious()); // 2
        assertTrue(s.checkDamage("p", 2.0, 20.0).isFlagged());    // 3
    }

    @Test
    void noDamageIsClean() {
        AntiGodModeService s = new AntiGodModeService();
        s.checkDamage("p", 2.0, 20.0);
        assertTrue(s.checkDamage("p", 0.0, 20.0).isClean()); // 无伤害不检测
    }

    @Test
    void healthLossResetsCounter() {
        AntiGodModeService s = new AntiGodModeService();
        s.checkDamage("p", 2.0, 20.0);
        s.checkDamage("p", 2.0, 20.0); // 不掉血 1
        assertTrue(s.checkDamage("p", 2.0, 18.0).isClean()); // 掉血重置
        assertTrue(s.checkDamage("p", 2.0, 18.0).isSuspicious()); // 重新计数 1
    }
}
