package com.aluer.monitor;

import com.aluer.config.ServerGuardConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraft 服务器进程定位器。
 *
 * <p>采用 JDK 原生 {@link ProcessHandle} API 遍历系统进程，识别正在运行的
 * Minecraft 服务器进程，替代脆弱的 {@code pgrep -f} 正则匹配。</p>
 *
 * <p><b>为什么不用 pgrep</b>：pgrep -f 将关键词当作正则表达式处理（"." 会匹配任意字符），
 * 且配置里去掉 build 号的关键词（如 {@code paper-1.21.11.jar}）无法命中真实进程名
 * （如 {@code paper-1.21.11-113.jar}）——因为 {@code .jar} 与 {@code -113.jar} 在
 * 子串匹配下并不相等，从而出现"识别不到服务器进程"的误判。</p>
 *
 * <p>本类通过「JAR 名归一化 + {@code -jar} 参数提取」解决上述 build 号问题，
 * 并返回结构化的 {@link ProcessInfo}（PID / 命令行 / JAR 名 / 启动时间 / CPU 时长 / 用户），
 * 供监控、告警与运维展示使用。</p>
 *
 * <p>此外，当命令行核心名无法识别时（例如自定义启动器或重命名的 JAR，
 * 如 {@code java -jar ModLoad.jar} 启动的 Youer 服务端），本类会进一步读取进程
 * 工作目录（Linux {@code /proc/<pid>/cwd}），检查是否存在 Minecraft 服务器特征文件
 * （{@code server.properties}/{@code eula.txt}/{@code logs/latest.log} 等）作为兜底判定，
 * 确保不依赖启动器命名即可完成「自动发现」。</p>
 */
public final class MinecraftProcessDetector {

    /** 从命令行提取 {@code -jar xxx.jar} 参数的正则（大小写不敏感） */
    private static final Pattern JAR_ARG_PATTERN =
            Pattern.compile("(?i)-jar\\s+([^\\s]+\\.jar)");

    /**
     * 已知 Minecraft 服务端核心名特征。
     *
     * <p>用于在用户未显式配置 processName / jarFile 时的通用识别，
     * 覆盖 Bukkit 系（Paper/Spigot/Purpur/Pufferfish/Folia/CraftBukkit/Glowstone）、
     * 混合端（Mohist/Arclight）与模组加载器（Forge/NeoForge/Fabric/Sponge）。</p>
     */
    private static final List<String> KNOWN_SERVER_CORE_MARKERS = List.of(
            "paper", "spigot", "purpur", "pufferfish", "folia",
            "craftbukkit", "glowstone", "mohist", "arclight",
            "forge", "neoforge", "fabric", "sponge",
            "youer");

    /**
     * 出现在进程工作目录即高度指向 Minecraft 服务器的特征文件。
     *
     * <p>作为命令行核心名识别失败时的兜底证据，覆盖自定义启动器（如 {@code ModLoad.jar}）、
     * 重命名 JAR 等无法从命令行文本判断核心名的场景。命中任一文件即判定为服务器进程。</p>
     */
    private static final List<String> MINECRAFT_SERVER_MARKER_FILES = List.of(
            "server.properties", "eula.txt", "bukkit.yml", "spigot.yml",
            "paper.yml", "purpur.yml", "pufferfish.yml", "fabricloader.log",
            "logs/latest.log");

    private MinecraftProcessDetector() {
        // 纯静态工具类，禁止实例化
    }

    // ─── 高层查询 ──────────────────────────────────────

    /**
     * 判断 Minecraft 服务器进程是否正在运行。
     *
     * @param config 服务器配置，读取 processName / jarFile 作为命令行匹配关键词
     * @return true 表示检测到至少一个服务器进程
     */
    public static boolean isRunning(ServerGuardConfig config) {
        return findProcess(config).isPresent();
    }

    /**
     * 定位 Minecraft 服务器进程的 PID。
     *
     * <p>存在多个匹配进程时返回首个匹配者（通常单机仅部署一台服务器）。</p>
     *
     * @param config 服务器配置
     * @return 服务器进程 PID，未找到返回空 Optional
     */
    public static Optional<Long> findProcess(ServerGuardConfig config) {
        return findAllProcesses(config).stream()
                .map(ProcessInfo::getPid)
                .findFirst();
    }

    /**
     * 定位所有匹配进程的 PID。
     *
     * @param config 服务器配置
     * @return 全部匹配进程的 PID 列表（可能为空）
     */
    public static List<Long> findPids(ServerGuardConfig config) {
        return findAllProcesses(config).stream()
                .map(ProcessInfo::getPid)
                .toList();
    }

    /**
     * 定位首个匹配进程的结构化信息。
     *
     * @param config 服务器配置
     * @return 进程详情，未找到返回空 Optional
     */
    public static Optional<ProcessInfo> findProcessInfo(ServerGuardConfig config) {
        return findAllProcesses(config).stream().findFirst();
    }

    /**
     * 定位所有匹配进程的结构化信息。
     *
     * @param config 服务器配置
     * @return 全部匹配进程的详情列表（可能为空）
     */
    public static List<ProcessInfo> findAllProcesses(ServerGuardConfig config) {
        String processName = config.getMinecraft().getProcessName();
        String jarFile = config.getMinecraft().getJarFile();
        try {
            List<ProcessInfo> result = new ArrayList<>();
            ProcessHandle.allProcesses()
                    .filter(ph -> isMinecraftServerProcess(ph, processName, jarFile))
                    .forEach(ph -> result.add(ProcessInfo.from(ph)));
            return result;
        } catch (Exception e) {
            // SecurityException 等：无权限枚举进程时按"未找到"处理，避免向上抛异常
            return List.of();
        }
    }

    // ─── 匹配核心（包内可见，供单元测试） ─────────────

    /**
     * 判断某进程是否为 Minecraft 服务器进程。
     *
     * <p>排除 Aluer 自身 JVM（Aluer 与 Minecraft 服务器是相互独立的两个 JVM），
     * 然后交由 {@link #commandLineMatches(String, String, String)} 做命令行匹配。</p>
     */
    private static boolean isMinecraftServerProcess(ProcessHandle ph, String processName, String jarFile) {
        // 排除自身：Aluer 自身命令行可能含 "java"，不能算作 Minecraft 服务器
        if (ph.pid() == ProcessHandle.current().pid()) {
            return false;
        }
        String commandLine = ph.info().commandLine().orElse("");
        if (commandLineMatches(commandLine, processName, jarFile)) {
            return true;
        }
        // 命令行匹配失败时，用进程工作目录下的 Minecraft 特征文件兜底识别。
        // 覆盖自定义启动器/重命名 JAR（如 java -jar ModLoad.jar 启动的 Youer 服务端）
        // 等核心名无法从命令行文本判断的情况。
        // 前置条件：命令行须含 .jar（jar 启动的 Java 进程），避免把恰好工作目录相同的
        // 终端 / Shell（konsole、zsh 等）误判为服务器进程。
        if (!commandLine.toLowerCase().contains(".jar")) {
            return false;
        }
        String cwd = resolveWorkingDirectory(ph);
        return cwd != null && hasMinecraftServerFiles(Path.of(cwd));
    }

    /**
     * 判断一条命令行是否匹配 Minecraft 服务器进程（纯逻辑，便于测试）。
     *
     * <p>匹配优先级：</p>
     * <ol>
     *   <li>{@code processName} 子串命中（服务名 / 进程标识）。</li>
     *   <li>{@code jarFile} 精确子串命中。</li>
     *   <li>{@code -jar} 参数提取出的 JAR 名与配置 JAR 名归一化相等（解决 build 号差异）。</li>
     *   <li>{@code jarFile} 归一化后的基础名字串命中（兜底）。</li>
     *   <li>未显式配置关键词时，按已知核心名特征做通用识别。</li>
     * </ol>
     */
    static boolean commandLineMatches(String commandLine, String processName, String jarFile) {
        if (commandLine == null || commandLine.isBlank()) {
            // 无命令行信息（权限不足或进程已退出），无法判定
            return false;
        }
        // 是否显式配置了匹配关键词（配置了则走精确匹配，不启用通用识别）
        boolean hasExplicitKeyword = (processName != null && !processName.isBlank())
                || (jarFile != null && !jarFile.isBlank());

        if (processName != null && !processName.isBlank() && commandLine.contains(processName)) {
            return true;
        }
        if (jarFile != null && !jarFile.isBlank()) {
            if (commandLine.contains(jarFile)) {
                return true;
            }
            String actualJar = extractJarName(commandLine);
            if (actualJar != null && jarNamesMatch(jarFile, actualJar)) {
                return true;
            }
            String jarBase = normalizeJarName(jarFile);
            if (!jarBase.isBlank() && commandLine.contains(jarBase)) {
                return true;
            }
        }
        // 未显式配置关键词时，按已知 Minecraft 核心特征做通用识别
        return !hasExplicitKeyword && looksLikeMinecraftServer(commandLine);
    }

    /**
     * 归一化 JAR 名：去掉路径与 {@code .jar} 后缀，并剥离末尾 build 号。
     *
     * <p>例如：</p>
     * <ul>
     *   <li>{@code /opt/minecraft/paper-1.21.11-113.jar} → {@code paper-1.21.11}</li>
     *   <li>{@code paper-1.21.11.jar} → {@code paper-1.21.11}</li>
     *   <li>{@code spigot-1.12.2.jar} → {@code spigot-1.12.2}</li>
     * </ul>
     *
     * <p>build 号剥离规则：仅去除末尾「-纯数字」段（如 {@code -113}），
     * 不影响形如 {@code -1.21.11} 的版本号（版本号含点，非纯数字）。</p>
     */
    static String normalizeJarName(String name) {
        if (name == null) {
            return "";
        }
        String s = name.trim();
        // 去掉路径，仅保留文件名
        int slash = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        if (slash >= 0) {
            s = s.substring(slash + 1);
        }
        // 去掉 .jar 后缀（大小写不敏感）
        if (s.toLowerCase().endsWith(".jar")) {
            s = s.substring(0, s.length() - 4);
        }
        // 去掉末尾 build 号（-纯数字）
        s = s.replaceFirst("-\\d+$", "");
        return s;
    }

    /**
     * 从命令行提取 {@code -jar} 参数对应的 JAR 文件名。
     *
     * @param commandLine 进程完整命令行
     * @return JAR 文件名（不含路径），未提取到返回 null
     */
    static String extractJarName(String commandLine) {
        if (commandLine == null || commandLine.isBlank()) {
            return null;
        }
        Matcher matcher = JAR_ARG_PATTERN.matcher(commandLine);
        if (matcher.find()) {
            String path = matcher.group(1);
            int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            return slash >= 0 ? path.substring(slash + 1) : path;
        }
        return null;
    }

    /**
     * 判断配置 JAR 名与实际 JAR 名是否「归一化相等」。
     *
     * <p>两者分别做 {@link #normalizeJarName(String)} 后再忽略大小写比较，
     * 使 {@code paper-1.21.11.jar} 与 {@code paper-1.21.11-113.jar} 判定为相等。</p>
     */
    static boolean jarNamesMatch(String configuredJar, String actualJar) {
        String a = normalizeJarName(configuredJar);
        String b = normalizeJarName(actualJar);
        return !a.isBlank() && a.equalsIgnoreCase(b);
    }

    /**
     * 判断命令行是否「看起来像」Minecraft 服务器进程（通用特征识别）。
     *
     * <p>当用户未显式配置 processName / jarFile 时作为兜底识别：
     * 命令行需包含 {@code .jar}（jar 启动），且命中已知核心名特征
     * （paper/spigot/purpur/folia/forge/fabric 等）即判定为服务器进程。
     * 这样即便不预先填写具体核心 JAR 名，也能跨平台、跨版本定位服务器。</p>
     */
    static boolean looksLikeMinecraftServer(String commandLine) {
        if (commandLine == null || commandLine.isBlank()) {
            return false;
        }
        String lower = commandLine.toLowerCase();
        if (!lower.contains(".jar")) {
            return false;
        }
        for (String marker : KNOWN_SERVER_CORE_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    // ─── 工作目录特征兜底 ──────────────────────────────

    /**
     * 解析进程的工作目录（Linux 专属）。
     *
     * <p>JDK 的 {@link ProcessHandle.Info} 不提供工作目录字段，故读取 {@code /proc/<pid>/cwd}
     * 符号链接指向的真实路径。权限不足或进程已退出时返回 null，由调用方按「无法判定」处理。</p>
     *
     * @param ph 目标进程句柄
     * @return 工作目录绝对路径，无法解析返回 null
     */
    static String resolveWorkingDirectory(ProcessHandle ph) {
        if (ph == null) {
            return null;
        }
        Path cwdLink = Path.of("/proc", String.valueOf(ph.pid()), "cwd");
        try {
            if (Files.exists(cwdLink)) {
                return cwdLink.toRealPath().toString();
            }
        } catch (IOException | RuntimeException e) {
            // 无权限（其他用户进程）或进程恰好退出：按无法解析处理，不向上抛异常
        }
        return null;
    }

    /**
     * 判断目录下是否存在 Minecraft 服务器特征文件（纯逻辑，便于测试）。
     *
     * <p>命中任一特征文件（{@code server.properties}/{@code eula.txt}/
     * {@code logs/latest.log}/{@code fabricloader.log} 等）即返回 true。
     * 这些文件是所有 Minecraft 服务端（含自定义启动器、重命名 JAR、混合端）的通用产物，
     * 与核心名无关，因此是「自动发现」最可靠的一层兜底。</p>
     *
     * @param dir 待检测目录
     * @return true 表示该目录下存在服务器特征文件
     */
    static boolean hasMinecraftServerFiles(Path dir) {
        if (dir == null) {
            return false;
        }
        for (String marker : MINECRAFT_SERVER_MARKER_FILES) {
            if (Files.isRegularFile(dir.resolve(marker))) {
                return true;
            }
        }
        return false;
    }

    // ─── 结果类 ────────────────────────────────────────

    /**
     * Minecraft 服务器进程的结构化信息。
     *
     * <p>封装 {@link ProcessHandle.Info} 提供的进程元数据，便于监控、告警与展示。</p>
     */
    public static final class ProcessInfo {
        private final long pid;
        private final String commandLine;
        private final String jarName;
        private final String workingDirectory;
        private final boolean alive;
        private final Instant startTime;
        private final Duration cpuDuration;
        private final String user;

        ProcessInfo(long pid, String commandLine, String jarName, String workingDirectory,
                    boolean alive, Instant startTime, Duration cpuDuration, String user) {
            this.pid = pid;
            this.commandLine = commandLine;
            this.jarName = jarName;
            this.workingDirectory = workingDirectory;
            this.alive = alive;
            this.startTime = startTime;
            this.cpuDuration = cpuDuration;
            this.user = user;
        }

        /** 从 {@link ProcessHandle} 构建进程信息 */
        static ProcessInfo from(ProcessHandle handle) {
            ProcessHandle.Info info = handle.info();
            String commandLine = info.commandLine().orElse("");
            return new ProcessInfo(
                    handle.pid(),
                    commandLine,
                    extractJarName(commandLine),
                    resolveWorkingDirectory(handle),
                    handle.isAlive(),
                    info.startInstant().orElse(null),
                    info.totalCpuDuration().orElse(null),
                    info.user().orElse(null));
        }

        public long getPid() { return pid; }
        public String getCommandLine() { return commandLine; }
        public String getJarName() { return jarName; }
        public String getWorkingDirectory() { return workingDirectory; }
        public boolean isAlive() { return alive; }
        public Instant getStartTime() { return startTime; }
        public Duration getCpuDuration() { return cpuDuration; }
        public String getUser() { return user; }

        @Override
        public String toString() {
            return String.format("ProcessInfo{pid=%d, jar=%s, cwd=%s, alive=%s, user=%s}",
                    pid, jarName, workingDirectory, alive, user);
        }
    }
}
