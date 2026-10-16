package com.aluer.platform;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BukkitCompatTest {

    // 测试用最小接口，模拟不同版本下玩家/服务器对象的方法形态，
    // 以验证 BukkitCompat 的反射回退链，而无需真实 Bukkit 运行时。

    /** 现代 Paper：Player 直接具备 getPing() */
    interface ModernPlayer { int getPing(); }

    /** 传统 Spigot：Player 无 getPing()，但具备 spigot().getPing() */
    interface LegacyPlayer { Object spigot(); }
    interface LegacySpigot { int getPing(); }

    /** 仅具备传统 kickPlayer(String) 的对象 */
    interface Kickable { void kickPlayer(String reason); }

    /** 仅具备传统 broadcastMessage(String) 的服务器对象 */
    interface Broadcastable { void broadcastMessage(String message); }

    /** 具备 getTPS() 的服务器对象 */
    interface TpsServer { double[] getTPS(); }

    /** 不具备任何 TPS 方法的服务器对象（模拟原版 CraftBukkit） */
    interface VanillaServer { }

    @Test
    void getPingModernPlayerPath() {
        ModernPlayer player = proxy(ModernPlayer.class, (p, m, a) -> 42);
        assertEquals(42, BukkitCompat.getPing(player));
    }

    @Test
    void getPingSpigotFallbackPath() {
        // 传统 Spigot：Player 无 getPing()，通过 spigot().getPing() 获取
        LegacySpigot spigot = proxy(LegacySpigot.class, (p, m, a) -> 7);
        LegacyPlayer player = proxy(LegacyPlayer.class, (p, m, a) -> spigot);
        assertEquals(7, BukkitCompat.getPing(player));
    }

    @Test
    void getPingReturnsZeroWhenNoPingApi() {
        VanillaServer player = proxy(VanillaServer.class, (p, m, a) -> null);
        assertEquals(0, BukkitCompat.getPing(player));
        assertEquals(0, BukkitCompat.getPing(null));
    }

    @Test
    void kickUsesLegacyKickPlayer() {
        List<String> kicked = new ArrayList<>();
        Kickable player = proxy(Kickable.class, (p, m, a) -> {
            kicked.add((String) a[0]);
            return null;
        });
        boolean ok = BukkitCompat.kick(player, "作弊");
        assertTrue(ok);
        assertEquals(List.of("作弊"), kicked);
    }

    @Test
    void kickReturnsFalseWhenNoKickApi() {
        assertFalse(BukkitCompat.kick(null, "x"));
        VanillaServer noKick = proxy(VanillaServer.class, (p, m, a) -> null);
        assertFalse(BukkitCompat.kick(noKick, "x"));
    }

    @Test
    void broadcastUsesLegacyBroadcastMessage() {
        List<String> messages = new ArrayList<>();
        Broadcastable server = proxy(Broadcastable.class, (p, m, a) -> {
            messages.add((String) a[0]);
            return null;
        });
        BukkitCompat.broadcast(server, "hello");
        assertEquals(List.of("hello"), messages);
    }

    @Test
    void broadcastIgnoresNullServer() {
        // 不应抛异常
        BukkitCompat.broadcast(null, "hello");
        BukkitCompat.broadcast(proxy(VanillaServer.class, (p, m, a) -> null), "hello");
    }

    @Test
    void getTpsReturnsServerValues() {
        TpsServer server = proxy(TpsServer.class, (p, m, a) -> new double[]{19.5, 19.0, 18.5});
        assertArrayEquals(new double[]{19.5, 19.0, 18.5}, BukkitCompat.getTps(server), 0.0001);
    }

    @Test
    void getTpsFallsBackToFullSpeed() {
        assertArrayEquals(new double[]{20.0, 20.0, 20.0}, BukkitCompat.getTps(null), 0.0);
        VanillaServer noTps = proxy(VanillaServer.class, (p, m, a) -> null);
        assertArrayEquals(new double[]{20.0, 20.0, 20.0}, BukkitCompat.getTps(noTps), 0.0);
    }

    @Test
    void classPresenceDetection() {
        assertTrue(BukkitCompat.isClassPresent("java.lang.String"));
        assertFalse(BukkitCompat.isClassPresent("com.aluer.platform.DoesNotExist"));
    }

    // ─── 辅助 ──────────────────────────────────────────

    private interface InvocationHandler {
        Object handle(Object proxy, java.lang.reflect.Method method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> handler.handle(proxy, method, args));
    }
}
