package com.aluer.anticheat.combat;

import com.aluer.config.ServerGuardConfig;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 瞬间转向（Aim / AimAssist）检测服务 — V6.0 开源反作弊融合模块
 *
 * <p>检测算法参考 Kauri 的 Aim 检查与 GrimAC 的 AimModulo360 检查（GPL-3.0 兼容）：</p>
 * <ul>
 *   <li><b>snap 检测</b>（{@link #checkRotation}）：追踪连续两次旋转，归一化偏航角差超过
 *       人体上限即视为瞬间转向（Aimbot snap）。</li>
 *   <li><b>Modulo 360 检测</b>（{@link #checkModulo360}）：移植 Grim AimModulo360 ——
 *       客户端单次上报的原始偏航角变化接近一整圈（&gt;320°）且上次变化极小（&lt;30°），
 *       说明作弊客户端用 yaw 环绕 360° 来掩盖大角度瞬间转向。</li>
 * </ul>
 *
 * <p>配置开关：serverguard.security.super-evolution.anti-aim</p>
 */
@Service
public class AntiAimService {

    private final ServerGuardConfig config;

    /** 每个玩家最近一次偏航角（playerName → yaw） */
    private final Map<String, Float> lastYaw = new ConcurrentHashMap<>();

    /** 每个玩家最近一次原始偏航角变化量（playerName → deltaYaw，用于 Modulo 360 检测） */
    private final Map<String, Float> lastDeltaYaw = new ConcurrentHashMap<>();

    /** 每个玩家的连续瞬间转向违规次数（playerName → 次数） */
    private final Map<String, Integer> violations = new ConcurrentHashMap<>();

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong flaggedCount = new AtomicLong(0);

    /** snap 检测：单次归一化偏航角变化阈值（度） */
    private static final double SNAP_THRESHOLD = 60.0;

    /** 极端掉头阈值（度）—— 单次达到即直接标记 */
    private static final double EXTREME_THRESHOLD = 180.0;

    /** 连续瞬间转向次数阈值 */
    private static final int VIOLATION_THRESHOLD = 3;

    /** Modulo 360 检测：原始偏航角变化阈值（度） */
    private static final float MODULO_360_THRESHOLD = 320.0f;

    /** Modulo 360 检测：上次变化量上限（度） */
    private static final float MODULO_360_LAST_MAX = 30.0f;

    /** 无参构造（测试兼容） */
    public AntiAimService() {
        this(new ServerGuardConfig());
    }

    public AntiAimService(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 检测玩家视角旋转的瞬间转向（snap）。
     *
     * @param playerName 玩家名称
     * @param yaw        当前水平偏航角（度）
     * @return 检测结果
     */
    public AimCheckResult checkRotation(String playerName, float yaw) {
        if (!config.getSecurity().getSuperEvolution().isAntiAim()) {
            return AimCheckResult.clean();
        }
        totalChecks.incrementAndGet();

        Float prev = lastYaw.put(playerName, yaw);
        if (prev == null) {
            // 首次记录，无法计算变化量
            return AimCheckResult.clean();
        }

        double delta = Math.abs(yaw - prev);
        // 偏航角存在 360° 环绕，归一化到 [0, 180]
        if (delta > 180.0) {
            delta = 360.0 - delta;
        }

        if (delta <= SNAP_THRESHOLD) {
            violations.remove(playerName);
            return AimCheckResult.clean();
        }
        if (delta >= EXTREME_THRESHOLD) {
            flaggedCount.incrementAndGet();
            return AimCheckResult.flagged(List.of("AIM_SNAP: 单次转向 " + String.format("%.1f", delta) + " 度"));
        }

        int v = violations.merge(playerName, 1, Integer::sum);
        String reason = "AIM_SNAP: 单次转向 " + String.format("%.1f", delta) + " 度（阈值 " + SNAP_THRESHOLD + "）";
        if (v >= VIOLATION_THRESHOLD) {
            flaggedCount.incrementAndGet();
            return AimCheckResult.flagged(List.of(reason, "连续 " + v + " 次瞬间转向"));
        }
        return AimCheckResult.suspicious(List.of(reason));
    }

    /**
     * 检测 Modulo 360 偏航环绕（移植 Grim AimModulo360）。
     *
     * <p>需要 Agent 上报客户端原始偏航角变化量（deltaYaw）。当某次变化接近一整圈
     * （&gt;320°）而上次变化极小（&lt;30°）时，说明客户端用 yaw 环绕来掩盖瞬间转向。</p>
     *
     * @param playerName 玩家名称
     * @param deltaYaw   客户端上报的原始偏航角变化量（度）
     * @return 检测结果
     */
    public AimCheckResult checkModulo360(String playerName, float deltaYaw) {
        if (!config.getSecurity().getSuperEvolution().isAntiAim()) {
            return AimCheckResult.clean();
        }
        totalChecks.incrementAndGet();

        float last = lastDeltaYaw.getOrDefault(playerName, 0.0f);
        lastDeltaYaw.put(playerName, deltaYaw);

        if (Math.abs(deltaYaw) > MODULO_360_THRESHOLD && Math.abs(last) < MODULO_360_LAST_MAX) {
            flaggedCount.incrementAndGet();
            return AimCheckResult.flagged(List.of(
                    "AIM_MODULO_360: 单次偏航环绕 " + String.format("%.1f", deltaYaw)
                            + "°（上次 " + String.format("%.1f", last) + "°）"));
        }
        return AimCheckResult.clean();
    }

    /** 玩家离线时清理追踪数据 */
    public void clearPlayer(String playerName) {
        lastYaw.remove(playerName);
        lastDeltaYaw.remove(playerName);
        violations.remove(playerName);
    }

    /** 获取模块运行状态 */
    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("enabled", config.getSecurity().getSuperEvolution().isAntiAim());
        s.put("totalChecks", totalChecks.get());
        s.put("flaggedCount", flaggedCount.get());
        s.put("trackedPlayers", lastYaw.size());
        return s;
    }

    /** 瞬间转向检测结果 */
    public static class AimCheckResult {
        private final boolean flagged;
        private final boolean suspicious;
        private final List<String> reasons;

        private AimCheckResult(boolean flagged, boolean suspicious, List<String> reasons) {
            this.flagged = flagged;
            this.suspicious = suspicious;
            this.reasons = reasons;
        }

        public static AimCheckResult clean() {
            return new AimCheckResult(false, false, List.of());
        }

        public static AimCheckResult suspicious(List<String> reasons) {
            return new AimCheckResult(false, true, reasons);
        }

        public static AimCheckResult flagged(List<String> reasons) {
            return new AimCheckResult(true, false, reasons);
        }

        public boolean isFlagged() { return flagged; }
        public boolean isSuspicious() { return suspicious; }
        public boolean isClean() { return !flagged && !suspicious; }
        public List<String> getReasons() { return reasons; }
    }
}
