package com.aluer.anticheat.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AntiBadPacketsServiceTest {

    // ─── BadPacketsD：非法俯仰角 ──────────────────────

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

    // ─── 非法坐标 ─────────────────────────────────────

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

    // ─── BadPacketsA：重复槽位 ────────────────────────

    @Test
    void duplicateHeldSlotFlagged() {
        AntiBadPacketsService s = new AntiBadPacketsService();
        assertTrue(s.checkHeldItemSlot("p", 0).isClean());   // 首次
        assertTrue(s.checkHeldItemSlot("p", 1).isClean());   // 切换
        assertTrue(s.checkHeldItemSlot("p", 1).isFlagged()); // 重复
    }

    // ─── BadPacketsC：未睡起床 ────────────────────────

    @Test
    void leaveBedWhileAwakeFlagged() {
        AntiBadPacketsService s = new AntiBadPacketsService();
        assertTrue(s.checkLeaveBed("p", false).isFlagged());
        assertTrue(s.checkLeaveBed("p", true).isClean());
    }

    // ─── BadPacketsE：无位置更新 ──────────────────────

    @Test
    void noPositionUpdateFlagsAfterThreshold() {
        AntiBadPacketsService s = new AntiBadPacketsService();
        for (int i = 0; i < 20; i++) {
            assertTrue(s.checkMovementUpdate("p", false).isClean());
        }
        // 第 21 次无位置更新触发标记
        assertTrue(s.checkMovementUpdate("p", false).isFlagged());
    }

    @Test
    void positionUpdateResetsCounter() {
        AntiBadPacketsService s = new AntiBadPacketsService();
        for (int i = 0; i < 20; i++) {
            s.checkMovementUpdate("p", false);
        }
        // 一次位置更新后计数器归零
        assertTrue(s.checkMovementUpdate("p", true).isClean());
        assertTrue(s.checkMovementUpdate("p", false).isClean());
    }

    // ─── BadPacketsF：重复疾跑状态 ────────────────────

    @Test
    void duplicateSprintStateFlagged() {
        AntiBadPacketsService s = new AntiBadPacketsService();
        assertTrue(s.checkSprintAction("p", true).isClean());    // 首次 START
        assertTrue(s.checkSprintAction("p", false).isClean());   // STOP
        assertTrue(s.checkSprintAction("p", false).isFlagged()); // 重复 STOP
    }
}
