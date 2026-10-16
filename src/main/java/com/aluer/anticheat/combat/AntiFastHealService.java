package com.aluer.anticheat.combat;

import com.aluer.config.ServerGuardConfig;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 快速回血（FastHeal）检测服务 — V6.0 开源反作弊融合模块
 *
 * <p>检测算法<b>移植自 NoCheatPlus 的 fight/FastHeal</b>：NCP 追踪玩家回血事件，
 * 若两次回血间隔小于合法间隔（fastHealInterval，饱和回血约 3 秒）则累积违规，
 * 并辅以 buffer 缓冲与 VL 衰减。本服务保留「间隔阈值 + 连续违规」的核心判定。</p>
 *
 * <p>Aluer 架构下 Agent 采集玩家生命值变化，本服务在检测到回血（生命值上升）时
 * 校验与前次回血的时间间隔。</p>
 *
 * <p>配置开关：serverguard.security.super-evolution.anti-fast-heal</p>
 */
@Service
public class AntiFastHealService {

    private final ServerGuardConfig config;

    /** 每个玩家最近一次生命值（playerName → health） */
    private final Map<String, Double> lastHealth = new ConcurrentHashMap<>();

    /** 每个玩家最近一次回血时间（playerName → 时间戳） */
    private final Map<String, Instant> lastRegenTime = new ConcurrentHashMap<>();

    /** 每个玩家连续快速回血次数（playerName → 次数） */
    private final Map<String, Integer> violations = new ConcurrentHashMap<>();

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong flaggedCount = new AtomicLong(0);

    /** 饱和回血的最小合法间隔（毫秒）——约 3 秒回 0.5 格 */
    private static final long MIN_REGEN_INTERVAL_MS = 3000;

    /** 连续快速回血次数阈值 */
    private static final int VIOLATION_THRESHOLD = 3;

    /** 无参构造（测试兼容） */
    public AntiFastHealService() {
        this(new ServerGuardConfig());
    }

    public AntiFastHealService(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 检测玩家生命值变化是否包含快速回血。
     *
     * @param playerName 玩家名称
     * @param health     当前生命值
     * @param timestamp  采样时间戳
     * @return 检测结果
     */
    public HealCheckResult checkRegen(String playerName, double health, Instant timestamp) {
        if (!config.getSecurity().getSuperEvolution().isAntiFastHeal()) {
            return HealCheckResult.clean();
        }
        totalChecks.incrementAndGet();

        Double last = lastHealth.put(playerName, health);
        if (last == null || health <= last) {
            // 首次记录，或生命值未上升（掉血/不变），不检测
            return HealCheckResult.clean();
        }

        // 生命值上升（回血），校验间隔
        Instant lastRegen = lastRegenTime.put(playerName, timestamp);
        if (lastRegen == null) {
            // 首次回血，无法计算间隔
            return HealCheckResult.clean();
        }

        long interval = timestamp.toEpochMilli() - lastRegen.toEpochMilli();
        if (interval >= MIN_REGEN_INTERVAL_MS) {
            // 回血间隔正常
            violations.remove(playerName);
            return HealCheckResult.clean();
        }

        int v = violations.merge(playerName, 1, Integer::sum);
        String reason = "FAST_HEAL: 回血间隔 " + interval + "ms（合法最小 " + MIN_REGEN_INTERVAL_MS + "ms）";
        if (v >= VIOLATION_THRESHOLD) {
            flaggedCount.incrementAndGet();
            return HealCheckResult.flagged(List.of(reason, "连续 " + v + " 次快速回血"));
        }
        return HealCheckResult.suspicious(List.of(reason));
    }

    /** 玩家离线时清理追踪数据 */
    public void clearPlayer(String playerName) {
        lastHealth.remove(playerName);
        lastRegenTime.remove(playerName);
        violations.remove(playerName);
    }

    /** 获取模块运行状态 */
    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("enabled", config.getSecurity().getSuperEvolution().isAntiFastHeal());
        s.put("totalChecks", totalChecks.get());
        s.put("flaggedCount", flaggedCount.get());
        s.put("trackedPlayers", lastHealth.size());
        return s;
    }

    /** 快速回血检测结果 */
    public static class HealCheckResult {
        private final boolean flagged;
        private final boolean suspicious;
        private final List<String> reasons;

        private HealCheckResult(boolean flagged, boolean suspicious, List<String> reasons) {
            this.flagged = flagged;
            this.suspicious = suspicious;
            this.reasons = reasons;
        }

        public static HealCheckResult clean() {
            return new HealCheckResult(false, false, List.of());
        }

        public static HealCheckResult suspicious(List<String> reasons) {
            return new HealCheckResult(false, true, reasons);
        }

        public static HealCheckResult flagged(List<String> reasons) {
            return new HealCheckResult(true, false, reasons);
        }

        public boolean isFlagged() { return flagged; }
        public boolean isSuspicious() { return suspicious; }
        public boolean isClean() { return !flagged && !suspicious; }
        public List<String> getReasons() { return reasons; }
    }
}
