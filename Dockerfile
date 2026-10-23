# syntax=docker/dockerfile:1

# =============================================================================
# Aluer ServerGuard — 多阶段构建镜像
#
#   阶段 1（frontend）：Node 20 构建 React/Vite 前端，产物输出到 static/
#   阶段 2（builder） ：Maven 3.9.6 + JDK 21 打包 Spring Boot fat jar
#   阶段 3（runtime） ：eclipse-temurin JRE 21 运行
#
#   构建：  docker build -t aluer/serverguard:5.2.0 .
#   运行：  docker run -p 8080:8080 aluer/serverguard:5.2.0
# =============================================================================

# ---------- 阶段 1：前端构建 ----------
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
# 先只复制依赖清单，利用 Docker 层缓存
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
# 复制前端源码并构建；vite.config.js 的 outDir 指向 ../src/main/resources/static
COPY frontend/ ./
RUN npm run build

# ---------- 阶段 2：引擎打包 ----------
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
# 先复制 pom 并预下载依赖（PaperMC API 等来自外部仓库，首次构建较慢，有缓存后提速）
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
# 复制源码与前端构建产物，执行 clean package（跳过测试，测试在 CI 中运行）
COPY src/ ./src/
COPY --from=frontend /app/src/main/resources/static ./src/main/resources/static
RUN mvn -B -q clean package -DskipTests

# ---------- 阶段 3：运行时 ----------
FROM eclipse-temurin:21-jre
LABEL org.opencontainers.image.title="Aluer ServerGuard" \
      org.opencontainers.image.description="AI-powered Minecraft multi-platform Server Protection System" \
      org.opencontainers.image.version="5.2.0" \
      org.opencontainers.image.licenses="GPL-3.0"

WORKDIR /opt/serverguard
COPY --from=builder /app/target/serverguard.jar ./serverguard.jar

# 数据目录：
#   /opt/serverguard/reports             —— 攻击报告输出（report.dir）
#   /opt/serverguard/serverguard/plugins —— 第三方插件目录（plugin.directory）
#   引擎日志写入 /var/log/serverguard.log（logging.file.name）
RUN mkdir -p /opt/serverguard/reports /opt/serverguard/serverguard/plugins

# 可选挂载点（说明性）：
#   /minecraft —— Agent 模式：挂载 Minecraft 服务端目录，供引擎自动注入 agent jar
#   /var/log   —— 持久化引擎日志

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "serverguard.jar"]
