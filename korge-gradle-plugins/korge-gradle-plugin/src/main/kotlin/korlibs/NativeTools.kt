package korlibs

import korlibs.korge.gradle.targets.*
import korlibs.modules.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

object NativeTools {
    @JvmStatic
    fun configureAllCInterop(project: Project, name: String) {
        if (supportKotlinNative) {
            // TODO Fis the below functions
//            project.extensions.findByType(KotlinMultiplatformExtension::class.java) {
//                for (target in allNativeTargets(project)) {
//                    target.compilations["main"].cinterops {
//                        it.maybeCreate(name)
//                    }
//                }
//            }
        }
    }

    @JvmStatic
    fun configureAndroidDependency(project: Project, dep: Any) {
        project.dependencies {
            project.afterEvaluate {
                if (project.configurations.findByName("androidMainApi") != null) {
                    add("androidMainApi", dep)
                }
            }
        }
    }
}
