# Stage 1: Build web UI
FROM node:22-alpine AS web
WORKDIR /web
COPY web/package.json web/package-lock.json ./
RUN npm ci --no-update-notifier
COPY web/ ./
ARG VITE_GOOGLE_CLIENT_ID
RUN npm run build

# Stage 2: Build admin UI
FROM node:22-alpine AS admin
WORKDIR /admin
COPY admin/package.json admin/package-lock.json ./
RUN npm ci --no-update-notifier
COPY admin/ ./
RUN npm run build

# Stage 2b: Build market UI
FROM node:22-alpine AS market
WORKDIR /market
COPY market/package.json market/package-lock.json ./
RUN npm ci --no-update-notifier
COPY market/ ./
ARG VITE_GOOGLE_CLIENT_ID
RUN npm run build

# Stage 3: Download backend dependencies (cached unless build files change)
FROM gradle:8.12-jdk21 AS deps
WORKDIR /app
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/gradle.properties ./
COPY backend/gradle/ gradle/
RUN gradle dependencies --no-daemon -q

# Stage 4: Run backend tests (reuses cached dependencies)
FROM deps AS test
COPY backend/src/ src/
RUN gradle test --no-daemon

# Stage 5: Build backend
FROM test AS build
COPY --from=web /web/dist/ src/main/resources/META-INF/resources/
COPY --from=admin /admin/dist/ src/main/resources/META-INF/resources/admin/
COPY --from=market /market/dist/ src/main/resources/META-INF/resources/market/
RUN gradle quarkusBuild --no-daemon -Dquarkus.profile=prod && rm -rf /root/.kotlin /tmp/kotlin-daemon*

# Stage 6: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/build/quarkus-app/ ./

EXPOSE 8080

ENV JAVA_OPTS="-Xmx256m -Xms128m"

CMD ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]
