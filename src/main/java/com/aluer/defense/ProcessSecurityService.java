package com.aluer.defense;

import com.aluer.config.ServerGuardConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 进程安全扫描服务 — 检测宿主机上的异常进程。
 *
 * <p>与 {@link com.aluer.monitor.MinecraftProcessDetector}（定位 Minecraft 服务器进程）不同，
 * 本服务面向宿主机整体，通过 {@link ProcessHandle} 遍历所有进程，识别三类威胁特征：</p>
 * <ol>
 *   <li><b>挖矿木马</b>：命令行含 xmrig / minerd / cpuminer 等挖矿程序特征。</li>
 *   <li><b>反弹 shell</b>：命令行含 /dev/tcp/ / bash -i / nc -e 等反弹 shell 特征。</li>
 *   <li><b>可疑路径</b>：可执行文件位于 /tmp / /dev/shm 等常被利用的临时目录。</li>
 * </ol>
 *
 * <p>核心判定 {@link #detectSuspicion(String)} 为纯逻辑方法，便于单元测试。
 * 配置开关：serverguard.security.super-evolution.process-security</p>
 */
@Service
public class ProcessSecurityService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessSecurityService.class);

    private final ServerGuardConfig config;

    private final AtomicLong totalScans = new AtomicLong(0);
    private final AtomicLong suspiciousFound = new AtomicLong(0);

    /** 挖矿程序特征关键词 */
    private static final List<String> MINING_KEYWORDS = List.of(
            "xmrig", "minerd", "cpuminer", "cryptonight", "cgminer", "bfgminer",
            "ethminer", "phoenixminer", "lolminer", "nanominer", "t-rex", "minergate");

    /** 反弹 shell 特征 */
    private static final List<String> REVERSE_SHELL_PATTERNS = List.of(
            "/dev/tcp/", "bash -i", "nc -e", "sh -i", "/bin/sh -i");

    /** 常被利用的临时目录 */
    private static final List<String> SUSPICIOUS_PATHS = List.of(
            "/tmp/", "/dev/shm/", "/var/tmp/");

    /** 无参构造（测试兼容） */
    public ProcessSecurityService() {
        this(new ServerGuardConfig());
    }

    public ProcessSecurityService(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 扫描宿主机所有进程，返回可疑进程列表。
     */
    public ProcessScanResult scanProcesses() {
        if (!config.getSecurity().getSuperEvolution().isProcessSecurity()) {
            return ProcessScanResult.clean();
        }
        totalScans.incrementAndGet();

        List<SuspiciousProcess> suspicious = new ArrayList<>();
        try {
            ProcessHandle.allProcesses().forEach(ph -> {
                // 排除自身
                if (ph.pid() == ProcessHandle.current().pid()) {
                    return;
                }
                String commandLine = ph.info().commandLine().orElse("");
                String reason = detectSuspicion(commandLine);
                if (reason != null) {
                    suspicious.add(new SuspiciousProcess(ph.pid(), commandLine, reason));
                }
            });
        } catch (Exception e) {
            // 无权限枚举进程时安全返回
            logger.warn("进程扫描失败: {}", e.getMessage());
        }

        suspiciousFound.addAndGet(suspicious.size());
        return suspicious.isEmpty() ? ProcessScanResult.clean() : ProcessScanResult.found(suspicious);
    }

    /**
     * 判断命令行是否可疑（纯逻辑，可测试）。
     *
     * @param commandLine 进程完整命令行
     * @return 可疑原因；正常返回 null
     */
    static String detectSuspicion(String commandLine) {
        if (commandLine == null || commandLine.isBlank()) {
            return null;
        }
        String lower = commandLine.toLowerCase();
        for (String keyword : MINING_KEYWORDS) {
            if (lower.contains(keyword)) {
                return "MINING_PROCESS: " + keyword;
            }
        }
        for (String pattern : REVERSE_SHELL_PATTERNS) {
            if (lower.contains(pattern)) {
                return "REVERSE_SHELL: " + pattern;
            }
        }
        for (String path : SUSPICIOUS_PATHS) {
            if (lower.contains(path)) {
                return "SUSPICIOUS_PATH: " + path;
            }
        }
        return null;
    }

    /** 获取模块运行状态 */
    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("enabled", config.getSecurity().getSuperEvolution().isProcessSecurity());
        s.put("totalScans", totalScans.get());
        s.put("suspiciousFound", suspiciousFound.get());
        return s;
    }

    // ─── 结果类 ────────────────────────────────────────

    /** 可疑进程信息 */
    public static class SuspiciousProcess {
        private final long pid;
        private final String commandLine;
        private final String reason;

        SuspiciousProcess(long pid, String commandLine, String reason) {
            this.pid = pid;
            this.commandLine = commandLine;
            this.reason = reason;
        }

        public long getPid() { return pid; }
        public String getCommandLine() { return commandLine; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return "SuspiciousProcess{pid=" + pid + ", reason=" + reason + "}";
        }
    }

    /** 进程扫描结果 */
    public static class ProcessScanResult {
        private final boolean found;
        private final List<SuspiciousProcess> suspicious;

        private ProcessScanResult(boolean found, List<SuspiciousProcess> suspicious) {
            this.found = found;
            this.suspicious = suspicious;
        }

        public static ProcessScanResult clean() {
            return new ProcessScanResult(false, List.of());
        }

        public static ProcessScanResult found(List<SuspiciousProcess> suspicious) {
            return new ProcessScanResult(true, suspicious);
        }

        public boolean hasSuspicious() { return found; }
        public List<SuspiciousProcess> getSuspicious() { return suspicious; }
    }
}
