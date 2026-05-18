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

RUN apt-get update \
 && apt-get install -y --no-install-recommends unzip \
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

# Pre-create cache dirs owned by `vscode` so named volumes inherit that
# ownership on first mount. Without this, Docker creates the mount points
# as root and the non-root user can't write to its own gradle/npm cache.
# .android holds adb keys and avd settings.
RUN install -d -o vscode -g vscode /home/vscode/.gradle /home/vscode/.npm /home/vscode/.android
