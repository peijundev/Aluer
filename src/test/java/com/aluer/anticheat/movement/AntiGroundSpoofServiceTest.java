package com.aluer.anticheat.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AntiGroundSpoofServiceTest {

    @Test
    void onGroundWithStablePositionIsClean() {
        AntiGroundSpoofService s = new AntiGroundSpoofService();
        assertTrue(s.checkGroundStatus("p", true, 64.0, 64.0).isClean());
    }

    @Test
    void onGroundFallingIsSuspiciousThenFlagged() {
        AntiGroundSpoofService s = new AntiGroundSpoofService();
        assertTrue(s.checkGroundStatus("p", true, 64.0, 63.5).isSuspicious()); // 地面但下降 0.5
        s.checkGroundStatus("p", true, 63.5, 63.0);
        assertTrue(s.checkGroundStatus("p", true, 63.0, 62.5).isFlagged());
    }

    @Test
    void notOnGroundFallingIsClean() {
        AntiGroundSpoofService s = new AntiGroundSpoofService();
        // 未声明在地面，自由落体是正常的
        assertTrue(s.checkGroundStatus("p", false, 64.0, 63.0).isClean());
    }

    @Test
    void stablePositionResetsViolations() {
        AntiGroundSpoofService s = new AntiGroundSpoofService();
        s.checkGroundStatus("p", true, 64.0, 63.5); // 违规 1
        s.checkGroundStatus("p", true, 63.5, 63.0); // 违规 2
        assertTrue(s.checkGroundStatus("p", true, 63.0, 63.0).isClean()); // 稳定重置
        assertTrue(s.checkGroundStatus("p", true, 63.0, 62.5).isSuspicious()); // 重新计数 1
    }
}
