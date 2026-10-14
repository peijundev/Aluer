package com.aluer.monitor;

import com.aluer.config.ServerGuardConfig;

import java.util.Optional;

/**
 * Minecraft 服务器进程检测工具类。
 *
 * <p>采用 JDK 原生 {@link ProcessHandle} API 遍历系统进程，识别正在运行的
 * Minecraft 服务器进程，替代脆弱的 {@code pgrep -f} 正则匹配。</p>
 *
 * <p>为什么不用 pgrep：pgrep -f 将关键词当作正则表达式处理（"." 会匹配任意字符），
 * 且配置里去掉 build 号的关键词（如 paper-1.21.11.jar）无法命中真实进程名
 * （如 paper-1.21.11-113.jar），从而出现"识别不到服务器进程"的误判。</p>
 */
public final class MinecraftProcessDetector {

    private MinecraftProcessDetector() {
        // 纯静态工具类，禁止实例化
    }

    /**
     * 判断 Minecraft 服务器进程是否正在运行。
     *
     * @param config 服务器配置，读取 processName / jarFile 作为命令行匹配关键词
     * @return true 表示检测到至少一个服务器进程，false 表示未检测到
     */
    public static boolean isRunning(ServerGuardConfig config) {
        return findProcess(config).isPresent();
    }

    /**
     * 定位 Minecraft 服务器进程的 PID。
     *
     * @param config 服务器配置，读取 processName / jarFile 作为命令行匹配关键词
     * @return 若找到则返回服务器进程 PID，否则返回空 Optional
     */
    public static Optional<Long> findProcess(ServerGuardConfig config) {
        String processName = config.getMinecraft().getProcessName();
        String jarFile = config.getMinecraft().getJarFile();
        try {
            return ProcessHandle.allProcesses()
                .filter(ph -> isMinecraftServerProcess(ph, processName, jarFile))
                .map(ProcessHandle::pid)
                .findFirst();
        } catch (Exception e) {
            // SecurityException 等：无权限枚举进程时按"未找到"处理，避免向上抛异常
            return Optional.empty();
        }
    }

    /**
     * 判断某个系统进程是否为 Minecraft 服务器进程。
     *
     * <p>匹配依据：进程命令行中包含 processName 或 jarFile 关键词（子串匹配）。
     * 同时排除当前 Aluer 自身 JVM 进程——Aluer 与 Minecraft 服务器是相互独立的
     * 两个 JVM，当关键词设置过宽（如 "java"）时，排除自身可避免自匹配误判。</p>
     */
    private static boolean isMinecraftServerProcess(ProcessHandle ph, String processName, String jarFile) {
        // 排除自身进程：Aluer 自身命令行可能含 "java"，不能算作 Minecraft 服务器
        if (ph.pid() == ProcessHandle.current().pid()) {
            return false;
        }
        String cmd = ph.info().commandLine().orElse("");
        if (cmd.isBlank()) {
            // 无命令行信息（权限不足或进程已退出），无法判定，跳过
            return false;
        }
        if (processName != null && !processName.isBlank() && cmd.contains(processName)) {
            return true;
        }
        return jarFile != null && !jarFile.isBlank() && cmd.contains(jarFile);
    }
}
