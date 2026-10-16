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
 * 文件安全扫描服务 — 检测服务器目录下的 webshell / 后门脚本 / 可疑文件。
 *
 * <p>Minecraft 服务器目录本不应存在 PHP / JSP / ASP 等动态脚本文件，一旦出现这些
 * 文件往往是攻击者植入的 webshell。本服务通过文件名扩展名与文件内容特征识别此类威胁：</p>
 *
 * <ol>
 *   <li><b>webshell 扩展名</b>：.php / .jsp / .asp / .aspx 等。</li>
 *   <li><b>webshell 函数特征</b>：eval / base64_decode / gzinflate / shell_exec / passthru 等。</li>
 *   <li><b>代码执行特征</b>：Runtime.exec / ProcessBuilder / os.system / subprocess。</li>
 *   <li><b>反弹 shell 特征</b>：/dev/tcp/ / nc -e / bash -i。</li>
 * </ol>
 *
 * <p>核心判定为纯逻辑方法，便于单元测试。
 * 配置开关：serverguard.security.super-evolution.file-security</p>
 */
@Service
public class FileSecurityScannerService {

    private static final Logger logger = LoggerFactory.getLogger(FileSecurityScannerService.class);

    private final ServerGuardConfig config;

    private final AtomicLong totalScans = new AtomicLong(0);
    private final AtomicLong suspiciousFound = new AtomicLong(0);

    /** webshell 常见扩展名 */
    private static final List<String> WEBSHELL_EXTENSIONS = List.of(
            ".php", ".jsp", ".asp", ".aspx");

    /** webshell / 后门代码执行函数特征 */
    private static final List<String> CODE_EXEC_FUNCTIONS = List.of(
            "base64_decode", "gzinflate", "shell_exec", "passthru", "eval(",
            "getruntime().exec", "processbuilder", "os.system", "subprocess", "cmd.exe");

    /** 反弹 shell 特征 */
    private static final List<String> REVERSE_SHELL_PATTERNS = List.of(
            "/dev/tcp/", "nc -e", "bash -i");

    /** 无参构造（测试兼容） */
    public FileSecurityScannerService() {
        this(new ServerGuardConfig());
    }

    public FileSecurityScannerService(ServerGuardConfig config) {
        this.config = config;
    }

    /**
     * 检测文件名是否可疑。
     *
     * @param fileName 文件名（含扩展名）
     * @return 可疑原因；正常返回 null
     */
    static String detectSuspiciousFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String lower = fileName.toLowerCase();
        for (String ext : WEBSHELL_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return "WEBSHELL_EXTENSION: " + ext;
            }
        }
        return null;
    }

    /**
     * 检测文件内容是否可疑（webshell / 后门 / 反弹 shell）。
     *
     * @param content 文件内容
     * @return 可疑原因；正常返回 null
     */
    static String detectSuspiciousContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String lower = content.toLowerCase();
        for (String fn : CODE_EXEC_FUNCTIONS) {
            if (lower.contains(fn)) {
                return "CODE_EXECUTION: " + fn;
            }
        }
        for (String pattern : REVERSE_SHELL_PATTERNS) {
            if (lower.contains(pattern)) {
                return "REVERSE_SHELL: " + pattern;
            }
        }
        return null;
    }

    /**
     * 综合检测一个文件（名称 + 内容）。
     *
     * @param fileName 文件名
     * @param content  文件内容（可能为 null，仅做名称检测）
     * @return 可疑原因；正常返回 null
     */
    public String scanFile(String fileName, String content) {
        if (!config.getSecurity().getSuperEvolution().isFileSecurity()) {
            return null;
        }
        totalScans.incrementAndGet();

        String reason = detectSuspiciousFileName(fileName);
        if (reason == null && content != null) {
            reason = detectSuspiciousContent(content);
        }
        if (reason != null) {
            suspiciousFound.incrementAndGet();
        }
        return reason;
    }

    /** 获取模块运行状态 */
    public Map<String, Object> getStatus() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("enabled", config.getSecurity().getSuperEvolution().isFileSecurity());
        s.put("totalScans", totalScans.get());
        s.put("suspiciousFound", suspiciousFound.get());
        return s;
    }
}
