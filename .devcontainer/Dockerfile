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

# Pre-create cache dirs owned by `vscode` so named volumes inherit that
# ownership on first mount. Without this, Docker creates the mount points
# as root and the non-root user can't write to its own gradle/npm cache.
# .npm-global is the user-owned global-install prefix for `npm i -g` (see
# the claude-code install below); created here for the same reason.
# .android holds adb keys and avd settings.
RUN install -d -o vscode -g vscode /home/vscode/.gradle /home/vscode/.npm /home/vscode/.npm-global /home/vscode/.android /home/vscode/.config /home/vscode/.config/gcloud

# Claude Code CLI. Installed under a user-owned npm prefix so the vscode
# user can run `claude update` (or `npm i -g @anthropic-ai/claude-code`)
# without sudo — a root-owned /usr/lib/node_modules install would block
# both. The host's ~/.claude is bind-mounted at runtime (see
# docker-compose.yml) so auth/memory/plugins follow the developer in.
ENV PATH=/home/vscode/.npm-global/bin:$PATH
USER vscode
RUN npm config set prefix /home/vscode/.npm-global \
 && npm install -g @anthropic-ai/claude-code
USER root

# Android SDK — enables `./gradlew :app:compileDebugKotlin`, unit tests,
# lint, and APK assembly inside the container. Image baseline matches the
# project: compileSdk=35, AGP 8.13.x, build-tools 35.0.0.
#
# Interactive on-device debugging from the container is limited: Docker
# Desktop on macOS does not pass through USB or expose KVM, so emulators
# can't run here. To debug, run an emulator (or device) on the host and
# `adb connect host.docker.internal:5555` from inside the container after
# enabling `adb tcpip 5555` on the host.
ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Multi-arch support for x86_64 binaries when the image runs on Apple Silicon.
# Google ships only x86_64 Linux build-tools (aapt2, aidl, etc.) — on
# arm64 hosts the JVM toolchain runs natively, but those few native helpers
# need Rosetta translation, which in turn needs the x86_64 dynamic linker
# present in the rootfs. Installing libc6:amd64 + libstdc++6:amd64 provides
# /lib64/ld-linux-x86-64.so.2 and the runtime libs Rosetta requires. No-op
# on amd64 hosts because the architecture is already enabled by default.
RUN apt-get update \
 && apt-get install -y --no-install-recommends unzip \
 && dpkg --add-architecture amd64 \
 && apt-get update \
 && apt-get install -y --no-install-recommends \
      libc6:amd64 libstdc++6:amd64 zlib1g:amd64 \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/* \
 && mkdir -p $ANDROID_HOME/cmdline-tools \
 && curl -fsSL -o /tmp/cmdline-tools.zip \
      https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
 && unzip -q /tmp/cmdline-tools.zip -d $ANDROID_HOME/cmdline-tools \
 && mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest \
 && rm /tmp/cmdline-tools.zip \
 && yes | sdkmanager --licenses > /dev/null \
 && sdkmanager --install \
      "platforms;android-35" \
      "build-tools;35.0.0" \
      "platform-tools" \
 && chown -R vscode:vscode $ANDROID_HOME

# Google Cloud SDK + cloud-sql-proxy — match the production / staging
# tooling documented in README.md (gcloud secrets, Artifact Registry auth,
# Cloud SQL proxy for connecting to verdant-planner-staging). The host's
# ~/.config/gcloud is bind-mounted at runtime (see docker-compose.yml) so
# `gcloud auth list` works without re-auth inside the container.
RUN curl -fsSL https://packages.cloud.google.com/apt/doc/apt-key.gpg \
      | gpg --dearmor -o /usr/share/keyrings/cloud.google.gpg \
 && echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" \
      > /etc/apt/sources.list.d/google-cloud-sdk.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends google-cloud-cli \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/* \
 && arch="$(dpkg --print-architecture)" \
 && curl -fsSL -o /usr/local/bin/cloud-sql-proxy \
      "https://storage.googleapis.com/cloud-sql-connectors/cloud-sql-proxy/v2.14.3/cloud-sql-proxy.linux.${arch}" \
 && chmod +x /usr/local/bin/cloud-sql-proxy
