plugins {
    kotlin("multiplatform")
}

kotlin {

    val arm64Target = macosArm64()
    val x64Tagert = macosX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":korgw"))
                implementation(project(":korge"))
            }
        }
    }

    listOf(arm64Target, x64Tagert)
        .forEach { target ->
            target.apply {
                binaries {
                    executable {
                        entryPoint = "main"

                    }
                }

            }
        }
}
