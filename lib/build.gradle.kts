plugins {
    // Apply precompiled script plugins.
    id("kotlin-multiplatform-conventions")

    // Apply third-party plugins.
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.openApiGenerator)
}

kotlin {
    linuxX64()
    mingwX64()

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(
                tasks.openApiGenerate.map { layout.buildDirectory.dir("generated/openapi/src/commonMain/kotlin") }
            )

            dependencies {
                api(ktorLibs.client.core)
                api(ktorLibs.http)
                api(ktorLibs.utils)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)

                implementation(ktorLibs.client.contentNegotiation)
                implementation(ktorLibs.serialization.kotlinx.json)
                implementation(libs.cryptoKotlin.core)
                implementation(libs.cryptoKotlin.provider.optimal)
            }
        }

        jvmMain {
            dependencies {
                implementation(ktorLibs.client.okhttp)
                implementation(libs.signum.indispensable)
                implementation(libs.signum.supreme)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.runner.junit5)
            }
        }

        linuxMain {
            dependencies {
                implementation(ktorLibs.client.curl)
            }
        }

        mingwMain {
            dependencies {
                implementation(ktorLibs.client.curl)
            }
        }
    }
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

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}
