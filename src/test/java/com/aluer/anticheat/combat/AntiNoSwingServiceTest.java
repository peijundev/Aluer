package com.aluer.anticheat.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AntiNoSwingServiceTest {

    @Test
    void attackAfterSwingIsClean() {
        AntiNoSwingService s = new AntiNoSwingService();
        s.recordSwing("p");
        assertTrue(s.checkAttack("p", "target").isClean());
    }

    @Test
    void blockBreakAndPlaceAlsoChecked() {
        AntiNoSwingService s = new AntiNoSwingService();
        s.recordSwing("p");
        assertTrue(s.checkBlockBreak("p", "STONE").isClean());
        s.recordSwing("p");
        assertTrue(s.checkBlockPlace("p", "STONE").isClean());
    }

    @Test
    void repeatedNoSwingFlags() {
        AntiNoSwingService s = new AntiNoSwingService();
        s.checkAttack("p", "t"); // VL 1
        s.checkAttack("p", "t"); // VL 2
        s.checkAttack("p", "t"); // VL 3
        assertTrue(s.checkAttack("p", "t").isFlagged()); // VL 4
    }

    @Test
    void swingDecaysViolationLevel() {
        AntiNoSwingService s = new AntiNoSwingService();
        s.checkAttack("p", "t"); // VL 1
        s.checkAttack("p", "t"); // VL 2
        s.checkAttack("p", "t"); // VL 3
        s.recordSwing("p");
        // 挥臂后攻击，VL 衰减到 2.7，不会立即标记
        assertTrue(s.checkAttack("p", "t").isClean());
        assertFalse(s.checkAttack("p", "t").isFlagged());
    }

    @Test
    void firstActionWithoutSwingIsSuspicious() {
        AntiNoSwingService s = new AntiNoSwingService();
        // 从未挥臂就攻击，首次 VL=1 为可疑（对齐 NCP 语义）
        assertTrue(s.checkAttack("p", "t").isSuspicious());
    }
}
