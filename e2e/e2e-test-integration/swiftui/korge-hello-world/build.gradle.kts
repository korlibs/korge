import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.korge.library)
	alias(libs.plugins.kotlin.serialization)
}

kotlin {
    val iosTargets = listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    )
    // Optionally add more targets

    val xcf = XCFramework("ios")
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "GameMain"
            xcf.add(this)
            export(libs.korlibs.image)
            isStatic = true
            linkerOpts("-lc++")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.korge.engine)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.korlibs.image)
        }

        iosMain.dependencies {
            implementation(libs.korge.engine)
            api(libs.korlibs.image)
        }

        iosX64Main.dependencies {
            implementation(libs.korlibs.image)
        }

        iosArm64Main.dependencies {
            implementation(libs.korlibs.image)
        }

        iosSimulatorArm64Main.dependencies {
            implementation(libs.korlibs.image)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.korge.engine)
        }

        /*
                val commonMain by getting {
                    dependencies {
                        implementation(libs.korge.engine)
                        implementation(libs.kotlinx.serialization.json)
                        implementation(libs.korlibs.image)
                    }
                }

                val iosMain by creating {
                    dependsOn(commonMain)
                    dependencies {
                        api(libs.korlibs.image)
                    }
                }

                val iosX64Main by getting {
                    dependsOn(iosMain)
                }

                val iosArm64Main by getting {
                    dependsOn(iosMain)
                }

                val iosSimulatorArm64Main by getting {
                    dependsOn(iosMain)
                }

                val commonTest by getting {
                    dependencies {
                        implementation(kotlin("test"))
                        implementation(libs.korge.engine)
                    }
                }
        */
    }
}

korge {
	id = "com.sample.demo"

// To enable all targets at once

	//targetAll()

// To enable targets based on properties/environment variables
	//targetDefault()

// To selectively enable targets
	
//	targetJvm()
//	targetJs()
//	targetAndroid()
}
