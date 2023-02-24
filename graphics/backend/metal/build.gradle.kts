plugins {
    kotlin("multiplatform")
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
        val iosMacosTest by getting {
            dependencies {
                implementation(project(":ktruth"))
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
