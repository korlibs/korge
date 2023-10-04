package korlibs

import korlibs.korge.gradle.targets.*
import korlibs.modules.*
import org.gradle.api.*

object NativeTools {
    @JvmStatic
    fun configureAllCInterop(project: Project, name: String) {
        if (supportKotlinNative) {
            project.kotlin {
                for (target in allNativeTargets(project)) {
                    target.compilations["main"].cinterops {
                        it.maybeCreate(name)
                    }
                }
            }
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

    @JvmStatic
    fun groovyConfigurePublishing(project: Project, multiplatform: Boolean) {
        project.configurePublishing(multiplatform = multiplatform)
    }

    @JvmStatic
    fun groovyConfigureSigning(project: Project) {
        project.configureSigning()
    }
}
