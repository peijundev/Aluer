package com.aluer.anticheat.movement;

import com.aluer.config.ServerGuardConfig;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 蜘蛛网减速绕过（NoWeb）检测服务 — V6.0 开源反作弊融合模块
 *
 * <p>检测思路参考 NoCheatPlus 的 NoWeb 检查：蜘蛛网会大幅降低玩家水平移动速度
 * （正常约 0.2 blocks/tick 降至约 0.03 blocks/tick）。NoWeb 类作弊会移除蜘蛛网的
 * 减速效果，使玩家在蜘蛛网内仍保持全速移动。</p>
 *
 * <p>检测原理：玩家处于蜘蛛网内时，追踪其单 tick 水平位移，超过蜘蛛网减速后的
 * 合法上限即累计违规。</p>
 *
 * <p>配置开关：serverguard.security.super-evolution.anti-no-web</p>
 */
@Service
public class AntiNoWebService {

    private final ServerGuardConfig config;

    /** 每个玩家的连续超速次数（playerName → 次数） */
    private final Map<String, Integer> violations = new ConcurrentHashMap<>();

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong flaggedCount = new AtomicLong(0);

    /** 蜘蛛网内单 tick 最大合法水平位移（blocks/tick）——正常约 0.03，此阈值已留容差 */
    private static final double MAX_WEB_SPEED = 0.1;

    /** 连续超速次数阈值 */
    private static final int VIOLATION_THRESHOLD = 3;

    /** 无参构造（测试兼容） */
    public AntiNoWebService() {
        this(new ServerGuardConfig());
    }

    public AntiNoWebService(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 检测玩家在蜘蛛网内的移动速度。
     *
     * @param playerName 玩家名称
     * @param fromX      移动前 X 坐标
     * @param fromZ      移动前 Z 坐标
     * @param toX        移动后 X 坐标
     * @param toZ        移动后 Z 坐标
     * @param inWeb      是否处于蜘蛛网内
     * @return 检测结果
     */
    public WebCheckResult checkMovement(String playerName, double fromX, double fromZ,
                                        double toX, double toZ, boolean inWeb) {
        if (!config.getSecurity().getSuperEvolution().isAntiNoWeb()) {
            return WebCheckResult.clean();
        }
        totalChecks.incrementAndGet();

        double dx = toX - fromX;
        double dz = toZ - fromZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        if (!inWeb) {
            // 不在蜘蛛网内，不检测
            return WebCheckResult.clean();
        }

        if (horizontal <= MAX_WEB_SPEED) {
            // 蜘蛛网减速效果正常
            violations.remove(playerName);
            return WebCheckResult.clean();
        }

        int v = violations.merge(playerName, 1, Integer::sum);
        String reason = "NO_WEB: 蜘蛛网内单 tick 水平位移 " + String.format("%.3f", horizontal)
                + " blocks（阈值 " + MAX_WEB_SPEED + "）";
        if (v >= VIOLATION_THRESHOLD) {
            flaggedCount.incrementAndGet();
            return WebCheckResult.flagged(List.of(reason, "连续 " + v + " 次蜘蛛网内全速移动"));
        }
        return WebCheckResult.suspicious(List.of(reason));
    }

    /** 玩家离线时清理追踪数据 */
    public void clearPlayer(String playerName) {
        violations.remove(playerName);
    }

    /** 获取模块运行状态 */
    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("enabled", config.getSecurity().getSuperEvolution().isAntiNoWeb());
        s.put("totalChecks", totalChecks.get());
        s.put("flaggedCount", flaggedCount.get());
        s.put("trackedPlayers", violations.size());
        return s;
    }

    /** 蜘蛛网减速绕过检测结果 */
    public static class WebCheckResult {
        private final boolean flagged;
        private final boolean suspicious;
        private final List<String> reasons;

        private WebCheckResult(boolean flagged, boolean suspicious, List<String> reasons) {
            this.flagged = flagged;
            this.suspicious = suspicious;
            this.reasons = reasons;
        }

        public static WebCheckResult clean() {
            return new WebCheckResult(false, false, List.of());
        }

        public static WebCheckResult suspicious(List<String> reasons) {
            return new WebCheckResult(false, true, reasons);
        }

        public static WebCheckResult flagged(List<String> reasons) {
            return new WebCheckResult(true, false, reasons);
        }

        public boolean isFlagged() { return flagged; }
        public boolean isSuspicious() { return suspicious; }
        public boolean isClean() { return !flagged && !suspicious; }
        public List<String> getReasons() { return reasons; }
    }
}
