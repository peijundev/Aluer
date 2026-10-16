package com.aluer.anticheat.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AntiBadPacketsServiceTest {

    @Test
    void invalidPitchFlagged() {
        AntiBadPacketsService s = new AntiBadPacketsService();
        assertTrue(s.checkRotation("p", 0f, 120f).isFlagged());
        assertTrue(s.checkRotation("p", 0f, -120f).isFlagged());
    }

    @Test
    void invalidYawFlagged() {
        AntiBadPacketsService s = new AntiBadPacketsService();
        assertTrue(s.checkRotation("p", 500f, 0f).isFlagged());
        assertTrue(s.checkRotation("p", Float.NaN, 0f).isFlagged());
    }

    @Test
    void nanAndInfinityPositionFlagged() {
        AntiBadPacketsService s = new AntiBadPacketsService();
        assertTrue(s.checkPosition("p", Double.NaN, 0, 0).isFlagged());
        assertTrue(s.checkPosition("p", 0, Double.POSITIVE_INFINITY, 0).isFlagged());
        assertTrue(s.checkPosition("p", 0, 0, Double.NEGATIVE_INFINITY).isFlagged());
    }

    @Test
    void validValuesAreClean() {
        AntiBadPacketsService s = new AntiBadPacketsService();
        assertTrue(s.checkRotation("p", 180f, 45f).isClean());
        assertTrue(s.checkPosition("p", 10.0, 64.0, -20.0).isClean());
    }
}
