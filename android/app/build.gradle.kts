plugins {
    id("verdant.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

buildscript {
    dependencies {
        classpath("org.yaml:snakeyaml:2.3")
    }
}

@Suppress("UNCHECKED_CAST")
val envYaml: Map<String, Any> = run {
    val envFile = rootProject.file(".env.yaml")
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

android {
    namespace = "app.verdant.android"
    // compileSdk, minSdk, targetSdk, source/target compat, jvmTarget — all
    // come from the verdant.android.application convention plugin.

    signingConfigs {
        create("release") {
            val keystorePath = envGet("android", "keystore-path").ifBlank { "../secrets/verdant-release.jks" }
            storeFile = file(keystorePath)
            storePassword = envGet("android", "keystore-password")
            keyAlias = "verdant"
            keyPassword = envGet("android", "key-password")
        }
    }

    defaultConfig {
        applicationId = "app.verdant.android"
        versionCode = 1
        versionName = "1.0"

        // Production-by-default. Erik can flip to the local emulator backend
        // at runtime from the Account screen toggle.
        val apiBaseUrl = envGet("android", "api-base-url").ifBlank { "https://verdantplanner.com/" }
        val localApiBaseUrl = envGet("android", "local-api-base-url").ifBlank { "http://10.0.2.2:8081/" }
        val webClientId = envGet("android", "google-web-client-id")
        val mapsApiKey = envGet("android", "maps-api-key")

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "LOCAL_API_BASE_URL", "\"$localApiBaseUrl\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$webClientId\"")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            val prodUrl = envGet("android", "prod-api-base-url").ifBlank { "https://verdantplanner.com/" }
            buildConfigField("String", "API_BASE_URL", "\"$prodUrl\"")
            ndk.debugSymbolLevel = "FULL"
        }
    }

}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.54")
    ksp("com.google.dagger:hilt-android-compiler:2.54")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Google Sign-In (Credential Manager)
    implementation("androidx.credentials:credentials:1.5.0-rc01")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0-rc01")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // DataStore for token storage
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:6.2.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
}
