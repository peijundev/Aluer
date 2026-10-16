package com.aluer.platform;

/**
 * Minecraft 服务端 / 代理端 / 模组加载器的平台类型枚举。
 *
 * Aluer ServerGuard 的目标是覆盖所有主流 Minecraft 服务端形态，本枚举是
 * 跨平台支持的核心分类模型，运行时由 {@link PlatformDetector} 通过反射探测：
 *
 * <ul>
 *   <li><b>Bukkit 系</b>（{@link #isBukkitFamily()} 为 true）：
 *       使用 plugin.yml + JavaPlugin 插件体系，是 Agent 可直接运行的一类。</li>
 *   <li><b>代理端</b>（{@link #isProxy()} 为 true）：
 *       位于玩家与后端服务器之间，插件 API 与 Bukkit 完全不同。</li>
 *   <li><b>模组加载器</b>（{@link #isModded()} 为 true）：
 *       使用 mod 体系（Forge/Fabric/Sponge），非 plugin.yml 体系。</li>
 * </ul>
 *
 * 该分类使得上层逻辑可以用统一的 {@link ServerPlatform} 抽象判断“当前环境能做什么”，
 * 而不是散落着针对某个具体服务端的硬编码判断。
 */
public enum PlatformType {

    // ─── Bukkit 系（JavaPlugin 插件体系） ────────────────────────────────

    /** 原版 CraftBukkit（无 Spigot/Paper 补丁，能力最弱） */
    VANILLA(false, false, true),

    /** Spigot（含 getTPS、spigot().getPing() 等补丁 API） */
    SPIGOT(false, false, true),

    /** Paper 及其衍生（Adventure、异步区块加载、网络包 API 等） */
    PAPER(false, false, true),

    /** Purpur（Paper 的进一步定制分支） */
    PURPUR(false, false, true),

    /** Folia（Paper 的分线程调度变体，需 folia-supported 声明） */
    FOLIA(false, false, true),

    /** Glowstone（无 NMS 的 Bukkit 轻量实现） */
    GLOWSTONE(false, false, true),

    // ─── 代理端 ────────────────────────────────────────────────

    /** BungeeCord（经典代理端） */
    BUNGEECORD(true, false, false),

    /** Waterfall（BungeeCord 的优化分支） */
    WATERFALL(true, false, false),

    /** Velocity（现代高性能代理端） */
    VELOCITY(true, false, false),

    // ─── 模组加载器 ────────────────────────────────────────────

    /** MinecraftForge（经典模组加载器） */
    FORGE(false, true, false),

    /** NeoForge（Forge 1.20.1+ 的分支） */
    NEOFORGE(false, true, false),

    /** Fabric（轻量模组加载器） */
    FABRIC(false, true, false),

    /** Sponge（面向插件的模组加载器） */
    SPONGE(false, true, false),

    /** 无法识别的平台（探测失败时的兜底值） */
    UNKNOWN(false, false, false);

    /** 是否为代理端 */
    private final boolean proxy;

    /** 是否为模组加载器 */
    private final boolean modded;

    /** 是否为 Bukkit 插件体系（插件可直接运行） */
    private final boolean bukkitFamily;

    PlatformType(boolean proxy, boolean modded, boolean bukkitFamily) {
        this.proxy = proxy;
        this.modded = modded;
        this.bukkitFamily = bukkitFamily;
    }

    /**
     * 是否为 Bukkit 插件体系。
     *
     * <p>仅 Bukkit 系平台能直接加载 plugin.yml 描述的 Agent 插件；
     * 代理端与模组加载器需要各自的适配器/描述符。</p>
     */
    public boolean isBukkitFamily() {
        return bukkitFamily;
    }

    /** 是否为代理端（BungeeCord/Waterfall/Velocity） */
    public boolean isProxy() {
        return proxy;
    }

    /** 是否为模组加载器（Forge/NeoForge/Fabric/Sponge） */
    public boolean isModded() {
        return modded;
    }
}
