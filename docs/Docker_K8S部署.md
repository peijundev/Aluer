# Aluer ServerGuard — Docker / Kubernetes 部署指南

本文档介绍如何将 Aluer ServerGuard 引擎以容器化方式部署到 Docker 与 Kubernetes。

配套文件：

| 文件 | 用途 |
|------|------|
| `Dockerfile` | 多阶段构建镜像（前端 → Maven 打包 → JRE 运行） |
| `.dockerignore` | 精简构建上下文 |
| `docker-compose.yml` | External 模式一键编排（引擎 + Paper 服务端） |
| `k8s/` | Kubernetes 清单（ConfigMap / Secret / PVC / Deployment / Service） |

---

## 1. 容器化能力边界（先读）

Aluer ServerGuard 是一个**主机级安全引擎**：它内置进程扫描（`ProcessSecurityService`）、进程注入检测（`ProcessInjectionDetectionService`）、网络嗅探（`NetworkSnifferService`）、ARP 欺骗检测、`/proc` 探测、`ProcessHandle` 进程定位等依赖宿主机内核与文件系统的能力。**容器化会改变这些模块的行为**，务必先理解下面的边界：

| 能力 | 容器内表现 |
|------|-----------|
| External 模式（RCON 外部监控） | ✅ 完全可用，最适合容器化 |
| Web 控制台 / Agent WebSocket 服务 | ✅ 完全可用 |
| AI（DeepSeek）/ ML 行为分析 | ✅ 完全可用 |
| 网络层防护（WAF / DDoS / 速率限制 / 流量分析） | ✅ 可用（基于应用层流量，无需特权） |
| Agent 插件注入 | ⚠️ 需与 Minecraft 服务端共享卷（见 3.2 节） |
| 进程扫描 / 进程注入检测 / 进程定位 | ⚠️ 只能看到**容器自身**的进程，看不到宿主机/其他容器 |
| 网络嗅探（raw socket）/ ARP 欺骗检测 | ❌ 需 `privileged` + `hostNetwork`，且仅对宿主网络有效 |
| 文件完整性监控 / 主机入侵对抗 | ⚠️ 仅监控容器内挂载的文件系统 |

**结论**：
- 需要「远程监控一台物理机上的 Minecraft 服务器」→ 用 **External 模式**，容器化无额外成本。
- 需要「事件级反作弊（Agent 插件）」→ Agent 插件跑在 Minecraft 端（通过 WebSocket 连引擎），引擎容器只需能收到 Agent 连接即可，无需特权；自动注入需要共享卷。
- 需要「容器化引擎同时做宿主机安全」→ 建议直接以 systemd 部署到宿主机（见 `install.sh` / `systemd/`），容器化不是合适载体。

---

## 2. 构建镜像

```bash
docker build -t aluer/serverguard:5.2.0 .
```

镜像采用三阶段构建：

1. `node:20-alpine` 构建 React/Vite 前端，产物输出到 `src/main/resources/static/`；
2. `maven:3.9.6-eclipse-temurin-21` 执行 `mvn clean package -DskipTests`（测试在 CI 中运行）；
3. `eclipse-temurin:21-jre` 作为精简运行时。

> 首次构建会从 PaperMC / CodeMC 等外部仓库下载依赖，耗时较长；后续构建命中 Docker 层缓存显著加速。

---

## 3. Docker 运行

### 3.1 External 模式（推荐容器化）

引擎通过 RCON 连接 Minecraft 服务端，零侵入、无需共享文件系统。

```bash
docker run -d \
  --name aluer-serverguard \
  -p 8080:8080 \
  -e SERVERGUARD_MODE=external \
  -e SERVERGUARD_MINECRAFT_RCON_HOST=192.168.1.10 \
  -e SERVERGUARD_MINECRAFT_RCON_PORT=25575 \
  -e RCON_PASSWORD=your-rcon-password \
  -v serverguard-logs:/var/log \
  -v serverguard-reports:/opt/serverguard/reports \
  -v serverguard-plugins:/opt/serverguard/serverguard \
  aluer/serverguard:5.2.0
```

访问 Web 控制台：`http://localhost:8080`。

### 3.2 Agent 模式（需共享卷）

Agent 插件运行在 Minecraft 服务端内，通过 WebSocket 连接引擎（`ws://<引擎地址>:8080/agent`）。引擎需把 agent jar 注入到 Minecraft 的 `plugins/` 目录，因此**必须共享卷**。

假设 Minecraft 服务端工作目录为 `/opt/minecraft`（其插件目录为 `/opt/minecraft/plugins`）：

```bash
# 1. 启动引擎，挂载 Minecraft 工作目录，配置 working-dir
docker run -d \
  --name aluer-serverguard \
  -p 8080:8080 \
  -e SERVERGUARD_MODE=plugin \
  -e SERVERGUARD_WORKING_DIR=/minecraft \
  -e SERVERGUARD_MINECRAFT_AUTO_INJECT=true \
  -v /opt/minecraft:/minecraft \
  -v serverguard-logs:/var/log \
  aluer/serverguard:5.2.0

# 2. Minecraft 端：Agent 插件配置连接引擎
mkdir -p /opt/minecraft/plugins/AluerServerGuard
cat > /opt/minecraft/plugins/AluerServerGuard/config.yml << 'EOF'
server-url: ws://<引擎地址>:8080/agent
EOF
```

引擎启动后会依据 `working-dir` 自动把 `serverguard-agent.jar` 注入到 `/opt/minecraft/plugins/`，并周期补注入（防止误删）。

> 说明：容器内 `MinecraftProcessDetector` 无法看到宿主机进程，因此 Agent 模式必须显式配置 `SERVERGUARD_WORKING_DIR`（不要留空依赖自动探测）。

---

## 4. Docker Compose

仓库根目录的 `docker-compose.yml` 提供了一个 External 模式的一体化示例（引擎 + Paper 1.21.1 服务端）：

```bash
# 修改 RCON_PASSWORD 后启动
docker compose up -d --build
docker compose logs -f serverguard
```

启动后访问 `http://localhost:8080`。

**Agent 模式 Compose 片段**（共享卷注入）：

```yaml
services:
  serverguard:
    image: aluer/serverguard:5.2.0
    ports: ["8080:8080"]
    environment:
      SERVERGUARD_MODE: plugin
      SERVERGUARD_WORKING_DIR: /minecraft
    volumes:
      - minecraft-data:/minecraft      # 与 minecraft 服务共享同一卷
  minecraft:
    image: itzg/minecraft-server:latest
    environment:
      EULA: "TRUE"
      TYPE: PAPER
      VERSION: "1.21.1"
    volumes:
      - minecraft-data:/data           # itzg 镜像插件目录即 /data/plugins
volumes:
  minecraft-data:
```

> 注意：上面两个容器对同一个卷的挂载路径不同（引擎挂到 `/minecraft`，服务端挂到 `/data`），引擎写入 `/minecraft/plugins/` 时对应的正是服务端的 `/data/plugins/`，实现插件注入。

---

## 5. Kubernetes 部署

以下清单位于 `k8s/`，采用 `aluer` 命名空间，External 模式（RCON 连接外部/集群内 Minecraft）。

### 5.1 准备命名空间

```bash
kubectl create namespace aluer
```

### 5.2 配置 ConfigMap 与 Secret

```bash
# 编辑 k8s/configmap.yaml 中的 RCON host/port，指向你的 Minecraft 服务端
vim k8s/configmap.yaml

# 编辑 k8s/secret.yaml，填入真实密钥（base64）
# 生成：echo -n 'your-password' | base64
vim k8s/secret.yaml

kubectl apply -f k8s/configmap.yaml -f k8s/secret.yaml
```

### 5.3 持久化与部署

```bash
kubectl apply -f k8s/pvc.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

### 5.4 验证

```bash
# 查看 Pod 状态
kubectl -n aluer get pods

# 查看日志（含启动 Banner 与 Agent 连接日志）
kubectl -n aluer logs -f deployment/serverguard

# 端口转发本地访问 Web 控制台
kubectl -n aluer port-forward svc/serverguard 8080:8080
# 浏览器打开 http://localhost:8080

# 健康检查
kubectl -n aluer exec deployment/serverguard -- \
  curl -sf http://localhost:8080/api/health/live
```

若 Service 类型为 `LoadBalancer`，通过 `kubectl -n aluer get svc serverguard` 获取外部 IP。

### 5.5 Agent 模式（K8S）

在 K8S 中推荐**手动部署 agent jar**而非自动注入（跨节点共享卷复杂）：

1. 将 `serverguard-agent.jar` 手动放入 Minecraft 服务端的 `plugins/` 目录；
2. Minecraft 端 Agent 的 `config.yml` 指向引擎 Service：`server-url: ws://serverguard.aluer.svc.cluster.local:8080/agent`；
3. 引擎容器无需挂载 Minecraft 卷，`SERVERGUARD_MODE` 设为 `plugin`（或不设，Agent 连接是事件驱动的）。

---

## 6. 健康检查端点

引擎提供以下健康检查 HTTP 端点（由 `DashboardController` 暴露）：

| 端点 | 用途 | HTTP 行为 |
|------|------|----------|
| `/api/health/live` | 存活探测 | 始终 200，返回 `{"status":"UP"}` |
| `/api/health/ready` | 就绪探测 | 始终 200，返回 `{"status":"READY"/"NOT_READY","rcon":...}` |
| `/api/health` | 完整健康报告 | 返回组件状态、系统信息、内存等 |

**重要**：`/api/health/ready` 的业务状态（`READY`/`NOT_READY`）体现在**响应体**而非 HTTP 状态码，该端点始终返回 200。因此 K8S 的 `readinessProbe` 只能表达「进程可服务」，RCON 连通性需另看响应体 `status` 字段或告警系统。

K8S 探针已在 `k8s/deployment.yaml` 中配置（liveness → `/api/health/live`，readiness → `/api/health/ready`）。

---

## 7. 环境变量参考

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SERVERGUARD_MODE` | 运行模式 `external` / `plugin` | `external` |
| `SERVERGUARD_WORKING_DIR` | 服务端工作目录（Agent 注入用，容器内需显式设置） | (空) |
| `SERVERGUARD_MINECRAFT_RCON_HOST` | RCON 主机（覆盖 application.yml 默认 localhost） | `localhost` |
| `SERVERGUARD_MINECRAFT_RCON_PORT` | RCON 端口 | `25575` |
| `RCON_PASSWORD` | RCON 密码 | (空) |
| `SERVERGUARD_MINECRAFT_AUTO_INJECT` | Agent 自动注入开关 | `true` |
| `DEEPSEEK_API_KEY` | DeepSeek 密钥（空则 AI 禁用） | (空) |
| `DEEPSEEK_BASE_URL` | DeepSeek API 地址 | `https://api.deepseek.com` |
| `DEEPSEEK_MODEL` | DeepSeek 模型 | `deepseek-chat` |
| `SERVERGUARD_PLUGIN_DIR` | 第三方插件目录 | `serverguard/plugins` |
| `SERVERGUARD_LOG_PATH` | 日志路径（留空自动推导） | (空) |
| `ALUER_ALERT_SMTP_USERNAME` / `_PASSWORD` | 邮件告警 SMTP 凭据 | (空) |
| `ALUER_ALERT_EMAIL_PRIMARY` / `_SECONDARY` | 告警收件邮箱 | (空) |

> 完整配置项见 `src/main/resources/application.yml`（326 行）。Spring Boot 支持 relaxed binding，任何 `serverguard.*` 配置均可通过 `SERVERGUARD_` 前缀的大写下划线环境变量覆盖。

---

## 8. 常见问题

**Q：容器里为什么看不到宿主机进程？**
引擎的进程扫描/注入检测依赖 `/proc` 与 `ProcessHandle`，容器默认只有自己的 PID 命名空间。需要 `hostPID: true` + `privileged: true` 才能看到宿主机进程，但即便这样也不等于与宿主机 systemd 部署等价。主机级安全建议直接部署到宿主机。

**Q：Agent 模式在容器里自动注入失败？**
确认已配置 `SERVERGUARD_WORKING_DIR` 并挂载了 Minecraft 的 `plugins/` 目录。容器内无法自动探测宿主机进程，不能留空依赖探测。

**Q：`/api/health/ready` 返回 NOT_READY 怎么办？**
表示 RCON 未连通或未启用。检查 `SERVERGUARD_MINECRAFT_RCON_HOST/PORT` 与 `RCON_PASSWORD` 是否与 Minecraft 端一致，网络是否互通。External 模式下 RCON 连不上引擎仍可运行，但就绪状态为 `NOT_READY`。

**Q：如何升级镜像？**
```bash
docker build -t aluer/serverguard:5.2.0 .
kubectl -n aluer set image deployment/serverguard serverguard=aluer/serverguard:5.2.0
```

**Q：多副本可以吗？**
不建议。引擎依赖本机状态（进程表、文件监控、插件目录、单机自愈决策），多副本会互相干扰。需要高可用时，建议按「一个 Minecraft 集群一个引擎」的方式拆分为多个独立单副本实例，而非水平扩展单实例。
