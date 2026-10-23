# =============================================================================
# Aluer ServerGuard — VS Code Dev Container / GitHub Codespaces 开发镜像
#
#   与 CI（.github/workflows/ci.yml）及生产构建阶段（根目录 Dockerfile 的
#   builder 阶段）严格对齐：
#     - JDK 21（Eclipse Temurin）
#     - Maven 3.9.6
#   Node.js 20 通过 NodeSource 官方源直接安装（用于前端 React/Vite 构建）。
#
#   不依赖任何 devcontainer feature，保证 VS Code 与 JetBrains Dev Containers
#   都走同一条确定性构建路径。
#
#   本镜像仅用于「开发环境」；生产运行镜像见仓库根目录 Dockerfile。
# =============================================================================
FROM maven:3.9.6-eclipse-temurin-21

# 开发常用工具 + Node.js 20（前端 React/Vite 构建）
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
        ca-certificates curl gnupg git sudo \
 && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && rm -rf /var/lib/apt/lists/*

# 非 root 开发用户（devcontainer 会自动把其 UID 对齐到宿主机用户，避免挂载卷权限问题）
ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=1000
RUN groupadd --gid "${USER_GID}" "${USERNAME}" \
 && useradd --uid "${USER_UID}" --gid "${USER_GID}" -m -s /bin/bash "${USERNAME}" \
 && echo "${USERNAME} ALL=(root) NOPASSWD:ALL" > "/etc/sudoers.d/${USERNAME}" \
 && chmod 0440 "/etc/sudoers.d/${USERNAME}"
