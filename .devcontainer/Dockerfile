# =============================================================================
# Aluer ServerGuard — VS Code Dev Container / GitHub Codespaces 开发镜像
#
#   与 CI（.github/workflows/ci.yml）及生产构建阶段（根目录 Dockerfile 的
#   builder 阶段）严格对齐：
#     - JDK 21（Eclipse Temurin）
#     - Maven 3.9.6
#   Node.js 20（前端构建）由 devcontainer.json 中的 node feature 注入。
#
#   本镜像仅用于「开发环境」；生产运行镜像见仓库根目录 Dockerfile。
# =============================================================================
FROM maven:3.9.6-eclipse-temurin-21
