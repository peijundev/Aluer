package com.aluer.monitor;

import com.aluer.config.ServerGuardConfig;
import com.aluer.model.AlertEvent;
import com.aluer.model.AlertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ProcessMonitor {
    private static final Logger logger = LoggerFactory.getLogger(ProcessMonitor.class);
    
    private final ServerGuardConfig config;
    private boolean processRunning = false;

    public ProcessMonitor(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 判断 Minecraft 服务器进程是否正在运行。
     *
     * 采用 JDK 原生 ProcessHandle API 遍历系统进程，识别命令行中包含
     * processName / jarFile 关键词的服务器进程，并排除 Aluer 自身 JVM。
     * 相比 pgrep -f：不依赖外部命令、不做正则模糊匹配，能可靠识别
     * 带 build 号（如 paper-1.21.11-113.jar）的进程。
     */
    public boolean isProcessRunning() {
        try {
            processRunning = MinecraftProcessDetector.isRunning(config);
        } catch (Exception e) {
            logger.warn("Failed to check process status: {}", e.getMessage());
            processRunning = false;
        }
        return processRunning;
    }

    public boolean restartProcess() {
        String serviceName = config.getMinecraft().getServiceName();
        try {
            logger.info("Attempting to restart Minecraft service: {}", serviceName);
            ProcessBuilder pb = new ProcessBuilder("sudo", "systemctl", "restart", serviceName);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor(30, TimeUnit.SECONDS);
            
            if (p.exitValue() == 0) {
                logger.info("Minecraft service restarted successfully");
                processRunning = true;
                return true;
            } else {
                logger.error("Failed to restart Minecraft service, exit code: {}", p.exitValue());
            }
        } catch (Exception e) {
            logger.error("Failed to restart process: {}", e.getMessage());
        }
        return false;
    }

    public boolean stopProcess() {
        String serviceName = config.getMinecraft().getServiceName();
        try {
            ProcessBuilder pb = new ProcessBuilder("sudo", "systemctl", "stop", serviceName);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor(30, TimeUnit.SECONDS);
            processRunning = false;
            return p.exitValue() == 0;
        } catch (Exception e) {
            logger.error("Failed to stop process: {}", e.getMessage());
            return false;
        }
    }

    public AlertEvent checkAndRestart() {
        if (!isProcessRunning()) {
            logger.warn("Minecraft process not found, attempting restart...");
            if (restartProcess()) {
                return new AlertEvent(AlertType.PROCESS_DEAD, "Process was dead, auto-restarted successfully");
            } else {
                return new AlertEvent(AlertType.PROCESS_DEAD, "Process was dead, restart FAILED");
            }
        }
        return null;
    }
}
