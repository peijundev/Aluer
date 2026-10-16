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
 * 无挥臂（NoSwing）检测服务 — V6.0 开源反作弊融合模块
 *
 * <p>检测思路参考开源反作弊 <b>NoCheatPlus 的 NoSwing 检查</b>：在 Minecraft 中，
 * 玩家攻击实体、破坏方块、放置方块都必须先挥动手臂（swing arm）。NoSwing 类作弊
 * 会跳过挥臂动画，直接发送攻击/破坏/放置数据包，从而绕过基于动画的检测。</p>
 *
 * <p>检测原理：追踪每个玩家最近一次挥臂时间，当发生攻击/破块/放块动作时，
 * 校验该动作与最近挥臂的时间间隔。若间隔超出正常窗口（说明「有动作但没挥臂」），
 * 累计违规；连续多次无挥臂动作即判定为作弊。</p>
 *
 * <p>配置开关：serverguard.security.super-evolution.anti-no-swing</p>
 */
@Service
public class AntiNoSwingService {

    private final ServerGuardConfig config;

    /** 每个玩家最近一次挥臂时间（playerName → 时间戳） */
    private final Map<String, Instant> lastSwingTimes = new ConcurrentHashMap<>();

    /** 每个玩家的连续无挥臂违规次数（playerName → 次数） */
    private final Map<String, Integer> violations = new ConcurrentHashMap<>();

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong flaggedCount = new AtomicLong(0);

    /** 挥臂到动作的最大允许间隔（毫秒）—— 超过视为「无挥臂动作」 */
    private static final long SWING_WINDOW_MS = 150;

    /** 连续无挥臂动作次数阈值 —— 达到即标记 */
    private static final int VIOLATION_THRESHOLD = 3;

    /** 无参构造（测试兼容） */
    public AntiNoSwingService() {
        this(new ServerGuardConfig());
    }

    public AntiNoSwingService(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 记录一次玩家挥臂动作（监听 SwingEvent 时调用）。
     *
     * @param playerName 玩家名称
     * @param timestamp  挥臂时间戳
     */
    public void recordSwing(String playerName, Instant timestamp) {
        if (!config.getSecurity().getSuperEvolution().isAntiNoSwing()) {
            return;
        }
        lastSwingTimes.put(playerName, timestamp);
    }

    /** 检测攻击实体时是否挥臂 */
    public NoSwingCheckResult checkAttack(String playerName, String targetName, Instant timestamp) {
        return checkAction(playerName, "attack", targetName, timestamp);
    }

    /** 检测破坏方块时是否挥臂 */
    public NoSwingCheckResult checkBlockBreak(String playerName, String material, Instant timestamp) {
        return checkAction(playerName, "block-break", material, timestamp);
    }

    /** 检测放置方块时是否挥臂 */
    public NoSwingCheckResult checkBlockPlace(String playerName, String material, Instant timestamp) {
        return checkAction(playerName, "block-place", material, timestamp);
    }

    /**
     * 统一动作校验：动作发生时，最近挥臂是否在允许窗口内。
     */
    private NoSwingCheckResult checkAction(String playerName, String action, String detail, Instant timestamp) {
        if (!config.getSecurity().getSuperEvolution().isAntiNoSwing()) {
            return NoSwingCheckResult.clean();
        }
        totalChecks.incrementAndGet();

        Instant lastSwing = lastSwingTimes.get(playerName);
        if (lastSwing == null) {
            // 尚无挥臂记录（可能是记录开始前的首次动作），无法判定，不标记
            return NoSwingCheckResult.clean();
        }

        long gap = timestamp.toEpochMilli() - lastSwing.toEpochMilli();
        if (gap <= SWING_WINDOW_MS) {
            // 动作伴随挥臂，正常
            return NoSwingCheckResult.clean();
        }

        // 有动作但窗口内无挥臂 —— 累计违规
        int v = violations.merge(playerName, 1, Integer::sum);
        String reason = "NO_SWING: " + action + " " + detail + " 距上次挥臂 " + gap + "ms";
        if (v >= VIOLATION_THRESHOLD) {
            flaggedCount.incrementAndGet();
            return NoSwingCheckResult.flagged(List.of(reason, "连续 " + v + " 次无挥臂动作"));
        }
        return NoSwingCheckResult.suspicious(List.of(reason));
    }

    /** 玩家离线时清理追踪数据 */
    public void clearPlayer(String playerName) {
        lastSwingTimes.remove(playerName);
        violations.remove(playerName);
    }

    /** 获取模块运行状态 */
    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("enabled", config.getSecurity().getSuperEvolution().isAntiNoSwing());
        s.put("totalChecks", totalChecks.get());
        s.put("flaggedCount", flaggedCount.get());
        s.put("trackedPlayers", lastSwingTimes.size());
        return s;
    }

    /** 无挥臂检测结果 */
    public static class NoSwingCheckResult {
        private final boolean flagged;
        private final boolean suspicious;
        private final List<String> reasons;

        private NoSwingCheckResult(boolean flagged, boolean suspicious, List<String> reasons) {
            this.flagged = flagged;
            this.suspicious = suspicious;
            this.reasons = reasons;
        }

        public static NoSwingCheckResult clean() {
            return new NoSwingCheckResult(false, false, List.of());
        }

        public static NoSwingCheckResult suspicious(List<String> reasons) {
            return new NoSwingCheckResult(false, true, reasons);
        }

        public static NoSwingCheckResult flagged(List<String> reasons) {
            return new NoSwingCheckResult(true, false, reasons);
        }

        public boolean isFlagged() { return flagged; }
        public boolean isSuspicious() { return suspicious; }
        public boolean isClean() { return !flagged && !suspicious; }
        public List<String> getReasons() { return reasons; }
    }
}
