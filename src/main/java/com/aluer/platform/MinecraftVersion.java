package com.aluer.platform;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraft 版本号 —— 可解析、可比较、可判断支持范围的不可变值对象。
 *
 * <p>用于替代散落各处的字符串比较，例如判断“是否 ≥ 1.12.2”“是否 ≥ 1.16.5（Adventure 可用）”。
 * 版本格式遵循 Minecraft 的语义化版本（major.minor.patch，例如 1.21.1、1.12.2）。</p>
 *
 * <p>实现为 {@link Comparable}，并保证与 {@link #equals(Object)} 一致，可直接放入 TreeSet / 排序。</p>
 */
public final class MinecraftVersion implements Comparable<MinecraftVersion> {

    /** 匹配 "1.12"、"1.12.2"、"1.21.1" 等数字版本段的模式 */
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    /** 本项目支持的最低版本：1.12.2（覆盖 1.12.2 至今所有版本） */
    public static final MinecraftVersion MIN_SUPPORTED = new MinecraftVersion(1, 12, 2);

    /** 引入 Adventure Component API 的版本：1.16.5（Paper 系） */
    public static final MinecraftVersion ADVENTURE_AVAILABLE = new MinecraftVersion(1, 16, 5);

    private final int major;
    private final int minor;
    private final int patch;

    public MinecraftVersion(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("版本号不能为负数: " + major + "." + minor + "." + patch);
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public int getMajor() { return major; }
    public int getMinor() { return minor; }
    public int getPatch() { return patch; }

    /**
     * 从任意字符串中提取 Minecraft 版本号。
     *
     * <p>能够容忍诸如 "git-Paper-1.21.1 (MC: 1.21.1)"、"" (空) 等非纯版本字符串，
     * 只要其中包含 "X.Y[.Z]" 片段即可。找不到时返回 null 而非抛异常。</p>
     *
     * @param raw 原始字符串
     * @return 解析出的版本，无法解析时返回 null
     */
    public static MinecraftVersion parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher matcher = VERSION_PATTERN.matcher(raw);
        if (matcher.find()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            return new MinecraftVersion(major, minor, patch);
        }
        return null;
    }

    /** 是否不低于给定版本（this &gt;= other） */
    public boolean isAtLeast(int major, int minor, int patch) {
        return compareTo(new MinecraftVersion(major, minor, patch)) >= 0;
    }

    /** 是否不低于给定版本（this &gt;= other） */
    public boolean isAtLeast(MinecraftVersion other) {
        return compareTo(other) >= 0;
    }

    /** 是否在本项目支持范围内（≥ 1.12.2） */
    public boolean isSupported() {
        return isAtLeast(MIN_SUPPORTED);
    }

    /** 是否具备 Adventure Component API（Paper 1.16.5+ 才可用） */
    public boolean hasAdventure() {
        return isAtLeast(ADVENTURE_AVAILABLE);
    }

    @Override
    public int compareTo(MinecraftVersion o) {
        int c = Integer.compare(this.major, o.major);
        if (c != 0) return c;
        c = Integer.compare(this.minor, o.minor);
        if (c != 0) return c;
        return Integer.compare(this.patch, o.patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MinecraftVersion that)) return false;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
