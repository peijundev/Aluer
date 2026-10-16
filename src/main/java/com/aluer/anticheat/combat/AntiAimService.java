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
 * <p>检测思路参考开源反作弊 <b>Kauri 的 Aim 检查</b>与 <b>GrimAC 的旋转预测</b>：
 * 人类玩家通过鼠标控制视角，头部旋转是渐进的，单次移动更新（tick）内的水平偏航角
 * 变化存在生理上限。Aimbot / AimAssist 作弊会让视角瞬间「跳」到目标上，产生
 * 远超人体极限的单次旋转角度（snap）。</p>
 *
 * <p>检测原理：追踪玩家连续两次旋转数据，计算偏航角变化量（含 360° 环绕处理），
 * 超过人体上限即累计违规；单次极端掉头（≥180°）直接标记。</p>
 *
 * <p>配置开关：serverguard.security.super-evolution.anti-aim</p>
 */
@Service
public class AntiAimService {

    private final ServerGuardConfig config;

    /** 每个玩家最近一次偏航角（playerName → yaw） */
    private final Map<String, Float> lastYaw = new ConcurrentHashMap<>();

    /** 每个玩家的连续瞬间转向违规次数（playerName → 次数） */
    private final Map<String, Integer> violations = new ConcurrentHashMap<>();

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong flaggedCount = new AtomicLong(0);

    /** 单次偏航角变化的生理上限（度）—— 超过视为瞬间转向 */
    private static final double SNAP_THRESHOLD = 60.0;

    /** 极端掉头阈值（度）—— 单次达到即直接标记 */
    private static final double EXTREME_THRESHOLD = 180.0;

    /** 连续瞬间转向次数阈值 */
    private static final int VIOLATION_THRESHOLD = 3;

    /** 无参构造（测试兼容） */
    public AntiAimService() {
        this(new ServerGuardConfig());
    }

    public AntiAimService(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 检测玩家视角旋转是否异常。
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
        // 偏航角存在 360° 环绕，需归一化到 [0, 180]
        if (delta > 180.0) {
            delta = 360.0 - delta;
        }

        if (delta <= SNAP_THRESHOLD) {
            // 正常旋转
            violations.remove(playerName);
            return AimCheckResult.clean();
        }

        // 极端掉头直接标记
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

    /** 玩家离线时清理追踪数据 */
    public void clearPlayer(String playerName) {
        lastYaw.remove(playerName);
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
