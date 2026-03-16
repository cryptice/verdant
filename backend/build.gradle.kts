import java.text.SimpleDateFormat
import java.util.Date

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
    implementation("com.google.cloud:google-cloud-storage:2.43.1")
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
    val gcsKey = envGet("backend", "gcs-service-account-key")
    if (gcsKey.isNotBlank()) systemProperty("verdant.gcs.service-account-key", gcsKey)

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
    val gcsKey = envGet("backend", "gcs-service-account-key")
    if (gcsKey.isNotBlank()) {
        jvmArgs = jvmArgs + listOf("-Dverdant.gcs.service-account-key=$gcsKey")
    }
}

// ── Database backup/restore tasks ──

data class DbConnection(
    val dbname: String,
    val username: String,
    val password: String,
    val containerId: String?, // non-null = use docker exec; null = use local pg_dump/psql
    val host: String?,        // used when containerId is null
    val port: String?,        // used when containerId is null
)

fun resolveDbConnection(): DbConnection {
    // Allow explicit override via -PdbUrl, -PdbUser, -PdbPass
    if (project.hasProperty("dbUrl")) {
        val url = project.property("dbUrl") as String
        val regex = Regex("""jdbc:postgresql://([^:]+):(\d+)/(.+)""")
        val match = regex.find(url) ?: error("Cannot parse dbUrl: $url")
        return DbConnection(
            dbname = match.groupValues[3],
            username = (project.findProperty("dbUser") as? String ?: "verdant"),
            password = (project.findProperty("dbPass") as? String ?: "verdant"),
            containerId = null,
            host = match.groupValues[1],
            port = match.groupValues[2],
        )
    }

    // Try to find Quarkus Dev Services postgres container
    try {
        val process = ProcessBuilder("docker", "ps", "--filter", "ancestor=postgres:17", "--format", "{{.ID}}")
            .redirectErrorStream(true).start()
        val containerId = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()

        if (containerId.isNotBlank()) {
            return DbConnection(
                dbname = "verdant",
                username = "quarkus",
                password = "quarkus",
                containerId = containerId.lines().first(),
                host = null,
                port = null,
            )
        }
    } catch (_: Exception) {
        // Docker not available, skip
    }

    // Fall back to .env.yaml prod config
    val prodUrl = envGet("backend", "prod", "db-url")
    val prodUser = envGet("backend", "prod", "db-username")
    val prodPass = envGet("backend", "prod", "db-password")
    if (prodUrl.isNotBlank()) {
        val regex = Regex("""jdbc:postgresql://([^:]+):(\d+)/(.+)""")
        val match = regex.find(prodUrl) ?: error("Cannot parse db-url: $prodUrl")
        return DbConnection(
            dbname = match.groupValues[3],
            username = prodUser,
            password = prodPass,
            containerId = null,
            host = match.groupValues[1],
            port = match.groupValues[2],
        )
    }
    error("No database found. Start quarkusDev or configure backend.prod.db-url in .env.yaml")
}

// Resolve full paths for tools that may not be on the Gradle daemon's PATH
fun findExecutable(name: String): String {
    val searchPaths = listOf("/usr/local/bin", "/opt/homebrew/bin", "/opt/homebrew/opt/libpq/bin", "/usr/bin")
    for (dir in searchPaths) {
        val f = File(dir, name)
        if (f.exists() && f.canExecute()) return f.absolutePath
    }
    return name // fall back to bare name
}

fun Project.pgExec(db: DbConnection, tool: String, args: List<String>, output: java.io.OutputStream? = null) {
    if (db.containerId != null) {
        exec {
            environment("PGPASSWORD", db.password)
            commandLine(listOf(findExecutable("docker"), "exec", "-e", "PGPASSWORD=${db.password}", db.containerId,
                tool, "-U", db.username) + args)
            if (output != null) standardOutput = output
        }
    } else {
        exec {
            environment("PGPASSWORD", db.password)
            commandLine(listOf(findExecutable(tool), "-h", db.host!!, "-p", db.port!!, "-U", db.username) + args)
            if (output != null) standardOutput = output
        }
    }
}

fun Project.pgDump(db: DbConnection, output: java.io.OutputStream) {
    pgExec(db, "pg_dump", listOf("--no-owner", "--no-acl", db.dbname), output)
}

fun Project.psql(db: DbConnection, database: String, vararg args: String) {
    pgExec(db, "psql", listOf("-d", database) + args.toList())
}

val backupDir = rootProject.file("db-backups")

tasks.register("dbBackup") {
    group = "database"
    description = "Back up the current database to a timestamped SQL file"
    doLast {
        val db = resolveDbConnection()
        backupDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val backupFile = File(backupDir, "verdant_${timestamp}.sql")

        println("Backing up ${db.dbname} to ${backupFile.name}...")
        pgDump(db, backupFile.outputStream())
        println("Done: ${backupFile.absolutePath} (${backupFile.length() / 1024} KB)")
    }
}

tasks.register("dbRestore") {
    group = "database"
    description = "Restore the database from the latest backup (or specify -PbackupFile=path)"
    doLast {
        val db = resolveDbConnection()
        val backupFile: File = if (project.hasProperty("backupFile")) {
            File(project.property("backupFile") as String)
        } else {
            val files = backupDir.listFiles { f -> f.extension == "sql" }
                ?.sortedByDescending { it.name }
                ?: error("No backups found in ${backupDir.absolutePath}")
            files.firstOrNull() ?: error("No .sql backups found in ${backupDir.absolutePath}")
        }
        require(backupFile.exists()) { "Backup file not found: ${backupFile.absolutePath}" }

        println("Restoring ${db.dbname} from ${backupFile.name}...")

        // Terminate connections, drop, recreate
        psql(db, "postgres", "-c",
            "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '${db.dbname}' AND pid <> pg_backend_pid()")
        psql(db, "postgres", "-c", "DROP DATABASE IF EXISTS ${db.dbname}")
        psql(db, "postgres", "-c", "CREATE DATABASE ${db.dbname}")

        // Restore from backup
        if (db.containerId != null) {
            // Pipe file content into docker exec psql via stdin
            exec {
                commandLine(findExecutable("docker"), "exec", "-i", "-e", "PGPASSWORD=${db.password}", db.containerId,
                    "psql", "-U", db.username, "-d", db.dbname)
                standardInput = backupFile.inputStream()
            }
        } else {
            psql(db, db.dbname, "-f", backupFile.absolutePath)
        }
        println("Done: restored from ${backupFile.name}")
    }
}
