package com.aluer.platform;

/**
 * Bukkit 系平台的 {@link ServerPlatform} 实现。
 *
 * <p>承载 {@link PlatformDetector} 探测出的平台类型与版本，并将 TPS、在线人数等
 * 运行时能力委托给 {@link BukkitCompat}（跨版本反射兼容层）。</p>
 *
 * <p>本类是一个纯数据载体 + 能力门面，不包含任何探测逻辑，
 * 以便单元测试时可以直接构造任意平台状态。</p>
 */
public class BukkitPlatform implements ServerPlatform {

    private final PlatformType type;
    private final MinecraftVersion version;
    private final String rawVersion;

    /**
     * @param type       探测出的平台类型
     * @param version    解析出的 Minecraft 版本（可能为 null）
     * @param rawVersion 服务端返回的原始版本字符串
     */
    public BukkitPlatform(PlatformType type, MinecraftVersion version, String rawVersion) {
        this.type = type == null ? PlatformType.UNKNOWN : type;
        this.version = version;
        this.rawVersion = rawVersion;
    }

    @Override
    public PlatformType type() {
        return type;
    }

    @Override
    public String platformName() {
        return type.name();
    }

    @Override
    public MinecraftVersion minecraftVersion() {
        return version;
    }

    @Override
    public String rawVersion() {
        return rawVersion;
    }

    @Override
    public boolean isSupported() {
        return version != null && version.isSupported();
    }

    @Override
    public boolean isBukkitFamily() {
        return type.isBukkitFamily();
    }

    @Override
    public boolean isProxy() {
        return type.isProxy();
    }

    @Override
    public boolean isModded() {
        return type.isModded();
    }

    @Override
    public boolean hasAdventure() {
        return BukkitCompat.hasAdventure();
    }

    @Override
    public double[] tps() {
        return BukkitCompat.getTps();
    }

    @Override
    public int onlinePlayerCount() {
        return BukkitCompat.getOnlinePlayerCount();
    }

    @Override
    public String toString() {
        return "BukkitPlatform{type=" + type + ", version=" + version + ", raw=" + rawVersion + "}";
    }
}
