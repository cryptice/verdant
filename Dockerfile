# Stage 1: Build admin UI
FROM node:22-alpine AS admin
WORKDIR /admin
COPY admin/package.json admin/package-lock.json ./
RUN npm ci
COPY admin/ ./
RUN npm run build

# Stage 2: Build backend
FROM gradle:8.12-jdk21 AS build
WORKDIR /app
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/gradle.properties ./
COPY backend/gradle/ gradle/
COPY backend/src/ src/
# Copy admin build output into Quarkus static resources
COPY --from=admin /admin/dist/ src/main/resources/META-INF/resources/
RUN gradle quarkusBuild --no-daemon -Dquarkus.profile=prod

# Stage 3: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/build/quarkus-app/ ./

EXPOSE 8080

ENV JAVA_OPTS="-Xmx256m -Xms128m"

CMD ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]
