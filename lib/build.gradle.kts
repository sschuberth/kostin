plugins {
    // Apply precompiled script plugins.
    id("kotlin-jvm-conventions")

    // Apply third-party plugins.
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.openApiGenerator)
}

dependencies {
    api(ktorLibs.client.core)
    api(ktorLibs.http)
    api(ktorLibs.utils)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)

    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
}

openApiGenerate {
    inputSpec.set("$projectDir/swagger.json")
    skipValidateSpec.set(true)

    outputDir.set(layout.buildDirectory.dir("generated/openapi"))

    generatorName.set("kotlin")
    library.set("multiplatform")

    configOptions.set(
        mapOf(
            "omitGradleWrapper" to "true",
            "packageName" to "dev.schuberth.kostin.client",
            "dateLibrary" to "kotlinx-datetime"
        )
    )

    globalProperties.set(
        mapOf(
            "apis" to "",
            "models" to "",
            "apiDocs" to "false",
            "modelDocs" to "false",
            "apiTests" to "false",
            "modelTests" to "false",
            "supportingFiles" to ""
        )
    )
}

sourceSets {
    main {
        kotlin {
            srcDir(layout.buildDirectory.dir("generated/openapi/src/commonMain/kotlin"))
        }
    }
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate)
}

afterEvaluate {
    tasks.named("explodeCodeSourceMain").configure {
        mustRunAfter(tasks.openApiGenerate)
    }
}
