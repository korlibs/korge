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
        iosSimulatorArm64(),
    )

    // Optionally add more targets

    val xcf = XCFramework("ios")
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "GameMain"
            xcf.add(this)
            export(libs.korlibs.image)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.korge.engine)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.korlibs.image)
        }
    }
}

korge {
	id = "com.sample.demo"

// To enable all targets at once

	//targetAll()

// To enable targets based on properties/environment variables
	//targetDefault()

// To selectively enable targets
	
	targetJvm()
	targetJs()
	targetAndroid()
}
