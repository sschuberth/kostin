import io.github.hfhbd.kfx.swagger.Swagger

plugins {
    // Apply core plugins.
    `java-library`

    // Apply precompiled script plugins.
    id("kotlin-jvm-conventions")

    // Apply third-party plugins.
    alias(libs.plugins.kfx)
    alias(libs.plugins.kotlinSerialization)
}

kfx {
    register<Swagger>("PlenticoreApi") {
        files.from("swagger.json")

        packageName = "dev.schuberth.kostin.client"

        dependencies {
            compiler(kotlinClasses())
            compiler(kotlinxJson())
            compiler(ktorClient())
        }

        usingKotlinSourceSet(kotlin.sourceSets.main)
    }
}

dependencies {
    api(ktorLibs.client.core)
    api(libs.kotlinx.serialization.core)

    implementation(ktorLibs.http)
    implementation(ktorLibs.utils)
}
