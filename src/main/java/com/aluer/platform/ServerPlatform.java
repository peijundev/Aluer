package com.aluer.platform;

/**
 * 服务端平台抽象 —— 屏蔽底层服务端/代理端/模组加载器差异的统一视图。
 *
 * <p>Agent 逻辑只依赖本接口，而不直接依赖 Bukkit / BungeeCord / Velocity / Forge 等具体 API。
 * 每种平台提供各自的实现（例如 Bukkit 系的 {@link BukkitPlatform}），
 * 使同一个核心引擎能无差别地运行在不同服务端形态上。</p>
 *
 * <p>能力查询（是否代理端、是否支持 Adventure、TPS、在线人数等）均收敛到此接口，
 * 上层代码无需再写平台判断分支。</p>
 */
public interface ServerPlatform {

    /** 平台类型 */
    PlatformType type();

    /** 平台显示名（例如 "Paper"、"Spigot"、"Purpur"、"Velocity"） */
    String platformName();

    /** Minecraft 游戏版本（解析自服务端返回的版本字符串，可能为 null） */
    MinecraftVersion minecraftVersion();

    /** 服务端返回的原始版本字符串（例如 "git-Paper-1.21.1 (MC: 1.21.1)"） */
    String rawVersion();

    /** 当前版本是否在本项目支持范围内（≥ 1.12.2） */
    boolean isSupported();

    /** 是否属于 Bukkit 插件体系（Agent 可直接以 plugin.yml 形式运行） */
    boolean isBukkitFamily();

    /** 是否为代理端 */
    boolean isProxy();

    /** 是否为模组加载器 */
    boolean isModded();

    /** 是否具备 Adventure Component API（Paper 1.16.5+） */
    boolean hasAdventure();

    /** 服务器 TPS 数组（[1m, 5m, 15m]），不支持时回退为 {20, 20, 20} */
    double[] tps();

    /** 当前在线玩家数 */
    int onlinePlayerCount();
}
