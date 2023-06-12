package korlibs.korge.gradle.module

import korlibs.korge.gradle.util.*
import org.gradle.api.Project
import org.gradle.plugins.ide.idea.model.*

fun Project.configureIdea() {
    project.plugins.applyOnce("idea")
    //val plugin = this.plugins.apply(IdeaPlugin::class.java)
    //val idea = this.extensions.getByType<IdeaModel>()

    project.extensions.getByName<IdeaModel>("idea").apply {
        module {
            val module = it
            module.excludeDirs = module.excludeDirs.also {
                it.addAll(
                    listOf(
                        ".gradle", ".idea", "gradle/wrapper", ".idea", "build", "@old", "_template", "docs",
                        "kotlin-js-store",
                        "korge-gradle-plugin/build/srcgen2",
                        "e2e-test/.gradle", "e2e-test/.idea", "e2e-test/build",
                        "e2e-test-multi/.gradle", "e2e-test-multi/.idea", "e2e-test-multi/build",
                    )
                        .map { file(it) }
                )
            }
        }
    }
}

/*
fun Project.initIdeaExcludes() {
    allprojectsThis {
        if (project.hasBuildGradle()) {
            val plugin = this.plugins.apply(IdeaPlugin::class.java)
            val idea = this.extensions.getByType<IdeaModel>()

            idea.apply {
                module {
                    it.excludeDirs = it.excludeDirs + listOf(
                        file(".gradle"), file("src2"), file("original"), file("original-tests"), file("old-rendering"),
                        file("gradle/wrapper"), file(".idea"), file("build"), file("@old"), file("_template"),
                        file("e2e-sample"), file("e2e-test"), file("experiments"),
                    )
                }
            }
        }
    }
}
*/
