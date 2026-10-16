package com.aluer.platform;

import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Bukkit 跨版本反射兼容层。
 *
 * <p>Bukkit/Spigot/Paper 的 API 在不同 Minecraft 版本间存在差异，直接编译期调用
 * 某些方法会导致在旧版本服务端上抛出 {@link NoSuchMethodError}。本工具类将所有
 * “版本敏感”的调用收敛到反射实现，并在运行时按能力逐级回退：</p>
 *
 * <ul>
 *   <li><b>getPing</b>：{@code Player#getPing()}（Paper 1.16.5+）→ {@code Player.Spigot#getPing()}（Spigot 1.7.9+）→ 0</li>
 *   <li><b>kick</b>：{@code Player#kick(Component)}（Adventure，Paper 1.16.5+）→ {@code Player#kickPlayer(String)}（传统）→ {@code Player#kick()}</li>
 *   <li><b>broadcast</b>：{@code Server#broadcast(Component)}（Adventure）→ {@code Server#broadcastMessage(String)}（传统）</li>
 *   <li><b>TPS</b>：{@code Server#getTPS()}（Spigot/Paper 补丁）→ {20,20,20}（原版回退）</li>
 * </ul>
 *
 * <p>所有方法均以 {@link Object} 接收目标对象并做空值/异常防御，保证在任意版本、
 * 任意平台（甚至测试环境无真实 Bukkit 服务器时）都不会抛出未捕获异常。</p>
 */
public final class BukkitCompat {

    private static final Logger logger = LoggerFactory.getLogger(BukkitCompat.class);

    /** Adventure Component 类的全限定名（Paper 1.16.5+ 才存在） */
    private static final String ADVENTURE_COMPONENT = "net.kyori.adventure.text.Component";

    private BukkitCompat() {
        // 工具类不可实例化
    }

    // ─── 能力探测 ──────────────────────────────────────

    /**
     * 判断指定类是否存在于当前类加载器可见范围。
     *
     * <p>用于探测 Adventure / Paper 专有 API 等“版本敏感”类是否存在。</p>
     */
    public static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, BukkitCompat.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 当前环境是否具备 Adventure Component API（Paper 1.16.5+） */
    public static boolean hasAdventure() {
        return isClassPresent(ADVENTURE_COMPONENT);
    }

    // ─── Ping ──────────────────────────────────────────

    /**
     * 获取玩家延迟（毫秒）。
     *
     * <p>优先调用 {@code Player#getPing()}（Paper/新版 Spigot），
     * 不存在时回退到 {@code Player#spigot().getPing()}（Spigot 1.7.9+），
     * 仍不可用则返回 0。</p>
     */
    public static int getPing(Object player) {
        if (player == null) {
            return 0;
        }
        // 路径 1：Player#getPing()
        Object value = invokeNoArg(player, "getPing");
        if (value instanceof Number number) {
            return number.intValue();
        }
        // 路径 2：Player#spigot().getPing()
        Object spigot = invokeNoArg(player, "spigot");
        if (spigot != null) {
            Object spigotPing = invokeNoArg(spigot, "getPing");
            if (spigotPing instanceof Number number) {
                return number.intValue();
            }
        }
        return 0;
    }

    // ─── Kick ──────────────────────────────────────────

    /**
     * 踢出玩家。
     *
     * <p>Adventure 可用时优先使用 {@code kick(Component)}（保留富文本），
     * 否则回退到传统 {@code kickPlayer(String)}，最后兜底无参 {@code kick()}。</p>
     *
     * @return 是否成功执行了任一踢出调用
     */
    public static boolean kick(Object player, String reason) {
        if (player == null) {
            return false;
        }
        String safeReason = reason == null ? "" : reason;

        // 路径 1：Adventure Component（Paper 1.16.5+）
        if (hasAdventure()) {
            try {
                Class<?> componentClass = Class.forName(ADVENTURE_COMPONENT);
                Object component = componentClass.getMethod("text", String.class).invoke(null, safeReason);
                if (tryInvoke(player, "kick", new Class<?>[]{componentClass}, component)) {
                    return true;
                }
            } catch (Throwable ignored) {
                // Adventure 类存在但方法签名不匹配，继续回退
            }
        }

        // 路径 2：传统 kickPlayer(String)（所有 Bukkit 版本）
        if (tryInvoke(player, "kickPlayer", new Class<?>[]{String.class}, safeReason)) {
            return true;
        }

        // 路径 3：无参 kick()（部分 Paper 版本）
        return tryInvoke(player, "kick", new Class<?>[0]);
    }

    // ─── Broadcast ─────────────────────────────────────

    /**
     * 向全服广播消息（传入 Server 对象，便于测试与复用）。
     *
     * <p>Adventure 可用时使用 {@code broadcast(Component)}，否则使用 {@code broadcastMessage(String)}。</p>
     */
    public static void broadcast(Object server, String message) {
        if (server == null || message == null) {
            return;
        }
        if (hasAdventure()) {
            try {
                Class<?> componentClass = Class.forName(ADVENTURE_COMPONENT);
                Object component = componentClass.getMethod("text", String.class).invoke(null, message);
                if (tryInvoke(server, "broadcast", new Class<?>[]{componentClass}, component)) {
                    return;
                }
            } catch (Throwable ignored) {
                // 回退到传统 API
            }
        }
        tryInvoke(server, "broadcastMessage", new Class<?>[]{String.class}, message);
    }

    /** 向全服广播消息（自动获取当前 Bukkit Server 实例） */
    public static void broadcast(String message) {
        try {
            broadcast(Bukkit.getServer(), message);
        } catch (Throwable t) {
            logger.debug("broadcast failed: {}", t.getMessage());
        }
    }

    // ─── TPS ───────────────────────────────────────────

    /**
     * 获取服务器 TPS 数组 [1m, 5m, 15m]（传入 Server 对象）。
     *
     * <p>{@code Server#getTPS()} 为 Spigot/Paper 补丁方法，原版 CraftBukkit 不存在；
     * 通过反射调用以避免 {@link NoSuchMethodError}，不可用时回退为满速 {20,20,20}。</p>
     */
    public static double[] getTps(Object server) {
        if (server == null) {
            return new double[]{20.0, 20.0, 20.0};
        }
        Object value = invokeNoArg(server, "getTPS");
        if (value instanceof double[] tps) {
            return tps;
        }
        return new double[]{20.0, 20.0, 20.0};
    }

    /** 获取服务器 TPS 数组（自动获取当前 Bukkit Server 实例） */
    public static double[] getTps() {
        try {
            return getTps(Bukkit.getServer());
        } catch (Throwable t) {
            return new double[]{20.0, 20.0, 20.0};
        }
    }

    /** 获取 1 分钟平均 TPS（满速 20.0） */
    public static double getTps1m() {
        double[] tps = getTps();
        return tps.length > 0 ? tps[0] : 20.0;
    }

    // ─── 服务器信息 ────────────────────────────────────

    /** 当前在线玩家数（getOnlinePlayers 在所有 Bukkit 版本均稳定存在） */
    public static int getOnlinePlayerCount() {
        try {
            return Bukkit.getServer().getOnlinePlayers().size();
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 服务端原始版本字符串（例如 "git-Paper-1.21.1 (MC: 1.21.1)"） */
    public static String getServerVersion() {
        try {
            return Bukkit.getServer().getVersion();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    // ─── 反射辅助 ──────────────────────────────────────

    /** 无参调用指定方法并返回结果；方法不存在或调用失败返回 null */
    private static Object invokeNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 以指定参数类型调用方法；返回 true 表示调用成功（含返回 void 的方法） */
    private static boolean tryInvoke(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, paramTypes);
            method.invoke(target, args);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
