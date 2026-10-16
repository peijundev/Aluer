package com.aluer.anticheat.movement;

import com.aluer.config.ServerGuardConfig;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 爬梯加速（FastLadder / Ascend）检测服务 — V6.0 开源反作弊融合模块
 *
 * <p>检测思路参考开源反作弊 <b>NoCheatPlus 的 FastLadder / Ascend 检查</b>：
 * Minecraft 中玩家在梯子、藤蔓上攀爬存在固定的速度上限，FastLadder 类作弊会
 * 大幅提高攀爬速度。本服务在玩家处于攀爬状态时追踪其垂直位移速度，
 * 超过合法上限即累计违规。</p>
 *
 * <p>检测原理：单 tick 内向上位移超过阈值即视为一次超速，连续多次超速判定为作弊。</p>
 *
 * <p>配置开关：serverguard.security.super-evolution.anti-fast-ladder</p>
 */
@Service
public class AntiFastLadderService {

    private final ServerGuardConfig config;

    /** 每个玩家的连续超速次数（playerName → 次数） */
    private final Map<String, Integer> violations = new ConcurrentHashMap<>();

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong flaggedCount = new AtomicLong(0);

    /** 单 tick 攀爬最大合法向上位移（blocks/tick）—— 正常爬梯约 0.2，此阈值已留容差 */
    private static final double MAX_LADDER_SPEED = 0.4;

    /** 连续超速次数阈值 */
    private static final int VIOLATION_THRESHOLD = 3;

    /** 无参构造（测试兼容） */
    public AntiFastLadderService() {
        this(new ServerGuardConfig());
    }

    public AntiFastLadderService(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 检测玩家在攀爬状态下的移动速度。
     *
     * @param playerName 玩家名称
     * @param fromY      移动前 Y 坐标
     * @param toY        移动后 Y 坐标
     * @param climbing   是否处于攀爬状态（梯子/藤蔓上）
     * @return 检测结果
     */
    public LadderCheckResult checkLadderMovement(String playerName, double fromY, double toY, boolean climbing) {
        if (!config.getSecurity().getSuperEvolution().isAntiFastLadder()) {
            return LadderCheckResult.clean();
        }
        totalChecks.incrementAndGet();

        double dy = toY - fromY;
        if (!climbing || dy <= 0) {
            // 非攀爬状态或未向上移动，不检测；正常攀爬时重置违规计数
            if (climbing && dy <= MAX_LADDER_SPEED) {
                violations.remove(playerName);
            }
            return LadderCheckResult.clean();
        }

        if (dy <= MAX_LADDER_SPEED) {
            // 正常攀爬速度
            violations.remove(playerName);
            return LadderCheckResult.clean();
        }

        int v = violations.merge(playerName, 1, Integer::sum);
        String reason = "FAST_LADDER: 单 tick 上升 " + String.format("%.3f", dy) + " blocks（阈值 " + MAX_LADDER_SPEED + "）";
        if (v >= VIOLATION_THRESHOLD) {
            flaggedCount.incrementAndGet();
            return LadderCheckResult.flagged(List.of(reason, "连续 " + v + " 次超速攀爬"));
        }
        return LadderCheckResult.suspicious(List.of(reason));
    }

    /** 玩家离线时清理追踪数据 */
    public void clearPlayer(String playerName) {
        violations.remove(playerName);
    }

    /** 获取模块运行状态 */
    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("enabled", config.getSecurity().getSuperEvolution().isAntiFastLadder());
        s.put("totalChecks", totalChecks.get());
        s.put("flaggedCount", flaggedCount.get());
        s.put("trackedPlayers", violations.size());
        return s;
    }

    /** 爬梯加速检测结果 */
    public static class LadderCheckResult {
        private final boolean flagged;
        private final boolean suspicious;
        private final List<String> reasons;

        private LadderCheckResult(boolean flagged, boolean suspicious, List<String> reasons) {
            this.flagged = flagged;
            this.suspicious = suspicious;
            this.reasons = reasons;
        }

        public static LadderCheckResult clean() {
            return new LadderCheckResult(false, false, List.of());
        }

        public static LadderCheckResult suspicious(List<String> reasons) {
            return new LadderCheckResult(false, true, reasons);
        }

        public static LadderCheckResult flagged(List<String> reasons) {
            return new LadderCheckResult(true, false, reasons);
        }

        public boolean isFlagged() { return flagged; }
        public boolean isSuspicious() { return suspicious; }
        public boolean isClean() { return !flagged && !suspicious; }
        public List<String> getReasons() { return reasons; }
    }
}
