package korlibs.root

//import kotlinx.kover.api.*
import org.gradle.api.*

fun Project.configureKover() {
    /*
    rootProject.allprojectsThis {
        plugins.apply(kotlinx.kover.KoverPlugin::class.java)
    }

    rootProject.koverMerged {
        it.enable()
    }

    // https://repo.maven.apache.org/maven2/org/jetbrains/intellij/deps/intellij-coverage-agent/1.0.688/
    //val koverVersion = "1.0.688"
    val koverVersion = rootProject._libs["versions"]["kover"]["agent"].dynamicInvoke("get").casted<String>()

    rootProject.allprojectsThis {
        kover {
            it.engine.set(kotlinx.kover.api.IntellijEngine(koverVersion))
        }
        extensions.getByType(kotlinx.kover.api.KoverProjectConfig::class.java).apply {
            engine.set(kotlinx.kover.api.IntellijEngine(koverVersion))
        }
        tasks.withType<Test> {
            extensions.configure<kotlinx.kover.api.KoverTaskExtension> {
                //generateXml = false
                //generateHtml = true
                //coverageEngine = kotlinx.kover.api.CoverageEngine.INTELLIJ
                excludes.add(".*BuildConfig")
            }
        }
    }
    */
}

//fun org.gradle.api.Project.`koverMerged`(configure: Action<KoverMergedConfig>): Unit = (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("koverMerged", configure)
//fun org.gradle.api.Project.`kover`(configure: Action<kotlinx.kover.api.KoverProjectConfig>): Unit = (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("kover", configure)
//fun TaskContainer.`dokkaHtml`(configure: org.jetbrains.dokka.gradle.DokkaTask.() -> Unit) {
//    configure(named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml").get())
//}
