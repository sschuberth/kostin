import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    // Apply precompiled script plugins.
    id("kotlin-multiplatform-conventions")

    alias(libs.plugins.upx)
}

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        binaries {
            executable {
                mainClass = "dev.schuberth.kostin.cli.MainKt"
            }
        }
    }

    linuxX64 {
        binaries.all {
            linkerOpts("-Wl,--as-needed")
        }
    }

    mingwX64()

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    targets.withType<KotlinNativeTarget> {
        binaries {
            executable(setOf(NativeBuildType.RELEASE)) {
                entryPoint = "dev.schuberth.kostin.cli.main"
                baseName = "kostin"
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.clikt)
                implementation(projects.lib)
            }
        }

        jvmMain {
            dependencies {
                runtimeOnly(libs.logbackClassic)
            }
        }
    }
}

upx {
    version = "5.2.0"
}

tasks.named<JavaExec>("runJvm") {
    jvmArgs = buildList {
        // See https://openjdk.org/jeps/424.
        if (javaVersion.isCompatibleWith(JavaVersion.VERSION_19)) {
            add("--enable-native-access=ALL-UNNAMED")
        }
    }

    System.getenv("TERM")?.also {
        val mode = it.substringAfter('-', "16color")
        environment("FORCE_COLOR" to mode)
    }

    System.getenv("COLORTERM")?.also {
        environment("FORCE_COLOR" to it)
    }
}
