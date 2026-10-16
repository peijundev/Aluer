package com.aluer.anticheat.player;

import com.aluer.config.ServerGuardConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 非法数据包（BadPackets）检测服务 — V6.0 开源反作弊融合模块
 *
 * <p>检测算法<b>移植自 GrimAC 的 BadPackets 检查系列</b>（本项目已采用 GPL-3.0，
 * 与 Grim 许可证兼容）。Grim 的原始实现基于 PacketEvents 数据包事件，本服务将其
 * 翻译为「接收 Agent 结构化数据」的等价状态机检测：</p>
 *
 * <ul>
 *   <li><b>BadPacketsA</b>（重复槽位）：连续发送相同的 held item slot 变更。</li>
 *   <li><b>BadPacketsC</b>（未睡起床）：不在床上却发送 LEAVE_BED 起床动作。</li>
 *   <li><b>BadPacketsD</b>（非法俯仰角）：pitch 越界（&gt;90 或 &lt;-90）。</li>
 *   <li><b>BadPacketsE</b>（无位置更新）：连续移动包超过阈值 tick 却不更新坐标。</li>
 *   <li><b>BadPacketsF</b>（重复疾跑）：连续发送相同的 sprint 状态切换。</li>
 *   <li><b>非法坐标</b>：NaN / Infinity 坐标（畸形数据包）。</li>
 * </ul>
 *
 * <p>配置开关：serverguard.security.super-evolution.anti-bad-packets</p>
 */
@Service
public class AntiBadPacketsService {

    private final ServerGuardConfig config;

    /** 每个玩家最近一次 held item slot（BadPacketsA 重复槽位检测） */
    private final Map<String, Integer> lastHeldSlot = new ConcurrentHashMap<>();

    /** 每个玩家当前 sprint 状态（BadPacketsF 重复疾跑检测） */
    private final Map<String, Boolean> lastSprinting = new ConcurrentHashMap<>();

    /** 每个玩家连续无位置更新的移动包计数（BadPacketsE） */
    private final Map<String, Integer> noPositionTicks = new ConcurrentHashMap<>();

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong flaggedCount = new AtomicLong(0);

    /** BadPacketsE：允许的最大无位置更新移动包数（tick） */
    private static final int MAX_NO_POSITION_TICKS = 20;

    /** 无参构造（测试兼容） */
    public AntiBadPacketsService() {
        this(new ServerGuardConfig());
    }

    public AntiBadPacketsService(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * BadPacketsD + 扩展：校验玩家旋转数据包（yaw / pitch）。
     *
     * <p>合法客户端 pitch 不会超出 [-90, 90]，yaw 不会超出 [-360, 360]。</p>
     */
    public BadPacketResult checkRotation(String playerName, float yaw, float pitch) {
        if (!config.getSecurity().getSuperEvolution().isAntiBadPackets()) {
            return BadPacketResult.clean();
        }
        totalChecks.incrementAndGet();

        List<String> reasons = new ArrayList<>();
        if (Float.isNaN(pitch) || Float.isInfinite(pitch) || pitch > 90.0f || pitch < -90.0f) {
            reasons.add("INVALID_PITCH: " + pitch);
        }
        if (Float.isNaN(yaw) || Float.isInfinite(yaw) || Math.abs(yaw) > 360.0f) {
            reasons.add("INVALID_YAW: " + yaw);
        }
        return reasons.isEmpty() ? BadPacketResult.clean() : flag(reasons);
    }

    /**
     * 非法坐标检测：NaN / Infinity 坐标是畸形数据包。
     */
    public BadPacketResult checkPosition(String playerName, double x, double y, double z) {
        if (!config.getSecurity().getSuperEvolution().isAntiBadPackets()) {
            return BadPacketResult.clean();
        }
        totalChecks.incrementAndGet();

        List<String> reasons = new ArrayList<>();
        if (Double.isNaN(x) || Double.isInfinite(x)) reasons.add("INVALID_POSITION_X: " + x);
        if (Double.isNaN(y) || Double.isInfinite(y)) reasons.add("INVALID_POSITION_Y: " + y);
        if (Double.isNaN(z) || Double.isInfinite(z)) reasons.add("INVALID_POSITION_Z: " + z);
        return reasons.isEmpty() ? BadPacketResult.clean() : flag(reasons);
    }

    /**
     * BadPacketsA：重复槽位检测。
     *
     * <p>合法客户端只在切换槽位时发送 held item change，若连续两次发送相同槽位，
     * 说明客户端伪造了重复数据包。</p>
     *
     * @param playerName 玩家名称
     * @param slot       当前手持槽位（0~8）
     */
    public BadPacketResult checkHeldItemSlot(String playerName, int slot) {
        if (!config.getSecurity().getSuperEvolution().isAntiBadPackets()) {
            return BadPacketResult.clean();
        }
        totalChecks.incrementAndGet();

        Integer prev = lastHeldSlot.put(playerName, slot);
        if (prev != null && prev == slot) {
            return flag(List.of("DUPLICATE_HELD_SLOT: " + slot));
        }
        return BadPacketResult.clean();
    }

    /**
     * BadPacketsC：未睡起床检测。
     *
     * <p>玩家未处于睡觉状态却发送 LEAVE_BED 起床动作，是伪造数据包。</p>
     *
     * @param playerName 玩家名称
     * @param inBed      玩家当前是否在床上睡觉
     */
    public BadPacketResult checkLeaveBed(String playerName, boolean inBed) {
        if (!config.getSecurity().getSuperEvolution().isAntiBadPackets()) {
            return BadPacketResult.clean();
        }
        totalChecks.incrementAndGet();

        if (!inBed) {
            return flag(List.of("LEAVE_BED_WHILE_AWAKE"));
        }
        return BadPacketResult.clean();
    }

    /**
     * BadPacketsE：无位置更新检测。
     *
     * <p>客户端需要周期性发送含坐标的移动包。若连续超过阈值 tick 只发送旋转/状态
     * 移动包而不更新坐标，说明客户端在伪造移动数据。</p>
     *
     * @param playerName  玩家名称
     * @param hasPosition 本次移动包是否包含坐标更新
     */
    public BadPacketResult checkMovementUpdate(String playerName, boolean hasPosition) {
        if (!config.getSecurity().getSuperEvolution().isAntiBadPackets()) {
            return BadPacketResult.clean();
        }
        totalChecks.incrementAndGet();

        if (hasPosition) {
            noPositionTicks.remove(playerName);
            return BadPacketResult.clean();
        }
        int ticks = noPositionTicks.merge(playerName, 1, Integer::sum);
        if (ticks > MAX_NO_POSITION_TICKS) {
            return flag(List.of("NO_POSITION_UPDATE: " + ticks + " ticks"));
        }
        return BadPacketResult.clean();
    }

    /**
     * BadPacketsF：重复疾跑状态检测。
     *
     * <p>合法客户端只在状态变化时发送 sprint 切换，若连续发送相同的 start/stop 状态，
     * 说明客户端伪造了重复状态包。</p>
     *
     * @param playerName     玩家名称
     * @param startSprinting 是否开始疾跑（true=START，false=STOP）
     */
    public BadPacketResult checkSprintAction(String playerName, boolean startSprinting) {
        if (!config.getSecurity().getSuperEvolution().isAntiBadPackets()) {
            return BadPacketResult.clean();
        }
        totalChecks.incrementAndGet();

        Boolean prev = lastSprinting.put(playerName, startSprinting);
        if (prev != null && prev == startSprinting) {
            return flag(List.of("DUPLICATE_SPRINT_STATE: " + startSprinting));
        }
        return BadPacketResult.clean();
    }

    /** 玩家离线时清理追踪状态 */
    public void clearPlayer(String playerName) {
        lastHeldSlot.remove(playerName);
        lastSprinting.remove(playerName);
        noPositionTicks.remove(playerName);
    }

    /** 获取模块运行状态 */
    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("enabled", config.getSecurity().getSuperEvolution().isAntiBadPackets());
        s.put("totalChecks", totalChecks.get());
        s.put("flaggedCount", flaggedCount.get());
        s.put("trackedPlayers", lastHeldSlot.size());
        return s;
    }

    private BadPacketResult flag(List<String> reasons) {
        flaggedCount.incrementAndGet();
        return BadPacketResult.flagged(reasons);
    }

    /** 非法数据包检测结果 */
    public static class BadPacketResult {
        private final boolean flagged;
        private final List<String> reasons;

        private BadPacketResult(boolean flagged, List<String> reasons) {
            this.flagged = flagged;
            this.reasons = reasons;
        }

        public static BadPacketResult clean() {
            return new BadPacketResult(false, List.of());
        }

        public static BadPacketResult flagged(List<String> reasons) {
            return new BadPacketResult(true, reasons);
        }

        public boolean isFlagged() { return flagged; }
        public boolean isClean() { return !flagged; }
        public List<String> getReasons() { return reasons; }
    }
}
