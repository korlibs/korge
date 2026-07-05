package korlibs.korge.gradle.targets.js

import java.io.File
import korlibs.korge.gradle.targets.registerModulesResources
import korlibs.korge.gradle.util.createThis
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

fun Project.configureWebpack() {
    val wwwFolder = File(buildDir, "www")

    val browserReleaseWebpack = tasks.createThis<Copy>("browserReleaseWebpack") {
        val jsBrowserProductionWebpack: KotlinWebpack = tasks.getByName("jsBrowserProductionWebpack") as KotlinWebpack
        dependsOn(jsBrowserProductionWebpack)
        val jsFile = jsBrowserProductionWebpack.mainOutputFile.get().asFile
        val mapFile = File(jsFile.parentFile, jsFile.name + ".map")

        from(project.tasks.getByName("jsProcessResources").outputs.files)
        from(jsFile)
        from(mapFile)
        registerModulesResources(project)
        into(wwwFolder)
    }
}
