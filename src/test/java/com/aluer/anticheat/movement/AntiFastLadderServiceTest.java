package com.aluer.anticheat.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AntiFastLadderServiceTest {

    @Test
    void normalClimbingIsClean() {
        AntiFastLadderService s = new AntiFastLadderService();
        assertTrue(s.checkLadderMovement("p", 64.0, 64.2, true).isClean());
    }

    @Test
    void fastClimbingIsSuspiciousThenFlagged() {
        AntiFastLadderService s = new AntiFastLadderService();
        // 单 tick 上升 1 格，远超 0.4 阈值
        assertTrue(s.checkLadderMovement("p", 64.0, 65.0, true).isSuspicious());
        s.checkLadderMovement("p", 65.0, 66.0, true);
        assertTrue(s.checkLadderMovement("p", 66.0, 67.0, true).isFlagged());
    }

    @Test
    void notClimbingIsClean() {
        AntiFastLadderService s = new AntiFastLadderService();
        // 非攀爬状态即使大位移也不判定
        assertTrue(s.checkLadderMovement("p", 64.0, 70.0, false).isClean());
    }

    @Test
    void downwardClimbingIsClean() {
        AntiFastLadderService s = new AntiFastLadderService();
        assertTrue(s.checkLadderMovement("p", 65.0, 64.0, true).isClean());
    }
}
