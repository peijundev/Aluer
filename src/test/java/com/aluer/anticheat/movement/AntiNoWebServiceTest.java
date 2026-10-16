package com.aluer.anticheat.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AntiNoWebServiceTest {

    @Test
    void normalWebMovementIsClean() {
        AntiNoWebService s = new AntiNoWebService();
        // 蜘蛛网内缓慢移动（正常减速）
        assertTrue(s.checkMovement("p", 0, 0, 0.03, 0, true).isClean());
    }

    @Test
    void fullSpeedInWebIsSuspiciousThenFlagged() {
        AntiNoWebService s = new AntiNoWebService();
        // 蜘蛛网内全速移动（0.2 blocks/tick）
        assertTrue(s.checkMovement("p", 0, 0, 0.2, 0, true).isSuspicious());
        s.checkMovement("p", 0, 0, 0.2, 0, true);
        assertTrue(s.checkMovement("p", 0, 0, 0.2, 0, true).isFlagged());
    }

    @Test
    void notInWebIsClean() {
        AntiNoWebService s = new AntiNoWebService();
        // 不在蜘蛛网内，即使全速也不检测
        assertTrue(s.checkMovement("p", 0, 0, 0.3, 0, false).isClean());
    }

    @Test
    void normalMovementResetsViolations() {
        AntiNoWebService s = new AntiNoWebService();
        s.checkMovement("p", 0, 0, 0.2, 0, true); // 超速 1
        s.checkMovement("p", 0, 0, 0.2, 0, true); // 超速 2
        // 正常减速移动重置
        assertTrue(s.checkMovement("p", 0, 0, 0.02, 0, true).isClean());
        assertTrue(s.checkMovement("p", 0, 0, 0.2, 0, true).isSuspicious()); // 重新计数 1
    }
}
