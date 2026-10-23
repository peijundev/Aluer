<p align="center">
  <img src="logo.png" alt="Aluer ServerGuard" width="180">
</p>

<h1 align="center">Aluer ServerGuard V5.2.0</h1>

<p align="center">
  <b>AI 驱动的 Minecraft 多平台服务器全方位安全防护系统</b><br>
  <sub>170+ 安全模块 · 全类型外挂对抗覆盖 · 多平台多版本支持 · Agent 实时架构 · DeepSeek AI 决策</sub>
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
  <a href="#快速开始">快速开始</a> ·
  <a href="#系统架构">系统架构</a> ·
  <a href="#安全模块完整清单">安全模块</a> ·
  <a href="#多平台多版本支持">多平台支持</a> ·
  <a href="#插件系统">插件系统</a> ·
  <a href="#部署指南">部署指南</a> ·
  <a href="docs/Docker_K8S部署.md">Docker/K8S</a> ·
  <a href="#配置参考">配置参考</a> ·
  <a href="#外挂对抗覆盖矩阵">外挂覆盖</a> ·
  <a href="#常见问题">FAQ</a> ·
  <a href="README_EN.md">English</a>
</p>

---

## 项目概述

Aluer ServerGuard 是一款**专为 Minecraft 服务器打造的新一代智能安全防护系统**。系统采用革命性的 **Agent 架构**——通过轻量级 Bukkit 插件作为数据采集前端植入服务器进程内部，配合外部 Spring Boot 分析引擎独立运行，通过 **WebSocket 实时双向通信**实现对服务器的全方位实时防护。

通过平台抽象层（`com.aluer.platform`），同一个 Agent 插件可运行于 **Spigot / Paper / Purpur / Pufferfish / Folia** 等全部 Bukkit 系平台，覆盖 **Minecraft 1.12.2 至今**的全部版本。

### 为什么选择 Aluer ServerGuard？

| 痛点 | Aluer 解决方案 |
|------|---------------|
| 外挂客户端泛滥（KillAura/Speed/Fly/Xray 等） | 全类型外挂对抗覆盖，实时检测毫秒级响应 |
| 传统反作弊依赖规则引擎，误报率高 | AI + ML 双层分析：DeepSeek 大模型语义理解 + 隔离森林/香农熵/FFT 统计检测 |
| DDoS 攻击导致服务器瘫痪 | 多层 DDoS 协同防御（SYN/UDP/ICMP/HTTP/Slowloris/MC协议/放大攻击） |
| 管理员不在线时服务器崩溃无人处理 | Kernel 自治引擎 + SelfHealing 自愈系统，自动检测→分析→执行→恢复 |
| 多个安全工具管理复杂 | 单一 JAR 部署，Web 控制台统一管理 170+ 模块，游戏内 /aluer 命令 |
| 插件模式侵入性高，影响服务器性能 | Agent 架构分离：插件仅几十 MB 内存占用，分析引擎独立运行不影响 TPS |
| 服务器核心/版本各异，难以统一防护 | 平台抽象层 + 反射兼容，一套 Agent 适配 Spigot→Folia、1.12.2→1.21.x |

### 核心能力一览

| 能力 | 说明 |
|------|------|
| **全类型反作弊** | 64 个反作弊模块（战斗 20 / 移动 21 / 世界 14 / 玩家 9），融合 Grim / NoCheatPlus 开源算法 |
| **AI 决策引擎** | DeepSeek 大模型自动分析安全告警 → 生成防御策略 → 自动执行（ban IP / kick player / enable whitelist / clear lag） |
| **ML 行为分析** | 隔离森林（Isolation Forest）异常检测 + 时间序列预测 + 香农熵（Shannon Entropy）玩家行为画像 + FFT 频谱移动分析 |
| **多平台多版本** | 平台抽象层 + 反射兼容层，Spigot/Paper/Purpur/Folia，Minecraft 1.12.2+ |
| **插件系统** | 第三方独立 JAR 扩展引擎能力，无需修改引擎源码 |
| **Agent 自动注入** | 引擎启动时自动将 Agent 插件打包并部署到服务端 plugins/ 目录 |
| **DDoS 多层防御** | SYN Flood / UDP Flood / ICMP Flood / HTTP Flood / Slowloris / Minecraft Status/Login/Rcon/Query 协同防御 |
| **主机安全扫描** | 挖矿木马 / 反弹 Shell / webshell / 后门脚本 / 进程注入检测 |
| **服务器自愈** | TPS < 12 或 CPU > 92% 或 Memory > 95% 自动触发恢复流程（备份 → 清理实体 → 限制连接 → 白名单 → 软重启） |
| **双模式部署** | Agent Plugin 模式（推荐，< 1ms 延迟，50K+/s 吞吐）+ External RCON 模式（零侵入） |
| **170+ 安全模块** | 反作弊 / 网络安全 / 协议验证 / 主机安全 / 入侵检测 / 取证分析 / 合规审计 / 运维自动化 |

---

## 快速开始

### 方式一：下载 Release JAR（推荐）

```bash
# 下载最新版本
wget https://github.com/peijundev/Aluer/releases/latest/download/serverguard.jar

# 启动（External 模式，开箱即用）
java -jar serverguard.jar

# 浏览器访问 Web 控制台
open http://localhost:8080
```

### 方式二：Agent Plugin 模式（生产环境推荐）

```bash
# 步骤 1：启动 ServerGuard 引擎
java -jar serverguard.jar

# 引擎启动时自动注入 Agent 插件到服务端 plugins/ 目录（serverguard.mode=plugin 时）
# 手动部署也可：将 JAR 复制到 Paper 服务器 plugins/ 目录
cp serverguard.jar /opt/minecraft/plugins/AluerServerGuard.jar

# 步骤 2：创建 Agent 配置（指定 ServerGuard 地址）
mkdir -p /opt/minecraft/plugins/AluerServerGuard
cat > /opt/minecraft/plugins/AluerServerGuard/config.yml << EOF
server-url: ws://localhost:8080/agent
EOF

# 步骤 3：启动 Paper 服务器——Agent 自动连接并开始推送数据
cd /opt/minecraft
java -Xms4G -Xmx4G -jar paper-1.21.1.jar nogui
```

> **Agent 自动注入**：引擎在 `serverguard.minecraft.auto-inject=true`（默认开启）且 `working-dir` 已配置时，会在启动时自动把 `serverguard-agent.jar` 部署到 `workingDir/plugins/` 目录，无需手工复制。

### 方式三：从源码构建

```bash
# 环境要求：Java 21+
git clone https://github.com/peijundev/Aluer.git
cd Aluer

# 编译（强制 clean，禁止增量编译）
./apache-maven-3.9.6/bin/mvn clean compile

# 运行全量测试（456 项）
./apache-maven-3.9.6/bin/mvn clean test

# 打包
./apache-maven-3.9.6/bin/mvn clean package -DskipTests

# JAR 产物在 target/serverguard.jar
```

---

## 版本历史

| 版本 | 日期 | 核心更新 |
|------|------|----------|
| **V5.2** | 2026-10 | 多平台/多版本支持（Spigot/Paper/Purpur/Folia 1.12.2+）· ServerGuard 插件系统 · Agent 自动注入与周期补注入 · 融合 Grim（BadPackets/AimModulo360/GroundSpoof）与 NoCheatPlus（NoSwing/GodMode/FastHeal/NoWeb/FastLadder）· PacketEvents 依赖 · 进程/文件安全扫描 · 进程定位器 · GitHub Actions CI 与自动发布 |
| **V5.0** | 2026-05 | Agent WebSocket 架构重构 · 151 文件安全模块分包 · 全类型外挂对抗（战斗/移动/世界/玩家 64 模块）· AI/ML 行为分析 · 网络安全 · 服务器漏洞防护 |
| **V4.0** | 2026-03 | 29 模块安全大扩展：KillAura · Reach · Speed · Jesus · NoFall · Scaffold · Nuker · AutoClicker · 玩家行为 · 服务器保护 · 聊天安全 · 访问控制 |

---

## 系统架构

```
+---------------------------------------------------------------------------+
|                        Aluer ServerGuard V5.2 系统架构                       |
+---------------------------------------------------------------------------+
|                                                                            |
|   +----------------------------+          WebSocket           +--------------------------+
|   |     Minecraft Server       | <==========================> |   ServerGuard Engine     |
|   |     (Spigot/Paper/Purpur)  |   ws://host:8080/agent       |   (Spring Boot :8080)    |
|   |     Folia, 1.12.2+         |                              |                          |
|   |                            |                              |                          |
|   |   +--------------------+   |   上行 (Agent → Server)       |   +------------------+   |
|   |   | AluerPlugin (Agent) |   |   EVENT / METRICS / ALERT   |   | 170+ Security    |   |
|   |   | extends JavaPlugin  |   |   HEARTBEAT / HANDSHAKE     |   | Modules          |   |
|   |   |                    |   |   COMMAND_RESULT            |   +------------------+   |
|   |   | 生命周期:           |   |                              |   +------------------+   |
|   |   | onLoad()           |   |   下行 (Server → Agent)       |   | ML/AI Engine     |   |
|   |   | → onEnable()       |   |   COMMAND / CONFIG           |   | - 行为画像        |   |
|   |   |   → connect WS      |   |   SHUTDOWN                   |   | - 威胁评分        |   |
|   |   |   → register 9      |   |                              |   | - 移动分析        |   |
|   |   |     Listeners       |   |                              |   | - 战斗识别        |   |
|   |   | → onDisable()       |   |                              |   +------------------+   |
|   |   |                    |   |                              |   +------------------+   |
|   |   | 9 Event Listeners: |   |                              |   | DeepSeek AI      |   |
|   |   | PlayerEventListener|   |                              |   | - 告警分析        |   |
|   |   | CombatEventListener|   |                              |   | - 健康报告        |   |
|   |   | ChatEventListener  |   |                              |   | - 自主防御        |   |
|   |   | CommandEventListnr |   |                              |   +------------------+   |
|   |   | BlockEventListener |   |                              |   +------------------+   |
|   |   | InventoryEventListn|   |                              |   | Kernel Engine    |   |
|   |   | EntityEventListener|   |                              |   | - 信号聚合        |   |
|   |   | WorldEventListener |   |                              |   | - 热/共振/控制    |   |
|   |   | PacketEventListener|   |                              |   | - 工作流决策      |   |
|   |   +--------------------+   |                              |   +------------------+   |
|   |                            |                              |   +------------------+   |
|   |   +--------------------+   |                              |   | Web Console      |   |
|   |   | AgentWebSocket     |   |                              |   | Nebula Console   |   |
|   |   | Client             |   |                              |   | http://:8080     |   |
|   |   | (java.net.http)    |   |                              |   | - Dashboard       |   |
|   |   | 断线重连 + 心跳      |   |                              |   | - SSH Gateway     |   |
|   |   +--------------------+   |                              |   | - Operations      |   |
|   |                            |                              |   +------------------+   |
|   |   +--------------------+   |                              |   +------------------+   |
|   |   | InternalCommand    |   |                              |   | AgentWebSocket    |   |
|   |   | Executor           |   |                              |   | Server            |   |
|   |   | Bukkit API 直接     |   |                              |   | - 连接注册管理     |   |
|   |   | 执行命令            |   |                              |   | - 消息路由分发     |   |
|   |   +--------------------+   |                              |   +------------------+   |
|   +----------------------------+                              +--------------------------+
```

### 数据流详解

**阶段 1：Agent 启动与连接**
1. Minecraft 服务端加载 AluerPlugin（`load: STARTUP` 确保最先加载）
2. `onEnable()` 创建 `AgentWebSocketClient` 实例，异步连接到 `ws://host:8080/agent`
3. 连接成功后发送 HANDSHAKE 消息（携带服务器版本、在线玩家数）
4. 启动心跳定时器（每 15 秒发送 HEARTBEAT）
5. 注册 9 个 Bukkit Event Listener

**阶段 2：事件采集与上报**
1. 玩家在游戏中产生动作 → Bukkit 触发对应 Event
2. Listener 的 `@EventHandler` 方法捕获事件，提取关键数据
3. 序列化为 JSON，通过 `AgentWebSocketClient.sendEvent()` 推送
4. Agent 端即时拦截（超高速移动 / 超远攻击 / 命令注入 / 广告），对可取消事件直接 `event.setCancelled(true)`

**阶段 3：ServerGuard 分析**
1. `AgentWebSocketServer` 接收消息，解析 type 字段
2. EVENT → `ServerGuardService` 注入指标流 → `AnomalyDetector` / `TimeSeriesPredictor`
3. ALERT → `handleAlert()` 管道 → `DeepSeekClient.analyzeAlertAsync()` AI 分析
4. DeepSeek 返回结构化 JSON 分析结果（severity / rootCause / recommendedActions / autoAction）

**阶段 4：自动防御执行**
1. `AutoExecutor` 解析 AI 返回的 `autoAction` 字段
2. Agent 模式：`AgentCommandDispatcher` 通过 WebSocket 下发 COMMAND 到 Agent
3. Agent 端 `InternalCommandExecutor` 通过 Bukkit API 直接执行（ban IP / kick player / clear lag / enable whitelist）
4. External 模式：通过 RCON 协议执行

### WebSocket 通信协议

所有消息为单行 JSON，通过 WebSocket 文本帧双向传输。

**Agent → Server（事件上报示例）：**
```json
{
  "type": "EVENT",
  "agentId": "agent-survival-01",
  "timestamp": 1715000000000,
  "payload": {
    "eventType": "PLAYER_MOVE",
    "data": {
      "playerName": "Steve",
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "fromX": 100.0, "fromY": 64.0, "fromZ": 200.0,
      "toX": 100.5, "toY": 64.0, "toZ": 200.3,
      "dx": 0.5, "dy": 0.0, "dz": 0.3,
      "horizontal": 0.583,
      "yaw": 45.0, "pitch": 0.0,
      "isFlying": false, "isGliding": false,
      "isSprinting": true, "isInWater": false,
      "isOnGround": true, "gameMode": "SURVIVAL"
    }
  }
}
```

**Server → Agent（防御指令下发）：**
```json
{
  "type": "COMMAND",
  "requestId": "550e8400-e29b-41d4-a716-446655440001",
  "timestamp": 1715000001000,
  "payload": {
    "command": "BAN_IP",
    "target": "192.168.1.100",
    "reason": "KillAura detected - confidence 94% - multi-target attack pattern"
  }
}
```

**上行消息类型（Agent → Server）：**

| 类型 | 说明 | 触发时机 |
|------|------|---------|
| `EVENT` | Bukkit 事件数据（移动/战斗/方块/聊天/命令等） | 每次事件触发时 |
| `METRICS` | 服务器指标（TPS/CPU/Memory/包速率/在线玩家数） | 每秒定时上报 |
| `ALERT` | Agent 端即时检测到的安全告警 | 即时拦截时 |
| `HEARTBEAT` | 心跳保活（`{"status":"alive"}`） | 每 15 秒 |
| `HANDSHAKE` | 初始握手（服务器版本、在线玩家数） | 连接建立时 |
| `COMMAND_RESULT` | 命令执行结果回执（success + details） | 收到命令并执行后 |

**下行消息类型（Server → Agent）：**

| 类型 | 说明 |
|------|------|
| `COMMAND` | 执行防御/运维命令 |
| `CONFIG` | 配置更新推送 |
| `SHUTDOWN` | 关闭连接 |

**下行命令类型（Server → Agent）：**

| 命令 | 对应操作 | 实现方式 |
|------|---------|---------|
| `BAN_IP` | 封禁 IP 地址 | `Bukkit.banIP()` via dispatchCommand |
| `BAN_PLAYER` | 封禁玩家账号 | `Bukkit.ban()` via dispatchCommand |
| `KICK` | 踢出玩家 | `Player.kick(Component.text(reason))` |
| `CLEAR_LAG` | 清理所有掉落物 | 遍历所有世界清除 Item 实体 |
| `SET_SPAWN_RATE` | 降低生物生成率 | `World.setMonsterSpawnLimit()` |
| `ENABLE_WHITELIST` | 启用紧急白名单 | `Bukkit.setWhitelist(true)` |
| `DISABLE_WHITELIST` | 关闭白名单 | `Bukkit.setWhitelist(false)` |
| `BROADCAST` | 全服广播消息 | `Bukkit.broadcast(Component.text())` |
| `SAVE_ALL` | 强制保存所有世界 | `dispatchCommand("save-all")` |
| `EXECUTE` | 执行任意控制台命令 | `dispatchCommand(target)` |

---

## 安全模块完整清单

模块已按职责拆分到 `com.aluer` 下的 `anticheat`（反作弊）、`defense`（纵深防御）、`network`（网络安全）、`server`（服务器漏洞）、`chat`（聊天安全）、`ml`/`ai`（行为分析与 AI）等子包。

### 战斗类反作弊（`anticheat.combat`，20 模块）

| # | 模块类名 | 对抗的外挂 | 检测原理 |
|---|---------|----------|---------|
| 1 | `AntiKillAuraService` | KillAura（杀戮光环） | 3 秒窗口内目标切换 > 4 个 · 连续 5 次攻击角度偏差 < 5° · 最大攻击距离精确命中 · 单 tick 多点攻击 |
| 2 | `AntiReachService` | Reach（攻击距离扩展） | 攻击距离 > 3.3 方块标记可疑 · > 5.0 方块即时拦截 · 位置回溯验证 |
| 3 | `AntiAutoClickerService` | AutoClicker（自动连点） | CPS > 20 标记 · 连续 50+ 次攻击间隔方差 < 0.08 · 点击间隔香农熵分析 |
| 4 | `AntiCriticalsService` | Criticals（自动暴击） | 零垂直速度 + 地面状态 + 产生暴击 → 不可能 · 无跳跃暴击率 > 20% |
| 5 | `AntiAutoCrystalService` | AutoCrystal（自动末影水晶） | 末影水晶同 tick 放置于黑曜石上方 · 水晶引爆 < 2 tick · 最优爆炸位置计算 |
| 6 | `AntiAutoTotemService` | AutoTotem（自动不死图腾） | 图腾消耗 → 新图腾装备 < 50ms · 连续 5+ 图腾使用均 < 100ms |
| 7 | `AntiSurroundService` | Surround（自动包围） | 脚下方块 4 方向同时放置 · 放置间隔 < 2 tick · 防御方块类型检测 |
| 8 | `AntiAutoTrapService` | AutoTrap（自动困笼） | 目标周围方块网格 < 1 秒完成 · 活塞放置 + 激活同 tick |
| 9 | `AntiAutoArmorService` | AutoArmor（自动盔甲） | 4 个盔甲槽位同 tick 更换 · 打开背包 → 换装 → 关闭 < 3 tick |
| 10 | `AntiChestSwapService` | ChestSwap（快速胸甲切换） | 胸甲 ↔ 鞘翅互换 < 1 tick · 受伤时即时切换 |
| 11 | `AntiAutoLogService` | AutoLog（自动断线） | 受伤 → 断线 < 500ms · 低血量断线 · 排除正常超时 |
| 12 | `AntiHitboxesService` | Hitboxes（扩大碰撞箱） | 攻击射线到实体中心最小距离异常 · 边缘命中率 > 60% |
| 13 | `AntiBowAimBotService` | BowAimbot（弓箭自瞄） | 移动目标命中率 > 90% · 弓箭瞬发精准命中 · 弹道预测精准度 |
| 14 | `AntiVelocityService` | Velocity（击退修改） | 受击后水平速度变化 < 预期 50% · 零击退连续受击 |
| 15 | `AntiAnchorService` | Anchor（洞穴锚定） | 封闭空间受击零位移 < 0.01 方块 · 连续多次受击同一坐标 |
| 16 | `AntiPistonAuraService` | PistonAura（活塞光环） | 战斗上下文活塞激活率异常 · 放置 + 激活同 tick · 活塞方向瞄准玩家 |
| 17 | `AntiAimService` | AimModulo360（Grim） | 原始 yaw 环绕（>320° 且上次 <30°）· 360° 模运算绕过检测 |
| 18 | `AntiNoSwingService` | NoSwing（无挥臂，NCP） | 挥臂标志缺失 + VL 累积/衰减判定 |
| 19 | `AntiGodModeService` | GodMode（无敌，NCP） | 受击后生命值不下降（无敌状态异常） |
| 20 | `AntiFastHealService` | FastHeal（快速回血，NCP） | 回血间隔小于合法间隔 |

### 移动类反作弊（`anticheat.movement`，21 模块）

| # | 模块类名 | 对抗的外挂 | 检测原理 |
|---|---------|----------|---------|
| 1 | `AntiFlyDetectionService` | Fly（飞行外挂） | 非创造/旁观/鞘翅状态垂直上升 · 悬空 > 5 秒 · 上下震荡模式 |
| 2 | `AntiSpeedService` | Speed（加速） | 生存模式水平速度 > 0.65 方块/tick · > 1.2 方块/tick 即时拦截 |
| 3 | `AntiJesusService` | Jesus（水上行走） | 水/岩浆中 y 坐标不下降 · 水中水平高速移动 |
| 4 | `AntiNoFallService` | NoFall（无摔伤） | 坠落距离 > 3.5 方块但伤害 < 1.0 · GroundSpoof + VClip 组合 |
| 5 | `AntiTimerService` | Timer（游戏加速） | 移动 tick 间隔 < 50ms · 持续 30 个加速 tick → 确认 |
| 6 | `AntiPhaseService` | Phase（穿墙） | 两次位置间存在固体方块 · 射线追踪验证 · 排除合法穿透 |
| 7 | `AntiBlinkService` | Blink（闪烁） | 断线 → 重连 < 5 秒 · 受伤后 < 1 秒断线 |
| 8 | `AntiScaffoldService` | Scaffold（自动搭路） | 脚下方块快速连续放置 · 悬空搭路模式 |
| 9 | `AntiSpiderService` | Spider（爬墙） | 无爬墙方块贴墙垂直上升 · 连续贴墙移动 |
| 10 | `AntiStepService` | Step（自动跨步） | 非跳跃 ΔY > 0.9 单 tick · 无跳跃粒子/音效 |
| 11 | `AntiNoSlowService` | NoSlow（无减速） | 使用物品时移速 > 正常值 · 各物品类型独立阈值 |
| 12 | `AntiPacketFlyService` | PacketFly（数据包飞行） | 连续悬浮不落地 · 上下快速震荡模式 |
| 13 | `AntiAirJumpService` | AirJump（空中跳跃） | 离地 > 1 方块时发出跳跃包 · 连续空中跳跃 |
| 14 | `AntiLongJumpService` | LongJump（远跳） | 单次跳跃水平距离 > 8 方块 · 水平速度衰减曲线异常 |
| 15 | `AntiAntiHungerService` | AntiHunger（免饥饿） | 高活动量零饥饿消耗 · onGround 伪造跳过饥饿 tick |
| 16 | `AntiFastFallService` | FastFall（快速下落） | 持续下落速度 > 5 方块/tick（终端速度 3.92） |
| 17 | `AntiVClipService` | VClip（垂直穿墙） | 单 tick ΔY > 3 方块 · 中间位置存在固体方块 · 排除合法传送 |
| 18 | `AntiElytraFlyService` | ElytraFly（鞘翅操控） | 鞘翅滑翔速度 > 60 方块/秒 · 无烟花持续上升 |
| 19 | `AntiFastLadderService` | FastLadder（快速爬梯，NCP） | 攀爬状态单 tick 上升超速 |
| 20 | `AntiNoWebService` | NoWeb（蜘蛛网减速绕过，NCP） | 蜘蛛网内水平位移超速 |
| 21 | `AntiGroundSpoofService` | GroundSpoof（伪造地面，Grim） | onGround=true 却持续下落的伪造地面状态 |

### 世界类反作弊（`anticheat.world`，14 模块）

| # | 模块类名 | 对抗的外挂 | 检测原理 |
|---|---------|----------|---------|
| 1 | `AntiNukerService` | Nuker（炸图） | 短时间内破坏方块 > 25 个 · 同 tick 破坏多个不相邻方块 |
| 2 | `AntiAutoMineService` | AutoMine（自动挖矿） | 挖掘间隔方差 < 0.05 秒 · 连续精准切换目标 · 无交互挖掘 |
| 3 | `AntiSpeedMineService` | SpeedMine（加速挖掘） | 黑曜石破坏 < 1 秒 · 石头破坏 < 0.1 秒 · 无视线挖掘 |
| 4 | `AntiFastBreakService` | FastBreak（快速破坏） | 方块破坏时间 < 标准时间 × 0.5 · 附魔/状态效果调整 |
| 5 | `AntiFastUseService` | FastUse（快速使用） | 食物消耗 < 1.0 秒 · 弓箭拉满 < 0.5 秒 · 药水 < 0.8 秒 |
| 6 | `AntiNoInteractService` | NoInteract（交互绕过） | 持剑打开箱子 · 未潜行 + 持有阻挡物品 + 成功打开容器 |
| 7 | `AntiVeinMinerService` | VeinMiner（连锁挖矿） | 相邻同类型矿石连续快速破坏 · 穿墙矿石挖掘 |
| 8 | `AntiAutoToolService` | AutoTool（自动工具） | 切换最优工具 + 开始挖掘同 tick · 完美工具匹配率 > 90% |
| 9 | `AntiAutoFishService` | AutoFish（自动钓鱼） | 鱼咬钩 < 100ms 收杆 · 连续快速收杆 > 5 次/30 秒 |
| 10 | `AntiChestStealService` | ChestSteal（自动偷箱） | 打开箱子 → 取物 < 200ms · 连续快速取走整组物品 |
| 11 | `AntiXrayDetectionService` | Xray（透视） | 钻石/矿石发现率异常 · 直线挖掘轨迹 · 暗处精准定位矿物 |
| 12 | `AntiBaritoneService` | Baritone（AI 矿透） | 路径平滑度 > 0.95 · 行为重复率异常 · 无视觉反馈挖掘 |
| 13 | `AntiGriefDetectionService` | Grief（破坏） | 贵重方块大量破坏 · TNT 频繁使用 · 纵火模式 |
| 14 | `AntiDupeDetectionService` | Dupe（物品复制） | 堆叠异常 / 高价值暴涨 / 容器复制 / 死亡复制等 |

### 玩家类反作弊（`anticheat.player`，9 模块）

| # | 模块类名 | 对抗的外挂 | 检测原理 |
|---|---------|----------|---------|
| 1 | `AntiFakePlayerService` | FakePlayer（假人） | 无心跳响应 · 无世界交互 · 完美静止或循环巡逻 · 受击无反 |
| 2 | `AntiInventoryManipulationService` | Inventory（背包操控） | 非正常速度背包操作 · 非法槽位移动 · 一键整理异常速度 |
| 3 | `AntiNameSpoofService` | NameSpoof（名称冒充） | 管理员/知名玩家昵称匹配 · Unicode 同形字替换 |
| 4 | `AntiSkinSpoofService` | SkinSpoof（皮肤伪造） | 皮肤模型数据异常 · 皮肤 URL 来源检测 |
| 5 | `AntiAltAccountService` | AltAccount（小号） | 同 IP 多账号在线 · 行为相似度分析 · 登录时间模式关联 |
| 6 | `AntiOfflineModeSpoofService` | OfflineSpoof（离线 UUID） | 正版 UUID 冲突检测 · IP 关联验证 · 离线-正版交叉验证 |
| 7 | `AntiVPNProxyService` | VPN/Proxy（代理） | 已知 VPN IP 库 · 托管 ASN 匹配 |
| 8 | `AntiBotDetectionService` | Bot（机器人） | 命名模式 · 加入速率 · 行为熵值 |
| 9 | `AntiBadPacketsService` | BadPackets（恶意数据包，Grim） | 重复 held item slot / 未睡觉却发送起床 / pitch 越界 / 连续移动包无位置更新 / 重复 sprint 状态 |

### 服务器漏洞防护（`server`，6 模块）

| # | 模块类名 | 防护对象 |
|---|---------|---------|
| 1 | `AntiSignExploitService` | 告示牌 NBT 漏洞（超长 JSON / 无效组件 / NBT 炸弹） |
| 2 | `AntiBookBanService` | 书与笔封禁漏洞（超大页码 / 超深 JSON 层级） |
| 3 | `AntiResourcePackExploitService` | 资源包漏洞（恶意 URL / 超大文件 / 格式校验） |
| 4 | `AntiTabCompleteCrashService` | Tab 补全崩溃（长文本 / 深度嵌套补全限制） |
| 5 | `CrashExploitProtectionService` | 崩溃漏洞防护（超大包 / NBT 炸弹 / 书与笔攻击） |
| 6 | `LagMachineDetectionService` | 卡服机检测（Observer 链 / TNT 堆 / 红石密度） |

### 纵深防御（`defense`，58 模块）

`AdvancedMalwareDetectionService`（恶意软件检测）· `AntiStashFinderService`（仓库扫描）· `BackdoorPluginScannerService`（后门插件）· `BackupIntegrityService`（备份完整性）· `BackupSecurityService`（备份安全）· `BotFingerprintDetector`（机器人指纹）· `ChunkLoadRateLimiter`（区块加载限速）· `CommandExecutionGuardService`（命令执行守护）· `ComplianceScannerService`（合规审计）· `ConfigTamperDetectionService`（配置篡改）· `ConnectionHandshakeValidator`（握手验证）· `ContainerSecurityService`（容器安全）· `CrashExploitSignatureDB`（崩溃签名库）· `CSPEnforcementService`（CSP 头强制）· `DatabaseFirewallService`（数据库防火墙）· `DataLossPreventionService`（数据防泄漏）· `DDoSDefenseCoordinator`（DDoS 协调）· `EncryptionService`（加密）· `EndpointDetectionResponseService`（EDR）· `EntityCountEnforcer`（实体数量）· `ExploitSignatureService`（漏洞签名）· `FileIntegrityMonitorService`（文件完整性）· `FileSecurityScannerService`（文件安全扫描：webshell/后门脚本）· `ForensicsCollectorService`（取证收集）· `GeoBlockService`（地理封锁）· `HoneypotService`（蜜罐）· `HostEnforcementService`（主机强制执行）· `HostIntrusionCountermeasureService`（主机入侵对抗）· `IncidentResponseService`（事件响应）· `IntrusionDetectionService`（IDS）· `IntrusionPreventionSystem`（IPS）· `JwtAuthService`（JWT 认证）· `LogAnalysisService`（日志分析）· `LogCorrelationService`（日志关联）· `MemoryProtectionService`（内存保护）· `NBTExploitPrevention`（NBT 漏洞防护）· `OPPrivilegeMonitorService`（OP 权限监控）· `PlayerSessionValidationService`（会话验证）· `PluginVerificationService`（插件校验）· `ProcessInjectionDetectionService`（进程注入检测）· `ProcessSecurityService`（进程安全扫描：挖矿木马/反弹 shell）· `ProtocolStateValidator`（协议状态机）· `RedstoneUpdateLimiter`（红石更新限速）· `SecureFileDeletionService`（安全删除）· `SecurityAutomationScheduler`（安全自动化）· `SecurityBaselineHardeningService`（安全基线）· `SecurityOrchestrationService`（安全编排）· `SessionManagementService`（会话管理）· `SIEMService`（SIEM）· `SSRFProtectionService`（SSRF 防护）· `ThreatHuntingService`（威胁狩猎）· `ThreatIntelligenceService`（威胁情报）· `TokenBucketRateLimiter`（令牌桶限速）· `TrafficShapingService`（流量整形）· `WafRequestFilter`（WAF 过滤器）· `WebApplicationFirewall`（WAF）· `XXEProtectionService`（XXE 防护）· `ZeroTrustArchitectureService`（零信任）

### 网络安全（`network`，28 模块）

`APIRateLimitService`（API 限速）· `ARPSpoofDetectionService`（ARP 欺骗）· `BruteForceProtectionService`（暴力破解）· `CloudflareIntegrationService`（Cloudflare 集成）· `ConnectionThrottleService`（连接限速）· `DDoSProtectionService`（DDoS 防护）· `DistributedAttackMitigationService`（分布式攻击缓解）· `DNSSecurityService`（DNS 安全）· `DNSTunnelDetectionService`（DNS 隧道）· `FirewallService`（防火墙）· `FlowAnalyzerService`（流量分析）· `GeoIPService`（GeoIP）· `IPReputationService`（IP 信誉）· `LoadBalancerService`（负载均衡）· `MinecraftProtocolSecurityService`（MC 协议安全）· `NetworkMonitorService`（网络监控）· `NetworkSegmentationService`（网络分段）· `NetworkSnifferService`（网络嗅探）· `NetworkThreatFusionService`（网络威胁融合）· `PacketFloodProtectionService`（数据包洪水）· `PacketInspectionService`（数据包检测）· `PortScanDetectionService`（端口扫描）· `ProtocolAnalysisService`（协议分析）· `RateLimitService`（速率限制）· `ReverseShellDetectionService`（反弹 Shell）· `SSLMonitorService`（SSL 监控）· `SSLTLSCertificateService`（SSL/TLS 证书）· `TrafficAnalysisService`（流量分析）

### 聊天与社交安全（`chat`，6 模块）

| # | 模块类名 | 防护对象 |
|---|---------|---------|
| 1 | `ChatFloodProtectionService` | 聊天洪水（10 秒内 5 条消息触发） |
| 2 | `AntiAdvertisementService` | IP/域名/群号正则匹配 |
| 3 | `AntiPhishingLinkService` | 钓鱼链接/短链接/可疑域名 |
| 4 | `AntiCommandAbuseService` | 敏感命令监控 + 命令注入检测 |
| 5 | `PlayerPrivacyService` | IP 脱敏/坐标隐藏/日志匿名化 |
| 6 | `ChatFilterService` | 聊天内容过滤 |

### ML 与 AI 行为分析引擎（12 模块）

| # | 模块类名 | 技术方法 |
|---|---------|---------|
| 1 | `BehavioralProfilingEngine` | 5 维特征向量 · Shannon 熵 · Z-score 基线 · 6 类玩家画像 |
| 2 | `ThreatScoreAggregator` | 指数衰减加权 · 4 级威胁上报 · Top-N 威胁排名 |
| 3 | `MovementPatternAnalyzer` | 100 tick 滑动窗口 · FFT 频谱分析 · 旋转平滑度 · 宏匹配 |
| 4 | `CombatPatternRecognizer` | CPS 变异系数 · 多目标熵 · 扫视分析 · 超距攻击比 |
| 5 | `AnomalyDetector` | 隔离森林（Isolation Forest）异常检测 |
| 6 | `TimeSeriesPredictor` | TPS/CPU/Memory 时间序列预测 · 60 分钟预测视野 |
| 7 | `DeepSeekClient` | DeepSeek API 客户端（告警分析/健康报告） |
| 8 | `AIAutonomousService` | AI 自治防御服务 |
| 9 | `AIStrategyEngine` | AI 策略引擎 |
| 10 | `AluerSovereignEngine` | 主权 AI 引擎 |
| 11 | `AdaptiveThreshold` | 自适应阈值 |
| 12 | `AttackDetector` | 攻击模式检测 |

---

## 开源反作弊融合

项目采用 **GPL-3.0** 许可证（见根目录 `LICENSE`），与 [GrimAC](https://github.com/GrimAnticheat/Grim)、[NoCheatPlus](https://github.com/NoCheatPlus/NoCheatPlus) 等 GPL 开源反作弊许可证兼容，因此可直接合并其检测算法。

**已合并的检测算法：**

| 来源 | Check | Aluer 服务 |
|------|-------|-----------|
| GrimAC | BadPacketsA/C/D/E/F（held item slot / leave bed / pitch 越界 / 移动包无位置更新 / 重复 sprint） | `AntiBadPacketsService` |
| GrimAC | AimModulo360（原始 yaw 环绕） | `AntiAimService.checkModulo360` |
| GrimAC | GroundSpoof（onGround=true 却持续下落） | `AntiGroundSpoofService` |
| NoCheatPlus | fight/NoSwing（挥臂标志 + VL） | `AntiNoSwingService` |
| NoCheatPlus | fight/GodMode（受击后生命不降） | `AntiGodModeService` |
| NoCheatPlus | fight/FastHeal（回血间隔过短） | `AntiFastHealService` |
| NoCheatPlus | FastLadder（爬梯超速） | `AntiFastLadderService` |
| NoCheatPlus | NoWeb（蜘蛛网内位移超速） | `AntiNoWebService` |

**移动模拟引擎移植（进行中）：** 为支持 Reach / Timer / Velocity 等预测式检测，已启动 Grim 移动模拟引擎移植，保留 `ac.grim.grimac` 原始包名以便同步上游。当前已完成 **Phase 1**（引入 PacketEvents 2.7.0 依赖 + `utils/math` 基础库 8 个文件），完整移植约 8 万行，按依赖图逐层推进。

详见 [`docs/开源反作弊融合.md`](docs/开源反作弊融合.md)。

---

## 多平台多版本支持

通过 `com.aluer.platform` 平台抽象层，同一 Agent 插件可运行于全部 Bukkit 系平台，覆盖 **Minecraft 1.12.2 至今**。

| 平台族 | 平台 | 说明 |
|--------|------|------|
| Bukkit 系 | Vanilla(CraftBukkit) / Spigot / Paper / Purpur / Pufferfish / Folia / Glowstone | 直接运行（`plugin.yml`，`api-version: '1.12'` + `folia-supported: true`） |
| 代理端 | BungeeCord / Waterfall / Velocity | 需独立适配器（引擎已具备类型识别能力） |
| 模组加载器 | Forge / NeoForge / Fabric / Sponge | 需独立适配器（引擎已具备类型识别能力） |

**核心类型：**
- `MinecraftVersion`：解析、比较版本号（1.12.2 → 1.21.x），支持 `isAtLeast` / `isSupported`。
- `PlatformType`：平台类型枚举（Bukkit 系 / 代理端 / 模组加载器）。
- `PlatformDetector`：运行时反射探测平台与版本。
- `BukkitCompat`：跨版本反射兼容层，收敛所有"版本敏感"调用（`getPing` / `kick` / `broadcast` / `getTps`）。

**进程定位器：** `MinecraftProcessDetector` 使用 `ProcessHandle` API 检测 Minecraft 服务器进程，支持按已知核心名（paper/spigot/purpur/folia/forge/fabric 等）自动识别，无需硬编码；`ServerDirectoryResolver` 自动寻找服务端真实工作目录与日志路径。

详见 [`docs/插件系统与多平台支持.md`](docs/插件系统与多平台支持.md)。

---

## 插件系统

插件系统允许第三方开发者以独立 JAR 扩展 ServerGuard 引擎能力，无需修改引擎源码。

**核心类型（`com.aluer.pluginsystem`）：** `ServerGuardPlugin`（生命周期接口）· `Plugin`（元数据注解）· `PluginDescriptor`（描述符）· `PluginManager`（发现/加载/启用/停用）· `PluginContext`（插件上下文）· `PluginClassLoader`（隔离类加载器）· `PluginSystemService`（Spring 集成）。

插件实现 `ServerGuardPlugin` 接口并用 `@Plugin` 注解声明元数据，打包后放入 `serverguard/plugins/` 目录即可自动加载：

```java
@Plugin(name = "my-guard", version = "1.0.0", author = "example",
        description = "示例安全模块", depends = {})
public class MyGuardPlugin implements ServerGuardPlugin {
    private PluginContext context;

    @Override
    public void onLoad(PluginContext context) {
        this.context = context;
        context.registerCommand("myguard-scan", "扫描示例", args -> "scan complete");
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

## Agent 插件自动注入

ServerGuard 采用「外部引擎 + 游戏内 Agent」双体架构。引擎产物是 Spring Boot fat jar，其类位于 `BOOT-INF/classes/` 下，Bukkit 插件类加载器只识别 jar 根目录的标准类结构，因此 fat jar 无法被服务器直接加载。

`com.aluer.deploy.AgentDeployService` 在运行时把 Agent 最小依赖闭包（`com.aluer.plugin / platform / agent / model` 包）重新打包成标准结构的插件 JAR，写入服务端 `plugins/` 目录，并支持**周期补注入**（防止插件被误删）。

```yaml
serverguard:
  minecraft:
    working-dir: /opt/minecraft
    auto-inject: true
    agent-jar-name: serverguard-agent.jar
```

---

## 外挂对抗覆盖矩阵

Aluer ServerGuard 实现对主流 Minecraft 外挂客户端全类型 hack 模块的检测覆盖。

| 类别 | 常见外挂模块 | Aluer 对抗模块数 |
|------|-------------|----------------|
| **Combat（战斗）** | KillAura · AimAssist · AutoCrystal · Criticals · Surround · AutoTrap · AutoArmor · AutoTotem · BowAimbot · Hitboxes · Velocity · Anchor · PistonAura 等 | 20 |
| **Movement（移动）** | Fly · Speed · Jesus · NoFall · Timer · Phase · Blink · Scaffold · Spider · Step · NoSlow · PacketFly · AirJump · LongJump · ElytraFly · VClip 等 | 21 |
| **World（世界）** | Nuker · SpeedMine · VeinMiner · Xray · Grief · AutoMine · AutoFish · FastBreak · Baritone 等 | 14 |
| **Player（玩家）** | FakePlayer · InventoryTweaks · NameSpoof · AltAccount · BadPackets · Bot 等 | 9 |
| **总计** | **110+ 种外挂** | **64** |

---

## 部署指南

### Agent Plugin 模式部署（推荐）

生产环境首选。Agent 插件植入 Minecraft 服务器进程内部，零网络延迟，50,000+ 事件/秒吞吐。

**系统要求：**
- Java 21+
- Bukkit 系服务器（Spigot / Paper / Purpur / Folia），Minecraft 1.12.2+
- ServerGuard 引擎与服务器可网络互通（同机 localhost 最优）

**步骤 1：启动 ServerGuard 引擎**

```bash
java -jar serverguard.jar
# 输出：
# ╔══════════════════════════════════════════════════════════════╗
# ║          Aluer ServerGuard V5.2.0                           ║
# ║          Mode: Agent Server (WebSocket)                     ║
# ║          Agent WS: ws://0.0.0.0:8080/agent                 ║
# ║          Web Console: http://0.0.0.0:8080/                 ║
# ╚══════════════════════════════════════════════════════════════╝
```

**步骤 2：安装 Agent 插件**（引擎可自动注入，或手动安装）

```bash
# 方式 A：自动注入（推荐）—— 配置 working-dir 后引擎启动时自动部署
# 方式 B：手动复制 JAR 到 plugins 目录
cp serverguard.jar /opt/minecraft/plugins/AluerServerGuard.jar

# 创建 Agent 配置目录和文件
mkdir -p /opt/minecraft/plugins/AluerServerGuard
cat > /opt/minecraft/plugins/AluerServerGuard/config.yml << 'EOF'
# Aluer Agent 配置文件
# Agent 通过 WebSocket 连接外部 ServerGuard 引擎
server-url: ws://localhost:8080/agent
# 是否在服务器控制台回显发往引擎的消息摘要（默认开启）
log-outbound: true
EOF
```

**步骤 3：启动 Minecraft 服务器**

```bash
cd /opt/minecraft
java -Xms4G -Xmx4G -jar paper-1.21.1.jar nogui

# Agent 启动日志：
# [AluerServerGuard] ═════════════════════════════════════════════
# [AluerServerGuard]   Aluer ServerGuard Agent v5.2.0
# [AluerServerGuard]   轻量数据采集前端 - 连接外部 ServerGuard 引擎
# [AluerServerGuard] ═════════════════════════════════════════════
# [AluerServerGuard] Agent a1b2c3d4 connected to ServerGuard successfully
# [AluerServerGuard]   Listeners: 9 event handlers registered
# [AluerServerGuard]   Commands: /aluer /aluerstatus /aluerplayers /aluerblock /aluerunblock /aluerscan /aluerwhitelist
```

**步骤 4：验证连接**

在 Minecraft 游戏内执行 `/aluer status`，应显示 Agent 已连接、当前 TPS、在线玩家数。

### External 外部模式部署

适用于不想安装插件的场景。通过 RCON 和日志解析进行外部监控。

**步骤 1：配置 Minecraft RCON**

编辑 `server.properties`：
```properties
enable-rcon=true
rcon.port=25575
rcon.password=YOUR_STRONG_PASSWORD
```

**步骤 2：配置 ServerGuard**

创建 `application.yml`：
```yaml
serverguard:
  mode: external
  minecraft:
    process-name: ""          # 留空则自动识别
    working-dir: ""           # 留空则自动寻找
    rcon:
      enabled: true
      host: localhost
      port: 25575
      password: YOUR_STRONG_PASSWORD
```

**步骤 3：启动 ServerGuard**
```bash
java -jar serverguard.jar
```

### 模式对比

| 特性 | Agent Plugin 模式 | External 外部模式 |
|------|------------------|-------------------|
| 通信方式 | WebSocket 双向实时 | RCON + 日志解析 |
| 延迟 | < 1ms（同机） | RCON 50-200ms |
| 事件吞吐 | 50,000+ 事件/秒 | 依赖日志写入速度 |
| 数据精度 | Bukkit 事件级（精确到 tick） | 日志文本级（秒级延迟） |
| Minecraft 侵入性 | 需安装 Paper 插件（几十 MB） | 零侵入（仅需 RCON 端口） |
| 即时拦截能力 | 是（event.setCancelled） | 否（告警后执行） |
| 反作弊精度 | 最高（原始事件数据） | 较低（日志文本匹配） |
| 模块覆盖 | 全部 170+ 模块 | 外部监控模块（约 40 模块） |
| 推荐场景 | **生产服务器（首选）** | 评测 / 临时监控 / 测试 |

### Docker / Kubernetes 部署

项目内置容器化部署支持（`Dockerfile` · `.dockerignore` · `docker-compose.yml` · `k8s/`），适合 External 模式容器化部署：

```bash
# 构建镜像
docker build -t aluer/serverguard:5.2.0 .

# Docker Compose 一键编排（引擎 + Paper 服务端）
docker compose up -d --build

# Kubernetes 部署（External 模式）
kubectl create namespace aluer
kubectl apply -f k8s/
```

> **容器化能力边界**：引擎内置的进程扫描 / 网络嗅探 / ARP 检测等主机级模块在容器内受限（只能看到容器自身），External 模式（RCON 远程监控 + 网络层防护）与 Agent 模式（WebSocket 通信）可完整容器化。详见 [`docs/Docker_K8S部署.md`](docs/Docker_K8S部署.md)。

---

## 配置参考

### 核心配置（serverguard.*）

```yaml
serverguard:
  # 运行模式：plugin（Paper 插件 Agent 模式）或 external（传统外部监控）
  mode: ${SERVERGUARD_MODE:external}

  # 插件系统
  plugin:
    enabled: true
    directory: ${SERVERGUARD_PLUGIN_DIR:serverguard/plugins}

  minecraft:
    service-name: minecraft           # 系统服务名（用于 systemd 管理）
    process-name: ""                  # 留空则按已知核心名自动识别
    jar-file: ""                      # 服务端 JAR 文件名
    working-dir: ${SERVERGUARD_WORKING_DIR:}  # 留空则自动寻找
    auto-inject: true                 # 启动时自动注入 Agent 插件
    agent-jar-name: serverguard-agent.jar
    java-opts: -Xms4G -Xmx4G          # JVM 启动参数
    check-interval-seconds: 5         # 进程保活检查间隔（秒）
    rcon:
      enabled: true
      host: localhost
      port: 25575
      password: ${RCON_PASSWORD:}

  monitor:
    tps-threshold: 15
    cpu-threshold: 80.0
    memory-threshold: 85.0
    connection-threshold: 50
    log-watch-lines: 100
    log-path: ${SERVERGUARD_LOG_PATH:}  # 留空则自动推导
```

### AI 配置（serverguard.ai.*）

```yaml
  ai:
    enabled: true
    use-isolation-forest: true        # 隔离森林异常检测
    use-prediction: true              # 时间序列预测
    sliding-window-size: 100
    anomaly-threshold: 0.7
    prediction-horizon-minutes: 60
    deepseek:
      enabled: true
      api-key: ${DEEPSEEK_API_KEY:}
      base-url: ${DEEPSEEK_BASE_URL:https://api.deepseek.com}
      model: ${DEEPSEEK_MODEL:deepseek-chat}
      max-tokens: 1000
      temperature: 0.35
      auto-analyze-alerts: true
      analysis-interval-seconds: 45
      auto-execute:
        enabled: true
        ban-ip: true
        kill-entity: true
        clear-lag: true
        set-spawn-rate: true
        kick-player: true
        whitelist: true
        min-confidence: 88            # 自动执行所需最低 AI 置信度（%）
```

### 安全策略配置（serverguard.security.*）

```yaml
  security:
    enabled: true
    auto-ban-vpn: true
    check-on-login: true
    max-connections-per-ip: 3
    block-common-exploits: true
    log-all-commands: true
    host-enforcement:
      enabled: true
      dry-run: true                   # 仅记录不执行
    self-healing:
      enabled: true
      dry-run: true
      tps-emergency-threshold: 12
      cpu-emergency-threshold: 92.0
      memory-emergency-threshold: 95.0
    kernel:
      enabled: true
      pulse-interval-seconds: 30
    task-bus:
      enabled: true
      auto-dispatch: true
    super-evolution:
      # 反作弊 / 网络 / 主机 / 聊天 等 100+ 个模块开关
      anti-kill-aura: true
      anti-speed: true
      # ...（完整清单见 application.yml）
```

完整配置文件参考见 `src/main/resources/application.yml`，包含 326 行完整配置及中文注释。

---

## 游戏内命令参考

Agent Plugin 模式提供以下游戏内管理命令：

| 命令 | 权限 | 说明 |
|------|------|------|
| `/aluer status` | `aluer.status` | 查看防护状态：TPS、在线玩家数、包速率、Spring 上下文状态、事件处理统计 |
| `/aluer scan <玩家名>` | `aluer.admin` | 深度扫描指定玩家：位置/IP/Ping/飞行状态/冲刺/潜行/最近攻击目标数/攻击角度一致性 |
| `/aluer info` | `aluer.status` | 显示系统版本信息：版本号、作者、技术栈、模块数、运行模式 |
| `/aluerplayers` | `aluer.status` | 查看在线玩家列表，带有风险标记（[多目标] / [瞄准可疑]） |
| `/aluerblock player <名> [理由]` | `aluer.admin` | 封禁玩家并广播通知全服 |
| `/aluerblock ip <地址> [理由]` | `aluer.admin` | 封禁 IP 并踢出该 IP 所有在线玩家 |
| `/aluerunblock player <名>` | `aluer.admin` | 解除玩家封禁 |
| `/aluerunblock ip <地址>` | `aluer.admin` | 解除 IP 封禁 |
| `/aluerwhitelist on` | `aluer.admin` | 紧急启用白名单模式（全服广播警告） |
| `/aluerwhitelist off` | `aluer.admin` | 关闭白名单模式 |
| `/aluerwhitelist status` | `aluer.admin` | 查看当前白名单状态 |

---

## 构建与测试

### 构建命令

```bash
# 编译项目（clean 全量编译）
./apache-maven-3.9.6/bin/mvn clean compile

# 运行全量测试（456 项，JUnit 5，无 Spring 上下文）
./apache-maven-3.9.6/bin/mvn clean test

# 打包（跳过测试，生成 serverguard.jar）
./apache-maven-3.9.6/bin/mvn clean package -DskipTests

# 清理构建产物
./apache-maven-3.9.6/bin/mvn clean
```

### CI/CD 自动化

项目内置 GitHub Actions 工作流（`.github/workflows/`）：

- **`ci.yml`**：推送到 `main` 或发起 PR 时触发，构建前端 → `mvn clean test` 跑全量测试 → 上传 surefire 测试报告。
- **`release.yml`**：推送 `v*` 标签时触发，构建前端 → `mvn clean package`（含全量测试）→ 自动创建 GitHub Release 并附上 `serverguard.jar`。

### 一键部署脚本

- **`install.sh`**：一键安装脚本，创建 systemd 服务并启动（`sudo bash install.sh`）。
- **`start.sh`**：快速启动脚本，`nohup` 后台运行引擎并输出日志。
- **`systemd/`**：预置 `serverguard.service` / `minecraft.service` / `sudoers.serverguard` 单元文件。

### 测试统计

| 指标 | 数值 |
|------|------|
| 测试文件 | 36 个（`V40AntiCheatTest` / `V40PlayerBehaviorTest` / `V40ServerProtectionTest` / `V40AccessControlTest` / `V40ChatSecurityTest` / `V50AntiCheatExtendedTest` / `V50ServerProtectionTest` / `SuperEvolutionSecurityTest` / `MinecraftProcessDetectorTest` / `PluginManagerTest` 等） |
| 测试用例 | 456 项 |
| 测试结果 | **456/456 全部通过，0 失败，0 错误，0 跳过** |
| 测试框架 | JUnit 5（JUnitPlatformProvider） |
| 测试模式 | 纯单元测试，无 Spring 上下文（服务使用无参构造函数 + `new ServerGuardConfig()`） |

### 测试规范（打靶试验）

项目遵循严格的"打靶试验"测试纪律：

- **双构造函数模式**：每个 `@Service` 类同时提供无参构造函数（用于测试）和 `@Autowired` 全参构造函数（用于 Spring 注入），确保测试零依赖
- **内部静态结果类**：检测结果统一使用内部静态结果类 + 静态工厂方法（`clean()` / `suspicious()` / `blocked()` / `flagged()`），类型安全
- **生产对齐**：测试数据和阈值与生产环境完全一致，不做简化或 mock
- **全量验证**：修改代码前后必须跑全量 `mvn clean test`，任何失败必须立即修复

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 核心编程语言 |
| Spring Boot | 3.2.0 | 应用框架（Web · WebSocket · Mail · Shell · Actuator · Configuration Processor） |
| Maven | 3.9.6（bundled） | 构建管理 |
| PaperMC API | 1.21.1-R0.1-SNAPSHOT（provided） | Minecraft 服务端集成（编译目标） |
| PacketEvents | 2.7.0（provided） | 数据包级反作弊（运行时由 PacketEvents 插件提供） |
| JetBrains Annotations | 24.0.0（provided） | Grim 引擎代码编译期注解 |
| Spring WebSocket | 3.2.0 | Agent ↔ ServerGuard 双向通信 |
| Spring Mail | 3.2.0 | 邮件告警通知 |
| Spring Shell | 3.1.3 | CLI 命令行交互 |
| Apache Commons Math3 | 3.6.1 | 统计分析（DescriptiveStatistics/TTest/FastFourierTransformer） |
| Gson | 2.10.1 | JSON 序列化/反序列化 |
| SnakeYAML | 2.2 | YAML 配置文件解析 |
| JSch | 0.2.20 | SSH 远程网关 |
| Lombok | 1.18.38（optional） | 代码简化 |
| React | 19.1.0 | Web 控制台前端（Nebula Console） |
| Vite | 6.3.5 | 前端构建 |
| DeepSeek API | /v1/chat/completions | AI 大模型决策 |

---

## 环境变量参考

| 变量 | 说明 | 默认值 | 必须 |
|------|------|--------|------|
| `SERVERGUARD_MODE` | 运行模式：`plugin` 或 `external` | `external` | 否 |
| `SERVERGUARD_WORKING_DIR` | 服务端工作目录（留空自动寻找） | (空) | 否 |
| `SERVERGUARD_LOG_PATH` | 日志文件路径（留空自动推导） | (空) | 否 |
| `SERVERGUARD_PLUGIN_DIR` | 插件目录 | `serverguard/plugins` | 否 |
| `DEEPSEEK_API_KEY` | DeepSeek AI 接口密钥 | (空，AI 自动禁用) | 否 |
| `DEEPSEEK_BASE_URL` | DeepSeek API 地址 | `https://api.deepseek.com` | 否 |
| `DEEPSEEK_MODEL` | DeepSeek 模型名称 | `deepseek-chat` | 否 |
| `RCON_PASSWORD` | Minecraft RCON 密码（External 模式） | (空) | External 模式须 |
| `SERVER_PORT` | ServerGuard Web 服务端口 | `8080` | 否 |
| `ALUER_ALERT_SMTP_USERNAME` | 告警邮件 SMTP 发件用户名 | (空) | 邮件告警须 |
| `ALUER_ALERT_SMTP_PASSWORD` | 告警邮件 SMTP 发件密码 | (空) | 邮件告警须 |
| `ALUER_ALERT_EMAIL_PRIMARY` | 主告警接收邮箱 | (空) | 邮件告警须 |
| `ALUER_ALERT_EMAIL_SECONDARY` | 备用告警接收邮箱 | (空) | 否 |
| `ALUER_CLOUDFLARE_ZONE_ID` | Cloudflare Zone ID（边缘防御） | (空) | Cloudflare 须 |
| `ALUER_CLOUDFLARE_API_KEY` | Cloudflare API Key | (空) | Cloudflare 须 |
| `ALUER_CLOUDFLARE_EMAIL` | Cloudflare 账户邮箱 | (空) | Cloudflare 须 |
| `ALUER_WEBHOOK_DISCORD` | Discord Webhook 通知地址 | (空) | 否 |
| `ALUER_WEBHOOK_SLACK` | Slack Webhook 通知地址 | (空) | 否 |

---

## 项目结构

```
Aluer/
├── pom.xml                                    # Maven 构建配置（Java 21, Paper API, PacketEvents, GPL-3.0）
├── LICENSE                                     # GPL-3.0 许可证
├── CLAUDE.md                                  # AI 开发规范（并行Agent/打靶试验/零伪代码等）
├── README.md                                  # 项目说明文档（中文·本文件）
├── README_EN.md                               # 项目说明文档（英文）
├── logo.png                                   # 项目 Logo
├── install.sh                                 # 一键安装脚本（systemd 部署）
├── start.sh                                   # 快速启动脚本
├── systemd/                                   # systemd 服务单元（serverguard / minecraft / sudoers）
├── Dockerfile                                 # 多阶段 Docker 构建（前端 → Maven → JRE）
├── .dockerignore                              # Docker 构建上下文精简
├── docker-compose.yml                         # Docker Compose 编排（External 模式示例）
├── k8s/                                       # Kubernetes 清单（ConfigMap/Secret/PVC/Deployment/Service）
├── .github/workflows/                         # GitHub Actions CI 与自动发布工作流
├── docs/
│   ├── DEVELOPER.md                           # 开发者参考手册
│   ├── PROJECT_SUMMARY.md                     # 项目总览与模块拆解
│   ├── USER_MANUAL.md                         # 用户安装与运维手册
│   ├── 开源反作弊融合.md                        # Grim/NCP 融合说明与许可证合规
│   └── 插件系统与多平台支持.md                   # 插件系统与多平台/多版本支持说明
├── apache-maven-3.9.6/                        # 捆绑的 Maven 3.9.6
├── src/
│   ├── main/java/com/aluer/
│   │   ├── ServerGuardApplication.java        # Spring Boot 主入口
│   │   ├── agent/AgentMessage.java            # Agent ↔ Server 通信协议定义
│   │   ├── config/
│   │   │   ├── ServerGuardConfig.java         # 完整配置类（30+ 内部配置类）
│   │   │   ├── CorsConfig.java                # CORS 跨域配置
│   │   │   ├── WebSocketConfig.java           # WebSocket 端点配置（/agent）
│   │   │   └── ShellPromptProvider.java       # Shell 提示符
│   │   ├── model/                             # 告警事件/告警类型/指标数据模型
│   │   ├── anticheat/                         # 反作弊模块（64 个）
│   │   │   ├── combat/                        # 战斗类（20）
│   │   │   ├── movement/                      # 移动类（21）
│   │   │   ├── world/                         # 世界类（14）
│   │   │   └── player/                        # 玩家类（9）
│   │   ├── defense/                           # 纵深防御与主机安全（58 个）
│   │   ├── network/                           # 网络安全（28 个）
│   │   ├── server/                            # 服务器漏洞防护（7 个）
│   │   ├── chat/                              # 聊天与社交安全（6 个）
│   │   ├── ml/                                # ML 行为分析引擎（4 个）
│   │   ├── ai/                                # AI 集成（8 个）
│   │   ├── kernel/                            # Kernel 自治引擎（3 个）
│   │   ├── plugin/                            # Paper Agent 插件
│   │   │   ├── AluerPlugin.java               # JavaPlugin 主入口
│   │   │   ├── AluerCommandExecutor.java      # /aluer 等命令注册
│   │   │   ├── bridge/                        # WebSocket 客户端/数据桥/命令执行器
│   │   │   └── listener/                      # 9 个 Bukkit 事件监听器
│   │   ├── pluginsystem/                      # ServerGuard 插件系统（10 个）
│   │   ├── platform/                          # 平台抽象层（6 个）
│   │   ├── deploy/AgentDeployService.java     # Agent 自动注入服务
│   │   ├── monitor/                           # 进程定位/资源监控（6 个）
│   │   ├── console/                           # 运维中心（4 个）
│   │   ├── service/                           # 核心服务（主调度/自动执行/命令调度/RCON）
│   │   ├── web/                               # Web 层（仪表盘/运维控制台/SSE/健康检查）
│   │   ├── command/                           # Spring Shell 命令
│   │   └── ...（其余辅助包：backup/export/metrics/notification/punishment 等）
│   └── main/resources/
│       ├── application.yml                    # 默认配置文件（326 行）
│       ├── banner.txt                         # Spring Boot 启动 Banner
│       ├── config.yml                         # Agent 插件默认配置
│       ├── plugin.yml                         # Paper 插件描述
│       ├── logback.xml                        # 日志配置
│       └── static/                            # Web 控制台静态资源（Nebula Console）
├── src/main/java/ac/grim/grimac/utils/math/   # Grim 移动模拟引擎移植（math 库）
├── src/test/java/com/aluer/                   # 测试代码（36 个文件，456 项测试）
└── frontend/                                  # React 前端源码（Nebula Console）
    ├── src/ (App.jsx + main.jsx + styles.css)
    └── package.json (React 19 + Vite 6)
```

---

## 性能特性

| 指标 | Agent Plugin 模式 | External 外部模式 |
|------|------------------|-------------------|
| 通信延迟 | < 1ms（本地 WebSocket） | 50-200ms（RCON 往返） |
| 单 Agent 事件吞吐 | 50,000+ 事件/秒 | — |
| 在线玩家支持 | 100+（单 Agent） | 无直接限制 |
| Agent 内存占用 | 几十 MB（插件部分） | 0 |
| ServerGuard 内存占用 | ~200MB（含 JVM） | ~200MB（含 JVM） |
| Agent CPU 占用 | < 5%（100 人在线） | 0 |
| ServerGuard CPU 占用 | < 10%（中等负载） | < 5%（仅 RCON 轮询） |
| 模块热开关 | 支持（application.yml 即时生效） | 支持 |
| 自动恢复 | 全自动（Kernel 自治 + SelfHealing） | 全自动（RCON 执行） |
| AI 决策延迟 | ~45 秒（DeepSeek 分析周期） | ~45 秒 |
| DDoS 防御延迟 | 即时（事件驱动） | 秒级（日志轮询） |

---

## 常见问题

<details>
<summary><b>Q1: Agent 模式和 External 模式如何选择？</b></summary>

生产环境强烈推荐 **Agent Plugin 模式**。只有 Agent 模式能提供：
- 毫秒级即时拦截（`event.setCancelled(true)` 在事件处理中直接阻止作弊行为）
- Bukkit 事件级数据精度（精确到每个 tick 的玩家移动、攻击、物品使用）
- 50,000+ 事件/秒吞吐量
- 全部 170+ 模块的完整检测能力

External 模式适合不想安装插件的临时评测或已有其他反作弊插件的场景。
</details>

<details>
<summary><b>Q2: DeepSeek AI 是必须的吗？不配置有什么影响？</b></summary>

不是必须的。未配置 `DEEPSEEK_API_KEY` 环境变量时，AI 功能自动禁用，系统退化为：

- **规则引擎模式**：所有 170+ 模块基于固定阈值检测
- **ML 模式**：隔离森林异常检测 + 时间序列预测 + 香农熵行为画像均无需 AI
- **自动防御降级**：AutoExecutor 不会自动执行（因为没有 AI 的 `autoAction` 字段），但管理员仍可手动 `/aluerblock`

配置 DeepSeek 后额外获得：AI 告警分析（根因分析 + 建议措施）、AI 健康报告、自治防御指令生成。
</details>

<details>
<summary><b>Q3: 支持哪些 Minecraft 版本和核心类型？</b></summary>

- **完整支持（Agent 模式）**：Bukkit 系全部平台——Vanilla(CraftBukkit) / Spigot / Paper / Purpur / Pufferfish / Folia / Glowstone，Minecraft **1.12.2 至今**全部版本
- **编译目标**：PaperMC 1.21.1（`api-version: '1.12'` + `folia-supported: true` + 反射兼容层实现跨版本）
- **需独立适配器**：BungeeCord / Waterfall / Velocity（代理端）、Forge / NeoForge / Fabric / Sponge（模组加载器），引擎已具备类型识别能力
- **不支持**：Bedrock 版
</details>

<details>
<summary><b>Q4: 如何验证 Agent 正常工作？</b></summary>

1. 在 Minecraft 游戏内执行 `/aluer status`——应显示 TPS、在线玩家数、Agent 连接状态
2. 查看 ServerGuard 引擎日志——应有 "Agent xxx connected" 消息
3. 查看服务器控制台——Agent 插件启动时应输出加载日志，并回显发往引擎的消息摘要
4. Web 控制台 http://localhost:8080/——可看到系统状态面板
</details>

<details>
<summary><b>Q5: 可以同时运行多个 Agent 吗？</b></summary>

可以。`AgentWebSocketServer` 支持多 Agent 同时连接（每个服务器一个 Agent）。不同服务器的 Agent 通过不同的 `agentId` 区分，`broadcastCommand()` 可向所有已连接 Agent 广播指令。适用于 BungeeCord/Velocity 群组服多子服场景。
</details>

<details>
<summary><b>Q6: 会误封正常玩家吗？怎么办？</b></summary>

分层设计尽量减少误封：

1. **Agent 端即时拦截**：仅拦截极端异常（速度 > 1.2 blocks/tick、攻击距离 > 5.0 blocks），阈值设置为物理不可能值
2. **Server 端分析**：多维度交叉验证后才会生成告警
3. **AI 自动执行**：仅当 DeepSeek 置信度 ≥ 88% 时才自动执行 ban/kick
4. **Dry Run 模式**：SelfHealing 和 HostEnforcement 默认 `dry-run: true`，仅记录不执行

可在 `application.yml` 中提高 `min-confidence` 阈值或关闭特定模块的自动执行。
</details>

<details>
<summary><b>Q7: 性能优化建议？</b></summary>

- 大型服务器（50+ 人）：提高 `check-interval-seconds` 到 10，降低 AI `analysis-interval-seconds` 到 120
- 内存受限：关闭部分不需要的 `super-evolution` 模块开关
- External 模式：减小 `log-watch-lines` 降低日志解析开销
- 关闭非必要的 `auto-execute` 子项（如 `whitelist` 在大服务器可能导致误操作）
</details>

<details>
<summary><b>Q8: 如何添加新的反作弊模块？</b></summary>

参见 `docs/DEVELOPER.md`，完整流程包括 7 个步骤：

1. 在 `anticheat/` 对应子包下创建新 Service 类（遵循双构造函数 + 内部静态结果类模式）
2. 在 `AlertType.java` 中添加告警类型枚举
3. 在 `ServerGuardConfig.java` 的 `SuperEvolutionConfig` 中添加 `boolean` 开关 + getter/setter
4. 在 `application.yml` 的 `super-evolution` 节中添加配置项
5. 编写 JUnit 测试（模拟真实数据，打靶试验）
6. 运行 `mvn clean test` 验证全量通过
7. Git commit（中文 commit message）
</details>

<details>
<summary><b>Q9: 如何在 IDE 中调试？</b></summary>

1. 在 `ServerGuardApplication.main()` 中设断点启动 External 模式
2. 或启动 Minecraft 服务器（Agent 自动加载），在 `AluerPlugin.onEnable()` 设断点远程调试
3. 测试直接运行 JUnit——无需 Spring 上下文，无需 Paper 环境，纯 Java 单元测试
4. 所有 `.idea/` 配置已纳入 Git，IntelliJ IDEA 直接开箱即用
</details>

<details>
<summary><b>Q10: 如何开发 ServerGuard 第三方插件？</b></summary>

实现 `ServerGuardPlugin` 接口并用 `@Plugin` 注解声明元数据，打包 JAR 放入 `serverguard/plugins/` 目录即可自动加载。详见 [插件系统](#插件系统) 章节与 `docs/插件系统与多平台支持.md`。
</details>

<details>
<summary><b>Q11: 项目技术规范是什么？</b></summary>

详见 `CLAUDE.md`：YOLO 模式（直接执行不等确认）、中文注释（解释 WHY 非 WHAT）、打靶试验（真实 Minecraft 数据）、零伪代码（每行可运行可验证）、频繁提交（中文 commit message）、并行 Agent 开发。
</details>

---

## 贡献者

| 贡献者 | GitHub | 角色 |
|--------|--------|------|
| Peijun Zhao | [@peijundev](https://github.com/peijundev) | 架构设计、核心开发、全部安全模块实现 |

---

## 许可证

本项目基于 **GNU General Public License v3.0（GPL-3.0）** 开源。

```
Aluer ServerGuard — AI-powered Minecraft multi-platform Server Protection System
Copyright (C) 2026 Peijun Zhao

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
```

完整许可证文本见 [LICENSE](LICENSE)。

> 说明：本项目融合了 GrimAC、NoCheatPlus 等 GPL 开源反作弊的检测算法，因此采用 GPL-3.0 许可证以保证许可证兼容。详见 `docs/开源反作弊融合.md`。

<p align="center">
  <br>
  <b>Aluer ServerGuard V5.2.0</b><br>
  <sub>AI-Powered Minecraft Multi-Platform Server Protection</sub><br>
  <sub>保护每一台 Minecraft 服务器</sub>
</p>
