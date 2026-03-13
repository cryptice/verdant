plugins {
    kotlin("jvm") version "2.1.10"
    id("io.quarkus")
}

repositories {
    mavenCentral()
}

buildscript {
    dependencies {
        classpath("org.yaml:snakeyaml:2.3")
    }
}

@Suppress("UNCHECKED_CAST")
val envYaml: Map<String, Any> = run {
    val envFile = rootProject.file("../.env.yaml")
    if (envFile.exists()) {
        val yaml = org.yaml.snakeyaml.Yaml()
        yaml.load<Map<String, Any>>(envFile.readText()) ?: emptyMap()
    } else emptyMap()
}

@Suppress("UNCHECKED_CAST")
fun envGet(vararg keys: String): String {
    var current: Any? = envYaml
    for (key in keys) {
        current = (current as? Map<String, Any>)?.get(key)
    }
    return current?.toString() ?: ""
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-elytron-security-common")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        javaParameters.set(true)
    }
}

tasks.withType<JavaExec> {
    val geminiKey = envGet("backend", "gemini-api-key")
    if (geminiKey.isNotBlank()) systemProperty("verdant.gemini.api-key", geminiKey)

    val dbUser = envGet("backend", "prod", "db-username")
    if (dbUser.isNotBlank()) systemProperty("quarkus.datasource.username", dbUser)
    val dbPass = envGet("backend", "prod", "db-password")
    if (dbPass.isNotBlank()) systemProperty("quarkus.datasource.password", dbPass)
    val dbUrl = envGet("backend", "prod", "db-url")
    if (dbUrl.isNotBlank()) systemProperty("quarkus.datasource.jdbc.url", dbUrl)
}

// Pass config to quarkusDev via JVM args
tasks.named<io.quarkus.gradle.tasks.QuarkusDev>("quarkusDev") {
    val geminiKey = envGet("backend", "gemini-api-key")
    if (geminiKey.isNotBlank()) {
        jvmArgs = jvmArgs + listOf("-Dverdant.gemini.api-key=$geminiKey")
    }
}
