# Stage 1: Build admin UI
FROM node:22-alpine AS admin
WORKDIR /admin
COPY admin/package.json admin/package-lock.json ./
RUN npm ci --no-update-notifier
COPY admin/ ./
RUN npm run build

# Stage 2: Download backend dependencies (cached unless build files change)
FROM gradle:8.12-jdk21 AS deps
WORKDIR /app
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/gradle.properties ./
COPY backend/gradle/ gradle/
RUN gradle dependencies --no-daemon -q

# Stage 3: Build backend
FROM deps AS build
COPY backend/src/ src/
COPY --from=admin /admin/dist/ src/main/resources/META-INF/resources/
RUN gradle quarkusBuild --no-daemon -Dquarkus.profile=prod

# Stage 4: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/build/quarkus-app/ ./

EXPOSE 8080

ENV JAVA_OPTS="-Xmx256m -Xms128m"

CMD ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]
