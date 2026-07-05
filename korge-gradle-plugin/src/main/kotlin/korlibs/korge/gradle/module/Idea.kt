package korlibs.korge.gradle.module

import korlibs.korge.gradle.util.applyOnce
import korlibs.korge.gradle.util.getByName
import org.gradle.api.Project
import org.gradle.plugins.ide.idea.model.IdeaModel

fun Project.configureIdea() {
    project.plugins.applyOnce("idea")

    project.extensions.getByName<IdeaModel>("idea").apply {
        module {
            excludeDirs = excludeDirs.also {
                it.addAll(
                    listOf(
                        ".gradle", ".idea", "gradle/wrapper", ".idea", "build", "@old", "_template", "docs",
                        "kotlin-js-store", "archive",
                        "e2e-test/.gradle", "e2e-test/.idea", "e2e-test/build",
                        "e2e-test-multi/.gradle", "e2e-test-multi/.idea", "e2e-test-multi/build",
                    )
                        .map { file(it) }
                )
            }
        }
    }
}
