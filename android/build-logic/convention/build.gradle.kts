plugins {
    `kotlin-dsl`
}

group = "app.verdant.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly("com.android.tools.build:gradle:9.4.1")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "verdant.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
