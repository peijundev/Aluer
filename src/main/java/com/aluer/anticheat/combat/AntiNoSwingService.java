package com.aluer.anticheat.combat;

import com.aluer.config.ServerGuardConfig;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 无挥臂（NoSwing）检测服务 — V6.0 开源反作弊融合模块
 *
 * <p>检测算法<b>移植自 NoCheatPlus 的 fight/NoSwing</b>（本项目已采用 GPL-3.0，
 * 与 NCP 许可证兼容）。NCP 原始逻辑：</p>
 * <ul>
 *   <li>玩家挥臂时置位 {@code noSwingArmSwung} 标志。</li>
 *   <li>攻击/破坏时检查该标志：若已挥臂则衰减违规等级（VL × 0.9）并清除标志；
 *       若未挥臂则 VL 累加 1。</li>
 *   <li>VL 达到阈值即判定为 NoSwing 作弊。</li>
 * </ul>
 *
 * <p>本服务将其翻译为「接收 Agent 结构化数据」的等价状态机，保留 VL 累积 + 衰减机制。</p>
 *
 * <p>配置开关：serverguard.security.super-evolution.anti-no-swing</p>
 */
@Service
public class AntiNoSwingService {

    private final ServerGuardConfig config;

    /** 每个玩家的挥臂标志（true = 已挥臂待消费） */
    private final Map<String, Boolean> armSwung = new ConcurrentHashMap<>();

    /** 每个玩家的违规等级 VL（累积 + 衰减） */
    private final Map<String, Double> violationLevel = new ConcurrentHashMap<>();

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong flaggedCount = new AtomicLong(0);

    /** VL 达到此阈值判定为作弊 */
    private static final double VL_THRESHOLD = 4.0;

    /** 挥臂后攻击的 VL 衰减系数（对齐 NCP） */
    private static final double VL_DECAY = 0.9;

    /** 无参构造（测试兼容） */
    public AntiNoSwingService() {
        this(new ServerGuardConfig());
    }

    public AntiNoSwingService(ServerGuardConfig config) {
        this.config = config;
    }

    /** 记录一次挥臂动作（监听 SwingEvent 时调用） */
    public void recordSwing(String playerName) {
        if (!config.getSecurity().getSuperEvolution().isAntiNoSwing()) {
            return;
        }
        armSwung.put(playerName, true);
    }

    /** 检测攻击实体时是否挥臂 */
    public NoSwingCheckResult checkAttack(String playerName, String targetName) {
        return checkAction(playerName, "attack", targetName);
    }

    /** 检测破坏方块时是否挥臂 */
    public NoSwingCheckResult checkBlockBreak(String playerName, String material) {
        return checkAction(playerName, "block-break", material);
    }

    /** 检测放置方块时是否挥臂 */
    public NoSwingCheckResult checkBlockPlace(String playerName, String material) {
        return checkAction(playerName, "block-place", material);
    }

    /**
     * 统一动作校验（对齐 NCP 的「挥臂标志 + VL 累积/衰减」逻辑）。
     */
    private NoSwingCheckResult checkAction(String playerName, String action, String detail) {
        if (!config.getSecurity().getSuperEvolution().isAntiNoSwing()) {
            return NoSwingCheckResult.clean();
        }
        totalChecks.incrementAndGet();

        // 消费挥臂标志（null 表示从未挥臂）
        boolean swung = Boolean.TRUE.equals(armSwung.remove(playerName));
        double vl = violationLevel.getOrDefault(playerName, 0.0);

        if (swung) {
            // 挥臂后动作，正常，衰减违规等级
            violationLevel.put(playerName, vl * VL_DECAY);
            return NoSwingCheckResult.clean();
        }

        // 未挥臂动作，违规等级累加
        vl += 1.0;
        violationLevel.put(playerName, vl);
        String reason = "NO_SWING: " + action + " " + detail + "（VL=" + String.format("%.1f", vl) + "）";
        if (vl >= VL_THRESHOLD) {
            flaggedCount.incrementAndGet();
            return NoSwingCheckResult.flagged(List.of(reason, "无挥臂动作累积"));
        }
        return NoSwingCheckResult.suspicious(List.of(reason));
    }

    /** 玩家离线时清理追踪数据 */
    public void clearPlayer(String playerName) {
        armSwung.remove(playerName);
        violationLevel.remove(playerName);
    }

    /** 获取模块运行状态 */
    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("enabled", config.getSecurity().getSuperEvolution().isAntiNoSwing());
        s.put("totalChecks", totalChecks.get());
        s.put("flaggedCount", flaggedCount.get());
        s.put("trackedPlayers", violationLevel.size());
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
