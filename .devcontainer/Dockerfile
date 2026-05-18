# Base: official devcontainer JDK 21 image (Debian bookworm, non-root `vscode` user,
# git, curl, sudo, zsh). Node 22 LTS is installed here (not via a devcontainer
# Feature) so it is present regardless of how the container is launched —
# both the "Reopen in Container" path and the plain `docker compose up` path
# land in an image that already has Node.
FROM mcr.microsoft.com/devcontainers/java:21-bookworm

# Node 22 LTS via NodeSource (verdant's admin/web require Node 22+).
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

# Claude Code CLI. Installed globally so `claude` is on PATH for the
# vscode user. The host's ~/.claude is bind-mounted at runtime (see
# docker-compose.yml) so auth/memory/plugins follow the developer in.
RUN npm install -g @anthropic-ai/claude-code

# Pre-create cache dirs owned by `vscode` so named volumes inherit that
# ownership on first mount. Without this, Docker creates the mount points
# as root and the non-root user can't write to its own gradle/npm cache.
RUN install -d -o vscode -g vscode /home/vscode/.gradle /home/vscode/.npm
