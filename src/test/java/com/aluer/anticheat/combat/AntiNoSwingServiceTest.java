package com.aluer.anticheat.combat;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AntiNoSwingServiceTest {

    @Test
    void attackWithRecentSwingIsClean() {
        AntiNoSwingService s = new AntiNoSwingService();
        s.recordSwing("p", Instant.ofEpochMilli(1000));
        assertTrue(s.checkAttack("p", "target", Instant.ofEpochMilli(1050)).isClean());
    }

    @Test
    void attackWithoutSwingIsSuspicious() {
        AntiNoSwingService s = new AntiNoSwingService();
        s.recordSwing("p", Instant.ofEpochMilli(1000));
        // 挥臂后 1000ms 才攻击，远超窗口
        assertTrue(s.checkAttack("p", "target", Instant.ofEpochMilli(2000)).isSuspicious());
    }

    @Test
    void repeatedNoSwingFlags() {
        AntiNoSwingService s = new AntiNoSwingService();
        s.recordSwing("p", Instant.ofEpochMilli(0));
        s.checkAttack("p", "t", Instant.ofEpochMilli(1000));
        s.checkAttack("p", "t", Instant.ofEpochMilli(2000));
        assertTrue(s.checkAttack("p", "t", Instant.ofEpochMilli(3000)).isFlagged());
    }

    @Test
    void firstActionWithoutRecordIsClean() {
        AntiNoSwingService s = new AntiNoSwingService();
        // 从未挥臂，首次动作不判定
        assertTrue(s.checkBlockBreak("p", "STONE", Instant.ofEpochMilli(1000)).isClean());
    }

    @Test
    void blockBreakAndPlaceAlsoChecked() {
        AntiNoSwingService s = new AntiNoSwingService();
        s.recordSwing("p", Instant.ofEpochMilli(1000));
        assertTrue(s.checkBlockBreak("p", "STONE", Instant.ofEpochMilli(2000)).isSuspicious());
        assertTrue(s.checkBlockPlace("p", "STONE", Instant.ofEpochMilli(3000)).isSuspicious());
    }
}
