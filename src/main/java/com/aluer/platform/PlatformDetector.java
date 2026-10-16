package com.aluer.platform;

/**
 * 平台探测器 —— 在运行时识别当前运行的服务端/代理端/模组加载器类型与版本。
 *
 * <p>探测策略分两层：</p>
 * <ol>
 *   <li><b>字符串启发式</b>（{@link #detectType(String, String)}）：根据服务端版本字符串和
 *       服务端类包名做关键字匹配，纯函数、可单元测试。</li>
 *   <li><b>类存在性探测</b>（{@link #detect()}）：在 Bukkit 运行时通过反射检查
 *       Paper/Purpur/Folia/Spigot 等标志类的存在性，得到更精确的结论。</li>
 * </ol>
 *
 * <p>此外 {@link #detectFromClassName(String)} 扩展了对代理端（BungeeCord/Waterfall/Velocity）
 * 与模组加载器（Forge/NeoForge/Fabric/Sponge）的识别能力，用于引擎侧或工具场景。</p>
 */
public final class PlatformDetector {

    private PlatformDetector() {
        // 工具类不可实例化
    }

    // ─── 运行时探测 ────────────────────────────────────

    /**
     * 探测当前运行平台（面向 Bukkit 插件运行时）。
     *
     * <p>优先通过版本/包名字符串启发式判断，再结合标志类存在性做精确修正。</p>
     */
    public static ServerPlatform detect() {
        String version = BukkitCompat.getServerVersion();
        String packageName = getServerPackageName();

        PlatformType type = detectType(version, packageName);

        // 字符串启发式只区分到 Paper/Spigot/Vanilla 粒度，进一步用标志类精确到 Purpur/Folia
        if (type == PlatformType.VANILLA || type == PlatformType.SPIGOT || type == PlatformType.PAPER) {
            if (BukkitCompat.isClassPresent("org.purpurmc.purpur.PurpurConfig")) {
                type = PlatformType.PURPUR;
            } else if (BukkitCompat.isClassPresent("io.papermc.paper.threadedregions.RegionizedServer")) {
                type = PlatformType.FOLIA;
            } else if (BukkitCompat.isClassPresent("io.papermc.paper.configuration.GlobalConfiguration")) {
                type = PlatformType.PAPER;
            } else if (BukkitCompat.isClassPresent("org.spigotmc.SpigotConfig")) {
                type = PlatformType.SPIGOT;
            }
        }

        return new BukkitPlatform(type, MinecraftVersion.parse(version), version);
    }

    /**
     * 纯函数：根据服务端版本字符串和包名判断 Bukkit 系平台类型。
     *
     * <p>匹配顺序按“更具体优先”：Purpur → Folia → Paper → Spigot → Glowstone → Vanilla。</p>
     *
     * @param versionString    服务端版本字符串（如 "git-Paper-1.21.1 (MC: 1.21.1)"）
     * @param serverPackageName 服务端类包名（如 "org.bukkit.craftbukkit.v1_21_R1"）
     */
    public static PlatformType detectType(String versionString, String serverPackageName) {
        String v = lower(versionString);
        String p = lower(serverPackageName);

        if (contains(v, "purpur") || contains(p, "purpur")) {
            return PlatformType.PURPUR;
        }
        if (contains(v, "folia") || contains(p, "folia")) {
            return PlatformType.FOLIA;
        }
        if (contains(v, "paper") || contains(v, "pufferfish")
                || contains(p, "paper") || contains(p, "pufferfish")) {
            return PlatformType.PAPER;
        }
        if (contains(v, "spigot") || contains(p, "spigot")) {
            return PlatformType.SPIGOT;
        }
        if (contains(v, "glowstone") || contains(p, "glowstone")) {
            return PlatformType.GLOWSTONE;
        }
        if (contains(v, "bukkit") || contains(v, "craftbukkit") || contains(p, "craftbukkit")) {
            return PlatformType.VANILLA;
        }
        return PlatformType.UNKNOWN;
    }

    /**
     * 根据类名/版本字符串识别任意平台类型（含代理端与模组加载器）。
     *
     * <p>用于引擎侧能力上报或工具场景，Agent 运行时通常只需 {@link #detect()}。</p>
     */
    public static PlatformType detectFromClassName(String classNameOrVersion) {
        String c = lower(classNameOrVersion);

        // 代理端
        if (contains(c, "velocity")) {
            return PlatformType.VELOCITY;
        }
        if (contains(c, "waterfall")) {
            return PlatformType.WATERFALL;
        }
        if (contains(c, "bungee")) {
            return PlatformType.BUNGEECORD;
        }

        // 模组加载器
        if (contains(c, "neoforged") || contains(c, "neoforge")) {
            return PlatformType.NEOFORGE;
        }
        if (contains(c, "fabric")) {
            return PlatformType.FABRIC;
        }
        if (contains(c, "sponge")) {
            return PlatformType.SPONGE;
        }
        if (contains(c, "forge")) {
            return PlatformType.FORGE;
        }

        // 回退到 Bukkit 系启发式
        return detectType(classNameOrVersion, classNameOrVersion);
    }

    // ─── 内部辅助 ──────────────────────────────────────

    private static String getServerPackageName() {
        try {
            return org.bukkit.Bukkit.getServer().getClass().getPackageName();
        } catch (Throwable t) {
            return "";
        }
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && needle != null && haystack.contains(needle);
    }
}
