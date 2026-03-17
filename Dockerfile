# Stage 1: Build web UI
FROM node:22-alpine AS web
ARG VITE_GOOGLE_CLIENT_ID
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID
WORKDIR /web
COPY web/package.json web/package-lock.json ./
RUN npm ci --no-update-notifier
COPY web/ ./
RUN npm run build

# Stage 2: Build admin UI
FROM node:22-alpine AS admin
WORKDIR /admin
COPY admin/package.json admin/package-lock.json ./
RUN npm ci --no-update-notifier
COPY admin/ ./
RUN npm run build

# Stage 3: Download backend dependencies (cached unless build files change)
FROM gradle:8.12-jdk21 AS deps
WORKDIR /app
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/gradle.properties ./
COPY backend/gradle/ gradle/
RUN gradle dependencies --no-daemon -q

# Stage 4: Build backend
FROM deps AS build
COPY backend/src/ src/
COPY --from=web /web/dist/ src/main/resources/META-INF/resources/
COPY --from=admin /admin/dist/ src/main/resources/META-INF/resources/admin/
RUN gradle quarkusBuild --no-daemon -Dquarkus.profile=prod

# Stage 5: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/build/quarkus-app/ ./

EXPOSE 8080

ENV JAVA_OPTS="-Xmx256m -Xms128m"

CMD ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]
