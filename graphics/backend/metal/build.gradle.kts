plugins {
    kotlin("multiplatform")
    id("io.kotest.multiplatform") version "5.5.4"
}


kotlin {

    macosArm64()
    val nativeTarget = macosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":graphics:foundation"))
            }
        }
        val darwinTest by getting {
            dependencies {
                implementation("io.kotest:kotest-framework-engine:5.5.4")
            }
        }
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"

            }

        }
    }
}
