<p align="center">
  <img src="logo.png" alt="Aluer ServerGuard" width="180">
</p>

<h1 align="center">Aluer ServerGuard V5.2.0</h1>

<p align="center">
  <b>AI-Powered Multi-Platform Minecraft Server Protection System</b><br>
  <sub>170+ Security Modules · Full Hack Coverage · Multi-Platform & Multi-Version Support · Agent Architecture · DeepSeek AI</sub>
</p>

<p align="center">
  <a href="https://github.com/peijundev/Aluer/releases/latest"><img src="https://img.shields.io/github/v/release/peijundev/Aluer?style=for-the-badge&color=6366f1" alt="Latest Release"></a>
  <a href="https://github.com/peijundev/Aluer/actions/workflows/ci.yml"><img src="https://github.com/peijundev/Aluer/actions/workflows/ci.yml/badge.svg?branch=main" alt="CI"></a>
  <a href="#"><img src="https://img.shields.io/badge/tests-456%2F456-green?style=for-the-badge" alt="Tests"></a>
  <a href="#"><img src="https://img.shields.io/badge/Java-21-red?style=for-the-badge&logo=openjdk" alt="Java 21"></a>
  <a href="#"><img src="https://img.shields.io/badge/Minecraft-1.12.2%2B-blue?style=for-the-badge" alt="Minecraft 1.12.2+"></a>
  <a href="#"><img src="https://img.shields.io/badge/modules-170%2B-purple?style=for-the-badge" alt="170+ Modules"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL%203.0-orange?style=for-the-badge" alt="GPL 3.0 License"></a>
</p>

<p align="center">
  <a href="#quick-start">Quick Start</a> ·
  <a href="#architecture">Architecture</a> ·
  <a href="#security-modules">Security Modules</a> ·
  <a href="#multi-platform-support">Multi-Platform</a> ·
  <a href="#plugin-system">Plugin System</a> ·
  <a href="#deployment">Deployment</a> ·
  <a href="docs/Docker_K8S部署.md">Docker/K8S</a> ·
  <a href="#cheat-coverage-matrix">Cheat Coverage</a> ·
  <a href="#faq">FAQ</a> ·
  <a href="README.md">中文</a>
</p>

---

## Overview

Aluer ServerGuard is a **next-generation intelligent security system** for Minecraft servers. It employs a revolutionary **Agent Architecture** — a lightweight Bukkit plugin collects real-time data from inside the server process, while an external Spring Boot analysis engine runs independently. The two communicate via **real-time bidirectional WebSocket**.

Through a platform abstraction layer (`com.aluer.platform`), the same Agent plugin runs across the entire Bukkit family — **Spigot / Paper / Purpur / Pufferfish / Folia / Glowstone** — covering **Minecraft 1.12.2 through today**.

### Why Aluer ServerGuard?

| Problem | Aluer Solution |
|---------|---------------|
| Cheat clients rampant (KillAura/Speed/Fly/Xray etc.) | Full hack coverage with millisecond-level detection across 64 anti-cheat modules |
| Traditional anti-cheats rely on rigid rules, high false-positive rate | AI + ML dual-layer: DeepSeek LLM semantic analysis + Isolation Forest/Shannon Entropy/FFT statistical detection |
| DDoS attacks crash servers | Multi-layer DDoS defense (SYN/UDP/ICMP/HTTP/Slowloris/MC protocol/amplification) |
| Server crashes when admin is offline | Kernel autonomy engine + SelfHealing orchestrator: auto-detect → analyze → execute → recover |
| Multiple security tools complex to manage | Single JAR deployment, Web console for all 170+ modules, in-game `/aluer` commands |
| Plugin mode invasive, impacts TPS | Agent architecture: plugin uses only tens of MB RAM, analysis engine runs separately |
| Diverse cores/versions hard to protect uniformly | Platform abstraction + reflection compat layer: one Agent for Spigot→Folia, 1.12.2→1.21.x |

### Core Capabilities

| Capability | Description |
|---|---|
| **Full Anti-Cheat** | 64 anti-cheat modules (Combat 20 / Movement 21 / World 14 / Player 9), merging Grim & NoCheatPlus open-source algorithms |
| **AI Decision Engine** | DeepSeek LLM auto-analyzes alerts → generates defense strategy → auto-executes (ban IP / kick player / enable whitelist / clear lag) |
| **ML Behavioral Analysis** | Isolation Forest anomaly detection + time-series prediction + Shannon Entropy profiling + FFT spectrum movement analysis |
| **Multi-Platform & Multi-Version** | Platform abstraction + reflection layer for Spigot/Paper/Purpur/Folia, Minecraft 1.12.2+ |
| **Plugin System** | Third-party JARs extend engine capabilities without modifying engine source |
| **Agent Auto-Injection** | Engine packages and deploys the Agent plugin into `plugins/` automatically on startup |
| **Multi-Layer DDoS Defense** | SYN/UDP/ICMP/HTTP/Slowloris/MC Status/Login/Rcon/Query coordinated defense |
| **Host Security Scanning** | Crypto-miner / reverse shell / webshell / backdoor script / process injection detection |
| **Server Self-Healing** | TPS < 12 or CPU > 92% or Memory > 95% auto-triggers recovery (backup → clear entities → throttle → whitelist → soft restart) |
| **Dual-Mode** | Agent Plugin mode (recommended, <1ms latency, 50K+/s) + External RCON mode (zero invasion) |
| **170+ Security Modules** | Anti-cheat / network / protocol validation / host security / intrusion detection / forensics / compliance / ops automation |

---

## Quick Start

### Option 1: Download Release JAR (Recommended)

```bash
# Download the latest release
wget https://github.com/peijundev/Aluer/releases/latest/download/serverguard.jar

# Start (External mode, ready out of the box)
java -jar serverguard.jar

# Web Console
open http://localhost:8080
```

### Option 2: Agent Plugin Mode (Production)

```bash
# 1. Start the ServerGuard engine
java -jar serverguard.jar

# 2. Install the Agent plugin (auto-injected when working-dir is configured, or manually)
cp serverguard.jar /opt/minecraft/plugins/AluerServerGuard.jar

# 3. Create the Agent config
mkdir -p /opt/minecraft/plugins/AluerServerGuard
echo 'server-url: ws://localhost:8080/agent' > /opt/minecraft/plugins/AluerServerGuard/config.yml

# 4. Start Paper — Agent auto-connects and begins streaming data
cd /opt/minecraft
java -Xms4G -Xmx4G -jar paper-1.21.1.jar nogui
```

> **Agent Auto-Injection**: with `serverguard.minecraft.auto-inject=true` (default) and `working-dir` configured, the engine deploys `serverguard-agent.jar` into `workingDir/plugins/` on startup and periodically re-injects it (guards against accidental deletion).

### Option 3: Build from Source

```bash
# Requirements: Java 21+
git clone https://github.com/peijundev/Aluer.git
cd Aluer

# Compile (forced clean — incremental builds are prohibited)
./apache-maven-3.9.6/bin/mvn clean compile

# Run the full test suite (456 tests)
./apache-maven-3.9.6/bin/mvn clean test

# Package
./apache-maven-3.9.6/bin/mvn clean package -DskipTests

# JAR artifact at target/serverguard.jar
```

---

## Version History

| Version | Date | Highlights |
|---------|------|-----------|
| **V5.2** | 2026-10 | Multi-platform/multi-version support (Spigot/Paper/Purpur/Folia 1.12.2+) · ServerGuard plugin system · Agent auto-injection with periodic re-injection · Grim (BadPackets/AimModulo360/GroundSpoof) & NoCheatPlus (NoSwing/GodMode/FastHeal/NoWeb/FastLadder) fusion · PacketEvents dependency · Process/file security scanning · Process locator · GitHub Actions CI & auto-release |
| **V5.0** | 2026-05 | Agent WebSocket architecture rework · 151-file security module repackaging · Full hack coverage (Combat/Movement/World/Player 64 modules) · AI/ML behavioral analysis · Network security · Server vulnerability protection |
| **V4.0** | 2026-03 | 29-module security expansion: KillAura · Reach · Speed · Jesus · NoFall · Scaffold · Nuker · AutoClicker · player behavior · server protection · chat security · access control |

---

## Architecture

```
+---------------------------------------------------------------------------+
|                      Aluer ServerGuard V5.2 Architecture                    |
+---------------------------------------------------------------------------+
|                                                                            |
|   +----------------------------+        WebSocket         +--------------------------+
|   |     Minecraft Server       | <========================> |   ServerGuard Engine     |
|   |     (Spigot/Paper/Purpur)  |   ws://host:8080/agent     |   (Spring Boot :8080)    |
|   |     Folia, 1.12.2+         |                            |                          |
|   |                            |  Upstream (Agent→Server)   |   +------------------+   |
|   |   AluerPlugin (Agent)      |  EVENT/METRICS/ALERT       |   | 170+ Security    |   |
|   |   9 Event Listeners        |  HEARTBEAT/HANDSHAKE       |   | Modules          |   |
|   |   WebSocket Client         |  COMMAND_RESULT            |   +------------------+   |
|   |   (java.net.http,          |                            |   | ML/AI Engine     |   |
|   |    reconnect + heartbeat)  |  Downstream (Server→Agent) |   | DeepSeek Client  |   |
|   |   Internal Command         |  COMMAND/CONFIG/SHUTDOWN   |   | Kernel Autonomy  |   |
|   |   Executor (Bukkit API)    |                            |   | Web Console      |   |
|   |                            |                            |   +------------------+   |
|   |  Immediate interception:   |                            |                          |
|   |  event.setCancelled(true)  |                            |                          |
|   +----------------------------+                            +--------------------------+
```

**Message Types (Agent → Server):** `EVENT` · `METRICS` · `ALERT` · `HEARTBEAT` · `HANDSHAKE` · `COMMAND_RESULT`

**Commands (Server → Agent):** `BAN_IP` · `BAN_PLAYER` · `KICK` · `CLEAR_LAG` · `SET_SPAWN_RATE` · `ENABLE_WHITELIST` · `DISABLE_WHITELIST` · `BROADCAST` · `SAVE_ALL` · `EXECUTE`

---

## Security Modules

Modules are organized under `com.aluer` into `anticheat`, `defense`, `network`, `server`, `chat`, and `ml`/`ai` sub-packages.

### Anti-Cheat (`anticheat`, 64 modules)

**Combat (20):** `AntiKillAura` · `AntiReach` · `AntiAutoClicker` · `AntiCriticals` · `AntiAutoCrystal` · `AntiAutoTotem` · `AntiSurround` · `AntiAutoTrap` · `AntiAutoArmor` · `AntiChestSwap` · `AntiAutoLog` · `AntiHitboxes` · `AntiBowAimBot` · `AntiVelocity` · `AntiAnchor` · `AntiPistonAura` · `AntiAim` · `AntiNoSwing` · `AntiGodMode` · `AntiFastHeal`

**Movement (21):** `AntiFly` · `AntiSpeed` · `AntiJesus` · `AntiNoFall` · `AntiTimer` · `AntiPhase` · `AntiBlink` · `AntiScaffold` · `AntiSpider` · `AntiStep` · `AntiNoSlow` · `AntiPacketFly` · `AntiAirJump` · `AntiLongJump` · `AntiAntiHunger` · `AntiFastFall` · `AntiVClip` · `AntiElytraFly` · `AntiFastLadder` · `AntiNoWeb` · `AntiGroundSpoof`

**World (14):** `AntiNuker` · `AntiAutoMine` · `AntiSpeedMine` · `AntiFastBreak` · `AntiFastUse` · `AntiNoInteract` · `AntiVeinMiner` · `AntiAutoTool` · `AntiAutoFish` · `AntiChestSteal` · `AntiXray` · `AntiBaritone` · `AntiGrief` · `AntiDupe`

**Player (9):** `AntiFakePlayer` · `AntiInventoryManipulation` · `AntiNameSpoof` · `AntiSkinSpoof` · `AntiAltAccount` · `AntiOfflineModeSpoof` · `AntiVPNProxy` · `AntiBotDetection` · `AntiBadPackets`

### Server Vulnerability Protection (`server`, 6 modules)

`AntiSignExploit` (sign NBT) · `AntiBookBan` (book ban) · `AntiResourcePackExploit` · `AntiTabCompleteCrash` · `CrashExploitProtection` · `LagMachineDetection`

### Defense-in-Depth (`defense`, 58 modules)

Malware detection · Stash finder · Backdoor plugin scanner · Backup integrity/security · Bot fingerprint · Chunk-load rate limiter · Command execution guard · Compliance · Config tamper detection · Handshake validator · Container security · Crash exploit signature DB · CSP enforcement · Database firewall · Data loss prevention · DDoS coordinator · Encryption · EDR · Entity count enforcer · Exploit signature · File integrity · File security scanner (webshell/backdoor scripts) · Forensics · GeoBlock · Honeypot · Host enforcement · Host intrusion countermeasure · Incident response · IDS · IPS · JWT auth · Log analysis/correlation · Memory protection · NBT exploit prevention · OP privilege monitor · Session validation/management · Plugin verification · Process injection detection · Process security (crypto-miner/reverse shell) · Protocol state validator · Redstone update limiter · Secure file deletion · Security automation · Security baseline hardening · Security orchestration · SIEM · SSRF protection · Threat hunting · Threat intelligence · Token bucket rate limiter · Traffic shaping · WAF · XXE protection · Zero Trust

### Network Security (`network`, 28 modules)

API rate limit · ARP spoof detection · Brute-force protection · Cloudflare integration · Connection throttle · DDoS protection · Distributed attack mitigation · DNS security · DNS tunnel detection · Firewall · Flow analyzer · GeoIP · IP reputation · Load balancer · MC protocol security · Network monitor · Network segmentation · Network sniffer · Network threat fusion · Packet flood protection · Packet inspection · Port scan detection · Protocol analysis · Rate limit · Reverse shell detection · SSL monitor · SSL/TLS certificate · Traffic analysis

### Chat & Social Security (`chat`, 6 modules)

`ChatFloodProtection` · `AntiAdvertisement` · `AntiPhishingLink` · `AntiCommandAbuse` · `PlayerPrivacy` · `ChatFilter`

### ML & AI Engine (12 modules)

`BehavioralProfilingEngine` (5-dim feature vector · Shannon entropy · Z-score baseline) · `ThreatScoreAggregator` (exponential decay weighting) · `MovementPatternAnalyzer` (100-tick window · FFT) · `CombatPatternRecognizer` (CPS coefficient of variation) · `AnomalyDetector` (Isolation Forest) · `TimeSeriesPredictor` (TPS/CPU/Memory prediction) · `DeepSeekClient` · `AIAutonomousService` · `AIStrategyEngine` · `AluerSovereignEngine` · `AdaptiveThreshold` · `AttackDetector`

---

## Open-Source Anti-Cheat Fusion

The project is **GPL-3.0** licensed (see `LICENSE`), compatible with GPL open-source anti-cheats such as [GrimAC](https://github.com/GrimAnticheat/Grim) and [NoCheatPlus](https://github.com/NoCheatPlus/NoCheatPlus), allowing their detection algorithms to be merged directly.

| Source | Check | Aluer Service |
|--------|-------|---------------|
| GrimAC | BadPackets A/C/D/E/F (held item slot / leave bed / pitch out-of-range / no position update / repeated sprint) | `AntiBadPacketsService` |
| GrimAC | AimModulo360 (raw yaw wrap-around) | `AntiAimService.checkModulo360` |
| GrimAC | GroundSpoof (onGround=true while falling) | `AntiGroundSpoofService` |
| NoCheatPlus | fight/NoSwing | `AntiNoSwingService` |
| NoCheatPlus | fight/GodMode | `AntiGodModeService` |
| NoCheatPlus | fight/FastHeal | `AntiFastHealService` |
| NoCheatPlus | FastLadder | `AntiFastLadderService` |
| NoCheatPlus | NoWeb | `AntiNoWebService` |

**Movement simulation engine port (in progress):** Phase 1 is complete — PacketEvents 2.7.0 dependency + the `utils/math` base library (8 files under the original `ac.grim.grimac` package). The full port (~80K lines) proceeds layer by layer along the dependency graph. See `docs/开源反作弊融合.md`.

---

## Multi-Platform Support

Via the `com.aluer.platform` abstraction layer, the same Agent plugin runs across the entire Bukkit family, covering **Minecraft 1.12.2 through today**.

| Family | Platforms | Notes |
|--------|-----------|-------|
| Bukkit | Vanilla (CraftBukkit) / Spigot / Paper / Purpur / Pufferfish / Folia / Glowstone | Runs directly (`plugin.yml`, `api-version: '1.12'` + `folia-supported: true`) |
| Proxy | BungeeCord / Waterfall / Velocity | Requires a separate adapter (engine has type recognition) |
| Mod loader | Forge / NeoForge / Fabric / Sponge | Requires a separate adapter (engine has type recognition) |

**Key types:** `MinecraftVersion` (version parsing/comparison) · `PlatformType` (enum) · `PlatformDetector` (runtime reflection) · `BukkitCompat` (cross-version reflection compat layer for `getPing` / `kick` / `broadcast` / `getTps`).

**Process locator:** `MinecraftProcessDetector` uses `ProcessHandle` to detect the Minecraft server process by known core names (paper/spigot/purpur/folia/forge/fabric …); `ServerDirectoryResolver` auto-discovers the real working directory and log path.

---

## Plugin System

Third-party developers can extend ServerGuard with independent JARs — no engine source modification required.

Core types (`com.aluer.pluginsystem`): `ServerGuardPlugin` (lifecycle interface) · `Plugin` (metadata annotation) · `PluginDescriptor` · `PluginManager` · `PluginContext` · `PluginClassLoader` (isolated classloader) · `PluginSystemService` (Spring integration).

```java
@Plugin(name = "my-guard", version = "1.0.0", author = "example",
        description = "Example security module", depends = {})
public class MyGuardPlugin implements ServerGuardPlugin {
    private PluginContext context;

    @Override
    public void onLoad(PluginContext context) {
        this.context = context;
        context.registerCommand("myguard-scan", "Scan example", args -> "scan complete");
    }
    // onEnable / onDisable ...
}
```

```yaml
serverguard:
  plugin:
    enabled: true
    directory: ${SERVERGUARD_PLUGIN_DIR:serverguard/plugins}
```

---

## Cheat Coverage Matrix

Aluer ServerGuard covers all major Minecraft cheat client hack categories.

| Category | Common Hacks | Aluer Counter-Modules |
|----------|-------------|----------------------|
| **Combat** | KillAura · AimAssist · AutoCrystal · Criticals · Surround · AutoTrap · AutoArmor · AutoTotem · BowAimbot · Hitboxes · Velocity · Anchor · PistonAura | 20 |
| **Movement** | Fly · Speed · Jesus · NoFall · Timer · Phase · Blink · Scaffold · Spider · Step · NoSlow · PacketFly · AirJump · LongJump · ElytraFly · VClip | 21 |
| **World** | Nuker · SpeedMine · VeinMiner · Xray · Grief · AutoMine · AutoFish · FastBreak · Baritone | 14 |
| **Player** | FakePlayer · InventoryTweaks · NameSpoof · AltAccount · BadPackets · Bot | 9 |
| **Total** | **110+ hacks** | **64** |

---

## Deployment

### Mode Comparison

| Feature | Agent Plugin | External |
|---------|-------------|----------|
| Communication | WebSocket bidirectional real-time | RCON + log parsing |
| Latency | < 1ms (local) | 50-200ms (RCON) |
| Event Throughput | 50,000+ events/s | Log-dependent |
| Data Precision | Bukkit Event level (per tick) | Log text level (seconds delay) |
| MC Invasiveness | Paper plugin (tens of MB) | Zero (RCON port only) |
| Instant Interception | Yes (event.setCancelled) | No (post-alert only) |
| Module Coverage | Full 170+ modules | ~40 external monitor modules |
| Recommended | **Production** | Evaluation / Testing |

### One-Click Deployment Scripts

- **`install.sh`**: one-click installer — creates a systemd service and starts the engine (`sudo bash install.sh`).
- **`start.sh`**: quick-start script — runs the engine in the background via `nohup`.
- **`systemd/`**: prebuilt `serverguard.service` / `minecraft.service` / `sudoers.serverguard` unit files.

### Docker / Kubernetes

Containerized deployment is included (`Dockerfile` · `.dockerignore` · `docker-compose.yml` · `k8s/`):

```bash
docker build -t aluer/serverguard:5.2.0 .
docker compose up -d --build          # Engine + Paper server (External mode)
kubectl create namespace aluer && kubectl apply -f k8s/
```

> **Containerization caveats**: host-level modules (process scanning, network sniffing, ARP detection) are restricted inside containers — they only see the container itself. External mode (RCON + network-layer defense) and Agent mode (WebSocket) containerize fully. See [`docs/Docker_K8S部署.md`](docs/Docker_K8S部署.md).

---

## In-Game Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/aluer status` | `aluer.status` | Protection status, TPS, online players, packet rate, Spring context state |
| `/aluer scan <player>` | `aluer.admin` | Deep scan: position/IP/ping/fly/sprint/sneak/attack targets/attack-angle consistency |
| `/aluer info` | `aluer.status` | Version, tech stack, module count, run mode |
| `/aluerplayers` | `aluer.status` | Online player list with risk flags |
| `/aluerblock player <name>` | `aluer.admin` | Ban player + broadcast |
| `/aluerblock ip <addr>` | `aluer.admin` | Ban IP + kick all players from that IP |
| `/aluerunblock player/ip <target>` | `aluer.admin` | Unban player/IP |
| `/aluerwhitelist on/off/status` | `aluer.admin` | Emergency whitelist management |

---

## Build & Test

### CI/CD Automation

GitHub Actions workflows (`.github/workflows/`):

- **`ci.yml`**: on push/PR to `main` — build frontend → `mvn clean test` → upload surefire reports.
- **`release.yml`**: on `v*` tag push — build frontend → `mvn clean package` (with full tests) → create GitHub Release with `serverguard.jar`.

### Commands

```bash
./apache-maven-3.9.6/bin/mvn clean compile          # Compile (clean, full)
./apache-maven-3.9.6/bin/mvn clean test             # 456 tests (JUnit 5, no Spring context)
./apache-maven-3.9.6/bin/mvn clean package -DskipTests  # Build serverguard.jar
```

### Test Statistics

| Metric | Value |
|--------|-------|
| Source Files | 263 |
| Source Lines | ~78,573 |
| Test Files | 36 |
| Test Cases | 456 |
| Test Result | **456/456 PASS, 0 failures, 0 errors, 0 skipped** |

Tests use the **dual-constructor pattern** (no-arg constructor for tests + `@Autowired` constructor for Spring) and **internal static result classes** with static factory methods (`clean()` / `suspicious()` / `blocked()` / `flagged()`), matching production thresholds without mocks or simplifications.

---

## Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Core language |
| Spring Boot | 3.2.0 | Framework (Web · WebSocket · Mail · Shell · Actuator · Configuration Processor) |
| Maven | 3.9.6 (bundled) | Build |
| PaperMC API | 1.21.1-R0.1-SNAPSHOT (provided) | Minecraft integration (compile target) |
| PacketEvents | 2.7.0 (provided) | Packet-level anti-cheat (runtime by PacketEvents plugin) |
| Spring WebSocket | 3.2.0 | Agent ↔ ServerGuard communication |
| Apache Commons Math3 | 3.6.1 | Statistics · ML (DescriptiveStatistics/TTest/FFT) |
| Gson | 2.10.1 | JSON serialization |
| SnakeYAML | 2.2 | YAML config parsing |
| JSch | 0.2.20 | SSH remote gateway |
| React | 19.1.0 | Web console frontend (Nebula Console) |
| Vite | 6.3.5 | Frontend build |
| DeepSeek API | /v1/chat/completions | AI decision engine |

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVERGUARD_MODE` | Run mode: `plugin` or `external` | `external` |
| `SERVERGUARD_WORKING_DIR` | Server working directory (auto-discovered if empty) | (empty) |
| `SERVERGUARD_LOG_PATH` | Log file path (auto-derived if empty) | (empty) |
| `SERVERGUARD_PLUGIN_DIR` | Plugin directory | `serverguard/plugins` |
| `DEEPSEEK_API_KEY` | DeepSeek AI API key (empty = AI disabled) | (empty) |
| `DEEPSEEK_BASE_URL` | DeepSeek API base URL | `https://api.deepseek.com` |
| `DEEPSEEK_MODEL` | DeepSeek model name | `deepseek-chat` |
| `RCON_PASSWORD` | Minecraft RCON password (External mode) | (empty) |
| `SERVER_PORT` | Web server port | `8080` |
| `ALUER_ALERT_SMTP_USERNAME` | Alert email SMTP username | (empty) |
| `ALUER_ALERT_SMTP_PASSWORD` | Alert email SMTP password | (empty) |
| `ALUER_ALERT_EMAIL_PRIMARY` | Primary alert recipient | (empty) |
| `ALUER_ALERT_EMAIL_SECONDARY` | Secondary alert recipient | (empty) |
| `ALUER_CLOUDFLARE_ZONE_ID` | Cloudflare Zone ID (edge defense) | (empty) |
| `ALUER_CLOUDFLARE_API_KEY` | Cloudflare API Key | (empty) |
| `ALUER_CLOUDFLARE_EMAIL` | Cloudflare account email | (empty) |
| `ALUER_WEBHOOK_DISCORD` | Discord webhook URL | (empty) |
| `ALUER_WEBHOOK_SLACK` | Slack webhook URL | (empty) |

---

## FAQ

<details>
<summary><b>Agent vs External mode?</b></summary>

Agent Plugin mode is strongly recommended for production — sub-millisecond latency, 50K+ events/s, per-tick data precision, instant event cancellation, full 170+ module coverage. External mode suits evaluation or plugin-free setups.
</details>

<details>
<summary><b>Is DeepSeek AI required?</b></summary>

No. Without `DEEPSEEK_API_KEY`, AI auto-disables and the system operates as rule engine + ML mode with full anti-cheat capability. AI adds intelligent root-cause analysis, health reports, and autonomous defense generation.
</details>

<details>
<summary><b>Java version required?</b></summary>

Java 21+. Spring Boot 3.2.0 and Paper 1.21.1 API require Java 21 runtime.
</details>

<details>
<summary><b>Supported Minecraft versions?</b></summary>

Full Agent support across the Bukkit family — Vanilla (CraftBukkit) / Spigot / Paper / Purpur / Pufferfish / Folia / Glowstone, Minecraft **1.12.2+**. Compiled against Paper 1.21.1, cross-version via a reflection compatibility layer. Proxy (BungeeCord/Velocity) and mod-loader (Forge/Fabric) platforms require separate adapters.
</details>

<details>
<summary><b>Will it false-ban legit players?</b></summary>

Layered design minimizes false bans: immediate interception only at physically impossible values, server-side multi-dimensional cross-validation, AI auto-execute requires ≥88% confidence, and SelfHealing/HostEnforcement default to dry-run mode.
</details>

<details>
<summary><b>How do I add a new anti-cheat module?</b></summary>

See `docs/DEVELOPER.md`: create a Service under `anticheat/` (dual-constructor + internal static result class pattern), add an alert type, add a `boolean` switch in `ServerGuardConfig.SuperEvolutionConfig`, add a config item, write a JUnit test, run `mvn clean test`, commit.
</details>

---

## Contributors

| Contributor | GitHub | Role |
|------------|--------|------|
| Peijun Zhao | [@peijundev](https://github.com/peijundev) | Architecture Design & Core Development |

---

## License

**GNU General Public License v3.0 (GPL-3.0)** — see [LICENSE](LICENSE) for full text. The project merges detection algorithms from GPL open-source anti-cheats (GrimAC, NoCheatPlus), hence the GPL-3.0 license for compatibility.

<p align="center">
  <br>
  <b>Aluer ServerGuard V5.2.0</b><br>
  <sub>Protecting Every Minecraft Server</sub>
</p>
