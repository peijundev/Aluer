package com.aluer.anticheat.combat;

import com.aluer.config.ServerGuardConfig;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 无敌（GodMode）检测服务 — V6.0 开源反作弊融合模块
 *
 * <p>检测算法<b>移植自 NoCheatPlus 的 fight/GodMode</b>：NCP 通过比对玩家的
 * 无敌 tick 数（noDamageTicks / invulnerableTicks）与伤害 tick 差，判断玩家是否
 * 在受击后非法保持无敌状态。</p>
 *
 * <p>Aluer 架构下 Agent 采集的是「伤害值 + 受击后生命值」，故本服务采用等价判定：
 * 玩家持续受到伤害（damage &gt; 0）但生命值不下降，说明其免疫了伤害（GodMode）。</p>
 *
 * <p>配置开关：serverguard.security.super-evolution.anti-god-mode</p>
 */
@Service
public class AntiGodModeService {

    private final ServerGuardConfig config;

    /** 每个玩家最近一次生命值（playerName → health） */
    private final Map<String, Double> lastHealth = new ConcurrentHashMap<>();

    /** 每个玩家连续「受击不掉血」次数（playerName → 次数） */
    private final Map<String, Integer> godModeCount = new ConcurrentHashMap<>();

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong flaggedCount = new AtomicLong(0);

    /** 连续受击不掉血次数阈值 */
    private static final int GODMODE_THRESHOLD = 3;

    /** 生命值下降判定容差（浮点误差） */
    private static final double HEALTH_EPSILON = 0.001;

    /** 无参构造（测试兼容） */
    public AntiGodModeService() {
        this(new ServerGuardConfig());
    }

    public AntiGodModeService(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 检测玩家受击后的生命值变化是否异常。
     *
     * @param playerName 玩家名称
     * @param damage     本次受到的伤害量（>0 表示受到伤害）
     * @param health     受击后的生命值
     * @return 检测结果
     */
    public GodModeCheckResult checkDamage(String playerName, double damage, double health) {
        if (!config.getSecurity().getSuperEvolution().isAntiGodMode()) {
            return GodModeCheckResult.clean();
        }
        totalChecks.incrementAndGet();

        if (damage <= 0) {
            // 未受到伤害，不检测
            return GodModeCheckResult.clean();
        }

        Double last = lastHealth.put(playerName, health);
        if (last == null) {
            // 首次记录，无法比对
            return GodModeCheckResult.clean();
        }

        if (health < last - HEALTH_EPSILON) {
            // 生命值正常下降
            godModeCount.remove(playerName);
            return GodModeCheckResult.clean();
        }

        // 受伤害但生命值未下降 —— 疑似无敌
        int count = godModeCount.merge(playerName, 1, Integer::sum);
        String reason = "GOD_MODE: 受击 " + String.format("%.2f", damage)
                + " 但生命值未下降（" + String.format("%.2f", last) + " → " + String.format("%.2f", health) + "）";
        if (count >= GODMODE_THRESHOLD) {
            flaggedCount.incrementAndGet();
            return GodModeCheckResult.flagged(List.of(reason, "连续 " + count + " 次受击不掉血"));
        }
        return GodModeCheckResult.suspicious(List.of(reason));
    }

    /** 玩家离线时清理追踪数据 */
    public void clearPlayer(String playerName) {
        lastHealth.remove(playerName);
        godModeCount.remove(playerName);
    }

    /** 获取模块运行状态 */
    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("enabled", config.getSecurity().getSuperEvolution().isAntiGodMode());
        s.put("totalChecks", totalChecks.get());
        s.put("flaggedCount", flaggedCount.get());
        s.put("trackedPlayers", lastHealth.size());
        return s;
    }

    /** 无敌检测结果 */
    public static class GodModeCheckResult {
        private final boolean flagged;
        private final boolean suspicious;
        private final List<String> reasons;

        private GodModeCheckResult(boolean flagged, boolean suspicious, List<String> reasons) {
            this.flagged = flagged;
            this.suspicious = suspicious;
            this.reasons = reasons;
        }

        public static GodModeCheckResult clean() {
            return new GodModeCheckResult(false, false, List.of());
        }

        public static GodModeCheckResult suspicious(List<String> reasons) {
            return new GodModeCheckResult(false, true, reasons);
        }

        public static GodModeCheckResult flagged(List<String> reasons) {
            return new GodModeCheckResult(true, false, reasons);
        }

        public boolean isFlagged() { return flagged; }
        public boolean isSuspicious() { return suspicious; }
        public boolean isClean() { return !flagged && !suspicious; }
        public List<String> getReasons() { return reasons; }
    }
}
