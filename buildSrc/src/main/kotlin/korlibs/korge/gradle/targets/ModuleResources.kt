package korlibs.korge.gradle.targets

import org.gradle.api.*
import org.gradle.api.file.*
import java.io.*

fun CopySpec.registerModulesResources(project: Project) {
    project.afterEvaluate {
        for (file in (project.rootDir.resolve("modules").listFiles()?.toList() ?: emptyList())) {
            from(File(file, "resources"))
            from(File(file, "src/commonMain/resources"))
        }
    }
}
