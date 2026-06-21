package korlibs.root

import kotlinx.kover.gradle.plugin.KoverGradlePlugin
import korlibs.korge.gradle.util.*
import korlibs.*
import org.gradle.api.*
import org.gradle.api.tasks.testing.*

fun Project.configureKover() {
    rootProject.allprojects {
        plugins.apply(KoverGradlePlugin::class.java)
    }

    // https://repo.maven.apache.org/maven2/org/jetbrains/intellij/deps/intellij-coverage-agent/1.0.688/
    //val koverVersion = "1.0.688"
    val koverVersion = rootProject._libs["versions"]["kover"]["agent"].dynamicInvoke("get").casted<String>()

    rootProject.allprojects {
        kover {}
        // TODO See if this is required
//        tasks.withType<Test> {
//            extensions.configure<kotlinx.kover.api.KoverTaskExtension> {
//                //generateXml = false
//                //generateHtml = true
//                //coverageEngine = kotlinx.kover.api.CoverageEngine.INTELLIJ
//                excludes.add(".*BuildConfig")
//            }
//        }
    }
}
