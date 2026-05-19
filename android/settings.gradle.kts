// Self-heal `local.properties` across environments (host macOS and dev
// container share the same workspace, so a `sdk.dir` written by one side
// breaks the other). If the file exists and `sdk.dir` doesn't resolve to a
// real directory, drop the file so AGP falls back to ANDROID_HOME /
// ANDROID_SDK_ROOT.
run {
    val lp = java.io.File(rootDir, "local.properties")
    if (lp.exists()) {
        val sdkDir = lp.readLines()
            .map { it.trim() }
            .firstOrNull { it.startsWith("sdk.dir=") }
            ?.substringAfter("sdk.dir=")
            ?.takeIf { it.isNotBlank() }
        if (sdkDir != null && !java.io.File(sdkDir).isDirectory) {
            println("[verdant] local.properties sdk.dir=$sdkDir does not exist; removing so ANDROID_HOME is used.")
            lp.delete()
        }
    }
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Verdant"
include(":app")
