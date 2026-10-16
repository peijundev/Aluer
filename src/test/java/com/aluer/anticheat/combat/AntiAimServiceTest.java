package com.aluer.anticheat.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AntiAimServiceTest {

    @Test
    void normalRotationIsClean() {
        AntiAimService s = new AntiAimService();
        s.checkRotation("p", 0f);
        assertTrue(s.checkRotation("p", 10f).isClean());
    }

    @Test
    void snapRotationIsSuspicious() {
        AntiAimService s = new AntiAimService();
        s.checkRotation("p", 0f);
        // 单次 90 度瞬间转向
        assertTrue(s.checkRotation("p", 90f).isSuspicious());
    }

    @Test
    void extremeSnapIsFlaggedImmediately() {
        AntiAimService s = new AntiAimService();
        s.checkRotation("p", 0f);
        // 180 度掉头直接标记
        assertTrue(s.checkRotation("p", 180f).isFlagged());
    }

    @Test
    void repeatedSnapFlags() {
        AntiAimService s = new AntiAimService();
        s.checkRotation("p", 0f);
        s.checkRotation("p", 90f);   // 违规 1
        s.checkRotation("p", 180f);  // 违规 2
        assertTrue(s.checkRotation("p", 270f).isFlagged()); // 违规 3
    }

    @Test
    void wrapsAround360Correctly() {
        AntiAimService s = new AntiAimService();
        s.checkRotation("p", 0f);
        // 350 度等价于 -10 度，实际转向仅 10 度，应判定正常
        assertTrue(s.checkRotation("p", 350f).isClean());
    }
}
