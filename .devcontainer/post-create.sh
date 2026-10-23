#!/usr/bin/env bash
# =============================================================================
# Aluer ServerGuard — Dev Container 首次启动脚本（postCreateCommand）
#
#   由 devcontainer.json 在容器创建完成后执行（以 vscode 用户运行）。
#   负责：安装前端依赖（React + Vite）、输出环境与常用命令提示。
# =============================================================================
set -euo pipefail

echo "==> 安装前端依赖（frontend/）"
cd frontend
npm ci
cd ..

echo ""
echo "=============================================================="
echo "  Aluer ServerGuard 开发环境已就绪"
echo "=============================================================="
echo "  Java    $(java -version 2>&1 | head -1)"
echo "  Maven   $(./apache-maven-3.9.6/bin/mvn -version | head -1)"
echo "  Node    $(node -v) / npm $(npm -v)"
echo ""
echo "  常用命令："
echo "    编译            ./apache-maven-3.9.6/bin/mvn clean compile"
echo "    全量测试(456)   ./apache-maven-3.9.6/bin/mvn clean test"
echo "    打包            ./apache-maven-3.9.6/bin/mvn clean package -DskipTests"
echo "    前端开发        cd frontend && npm run dev   (http://localhost:4173)"
echo "    前端构建        cd frontend && npm run build (产物写入 src/main/resources/static)"
echo "=============================================================="
