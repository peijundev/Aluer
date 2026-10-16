package com.aluer.anticheat.movement;

import com.aluer.config.ServerGuardConfig;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 地面状态欺骗（GroundSpoof）检测服务 — V6.0 开源反作弊融合模块
 *
 * <p>检测思路参考 GrimAC 的 GroundSpoof/NoFall 检查：Grim 通过碰撞箱计算判断玩家
 * 是否真正贴地，若客户端上报 {@code onGround=true} 却未与地面碰撞则判定地面欺骗。</p>
 *
 * <p>Aluer 架构下 Agent 采集的是移动事件的坐标与 {@code onGround} 标志，故本服务采用
 * 等价判定：玩家声明站在地面（onGround=true），但 Y 坐标持续快速下降（自由落体），
 * 说明其伪造了地面状态（用于绕过 Speed/NoFall 等依赖地面状态的检测）。</p>
 *
 * <p>配置开关：serverguard.security.super-evolution.anti-ground-spoof</p>
 */
@Service
public class AntiGroundSpoofService {

    private final ServerGuardConfig config;

    /** 每个玩家的连续地面欺骗违规次数（playerName → 次数） */
    private final Map<String, Integer> violations = new ConcurrentHashMap<>();

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong flaggedCount = new AtomicLong(0);

    /** 声明在地面时，单 tick 允许的最大下降位移（blocks/tick）—— 超过即伪造地面 */
    private static final double GROUND_SPOOF_THRESHOLD = 0.1;

    /** 连续违规次数阈值 */
    private static final int VIOLATION_THRESHOLD = 3;

    /** 无参构造（测试兼容） */
    public AntiGroundSpoofService() {
        this(new ServerGuardConfig());
    }

    public AntiGroundSpoofService(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 检测玩家地面状态是否被伪造。
     *
     * @param playerName 玩家名称
     * @param onGround   客户端上报是否站在地面
     * @param fromY      移动前 Y 坐标
     * @param toY        移动后 Y 坐标
     * @return 检测结果
     */
    public GroundSpoofCheckResult checkGroundStatus(String playerName, boolean onGround,
                                                    double fromY, double toY) {
        if (!config.getSecurity().getSuperEvolution().isAntiGroundSpoof()) {
            return GroundSpoofCheckResult.clean();
        }
        totalChecks.incrementAndGet();

        double deltaY = toY - fromY;
        if (!onGround || deltaY >= -GROUND_SPOOF_THRESHOLD) {
            // 未声明在地面，或下降幅度正常
            violations.remove(playerName);
            return GroundSpoofCheckResult.clean();
        }

        // 声明在地面却快速下降 —— 伪造地面状态
        int v = violations.merge(playerName, 1, Integer::sum);
        String reason = "GROUND_SPOOF: onGround=true 但单 tick 下降 " + String.format("%.3f", deltaY)
                + " blocks（阈值 -" + GROUND_SPOOF_THRESHOLD + "）";
        if (v >= VIOLATION_THRESHOLD) {
            flaggedCount.incrementAndGet();
            return GroundSpoofCheckResult.flagged(List.of(reason, "连续 " + v + " 次地面欺骗"));
        }
        return GroundSpoofCheckResult.suspicious(List.of(reason));
    }

    /** 玩家离线时清理追踪数据 */
    public void clearPlayer(String playerName) {
        violations.remove(playerName);
    }

    /** 获取模块运行状态 */
    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("enabled", config.getSecurity().getSuperEvolution().isAntiGroundSpoof());
        s.put("totalChecks", totalChecks.get());
        s.put("flaggedCount", flaggedCount.get());
        s.put("trackedPlayers", violations.size());
        return s;
    }

    /** 地面欺骗检测结果 */
    public static class GroundSpoofCheckResult {
        private final boolean flagged;
        private final boolean suspicious;
        private final List<String> reasons;

        private GroundSpoofCheckResult(boolean flagged, boolean suspicious, List<String> reasons) {
            this.flagged = flagged;
            this.suspicious = suspicious;
            this.reasons = reasons;
        }

        public static GroundSpoofCheckResult clean() {
            return new GroundSpoofCheckResult(false, false, List.of());
        }

        public static GroundSpoofCheckResult suspicious(List<String> reasons) {
            return new GroundSpoofCheckResult(false, true, reasons);
        }

        public static GroundSpoofCheckResult flagged(List<String> reasons) {
            return new GroundSpoofCheckResult(true, false, reasons);
        }

        public boolean isFlagged() { return flagged; }
        public boolean isSuspicious() { return suspicious; }
        public boolean isClean() { return !flagged && !suspicious; }
        public List<String> getReasons() { return reasons; }
    }
}
