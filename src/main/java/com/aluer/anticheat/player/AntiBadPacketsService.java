package com.aluer.anticheat.player;

import com.aluer.config.ServerGuardConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 非法数据包（BadPackets）检测服务 — V6.0 开源反作弊融合模块
 *
 * <p>检测思路参考开源反作弊 <b>GrimAC 的 BadPackets 检查系列</b>：合法客户端在
 * 正常游戏状态下不可能发送某些畸形数据包，例如俯仰角越界、偏航角越界、
 * 坐标出现 NaN/无穷大等。这些数据往往是作弊客户端（或崩溃攻击）伪造的，
 * 可在进入其他分析逻辑之前低成本地拦截。</p>
 *
 * <p>检测维度：</p>
 * <ol>
 *   <li>俯仰角（pitch）越界 —— 合法范围为 [-90, 90]，超出即伪造。</li>
 *   <li>偏航角（yaw）越界 —— 合法范围为 [-360, 360]，超出即伪造。</li>
 *   <li>坐标非法 —— 出现 NaN / Infinity 的坐标是畸形数据包。</li>
 * </ol>
 *
 * <p>配置开关：serverguard.security.super-evolution.anti-bad-packets</p>
 */
@Service
public class AntiBadPacketsService {

    private final ServerGuardConfig config;

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong flaggedCount = new AtomicLong(0);

    /** 无参构造（测试兼容） */
    public AntiBadPacketsService() {
        this(new ServerGuardConfig());
    }

    public AntiBadPacketsService(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 校验玩家旋转数据包（yaw / pitch）是否合法。
     *
     * @param playerName 玩家名称
     * @param yaw        水平偏航角（度）
     * @param pitch      俯仰角（度）
     * @return 检测结果
     */
    public BadPacketResult checkRotation(String playerName, float yaw, float pitch) {
        if (!config.getSecurity().getSuperEvolution().isAntiBadPackets()) {
            return BadPacketResult.clean();
        }
        totalChecks.incrementAndGet();

        List<String> reasons = new ArrayList<>();
        if (Float.isNaN(pitch) || Float.isInfinite(pitch) || Math.abs(pitch) > 90.0f) {
            // 合法客户端 pitch 不会超出 [-90, 90]，越界说明是伪造数据包
            reasons.add("INVALID_PITCH: " + pitch);
        }
        if (Float.isNaN(yaw) || Float.isInfinite(yaw) || Math.abs(yaw) > 360.0f) {
            reasons.add("INVALID_YAW: " + yaw);
        }

        if (!reasons.isEmpty()) {
            flaggedCount.incrementAndGet();
            return BadPacketResult.flagged(reasons);
        }
        return BadPacketResult.clean();
    }

    /**
     * 校验玩家坐标数据包是否合法。
     *
     * @param playerName 玩家名称
     * @param x          X 坐标
     * @param y          Y 坐标
     * @param z          Z 坐标
     * @return 检测结果
     */
    public BadPacketResult checkPosition(String playerName, double x, double y, double z) {
        if (!config.getSecurity().getSuperEvolution().isAntiBadPackets()) {
            return BadPacketResult.clean();
        }
        totalChecks.incrementAndGet();

        List<String> reasons = new ArrayList<>();
        if (Double.isNaN(x) || Double.isInfinite(x)) {
            reasons.add("INVALID_POSITION_X: " + x);
        }
        if (Double.isNaN(y) || Double.isInfinite(y)) {
            reasons.add("INVALID_POSITION_Y: " + y);
        }
        if (Double.isNaN(z) || Double.isInfinite(z)) {
            reasons.add("INVALID_POSITION_Z: " + z);
        }

        if (!reasons.isEmpty()) {
            flaggedCount.incrementAndGet();
            return BadPacketResult.flagged(reasons);
        }
        return BadPacketResult.clean();
    }

    /** 获取模块运行状态 */
    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("enabled", config.getSecurity().getSuperEvolution().isAntiBadPackets());
        s.put("totalChecks", totalChecks.get());
        s.put("flaggedCount", flaggedCount.get());
        return s;
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
