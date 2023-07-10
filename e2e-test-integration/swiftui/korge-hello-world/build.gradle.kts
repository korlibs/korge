import korlibs.korge.gradle.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
	alias(libs.plugins.korge.library)
}

//apply
//apply(plugin = "")
//KorgeLibraryGradlePlugin().apply(project)

/*
kotlin {
    val xcf = XCFramework("iosUniversal")
    //val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())
    val iosTargets = listOf(iosX64(), iosArm64())

    for (target in iosTargets) {
        var framework: Framework? = null

        target.binaries.framework {
            framework = target.binaries.findFramework(NativeBuildType.DEBUG)
        }
        //baseName = "shared"
        xcf.add(target.binaries.findFramework(NativeBuildType.DEBUG)!!)
    }
}
*/

korge {
	id = "com.sample.demo"

// To enable all targets at once

	//targetAll()

// To enable targets based on properties/environment variables
	//targetDefault()

// To selectively enable targets
	
	targetJvm()
	targetJs()
	targetDesktop()
	targetIos()
	targetAndroid()

	serializationJson()
}


dependencies {
    add("commonMainApi", project(":deps"))
    //add("commonMainApi", project(":korge-dragonbones"))
}

kotlin {
    iosSimulatorArm64().binaries {
        //framework {
        //    export(project(":dependency"))
        //    export("org.example:exported-library:1.0")
        //}
        //framework().apply {
            // It's possible to export different sets of dependencies to different binaries.
            //export(/"com.soywiz.korlibs.korgw:korgw")
        //}
        findFramework(NativeBuildType.DEBUG)!!.apply {
            export("com.soywiz.korlibs.korgw:korgw")
        }
    }

}
