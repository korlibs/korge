package korlibs.korge.gradle.targets.js

import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.targets.js.webpack.*
import java.io.*

fun Project.configureWebpack() {
    val wwwFolder = File(buildDir, "www")

    val browserReleaseWebpack = tasks.createThis<Copy>("browserReleaseWebpack") {
        val jsBrowserProductionWebpack: KotlinWebpack = tasks.getByName("jsBrowserProductionWebpack") as KotlinWebpack
        dependsOn(jsBrowserProductionWebpack)
        //val jsFile = browserReleaseEsbuild.outputs.files.first()
        val jsFile = jsBrowserProductionWebpack.mainOutputFile.get().asFile
        val mapFile = File(jsFile.parentFile, jsFile.name + ".map")

        from(project.tasks.getByName("jsProcessResources").outputs.files)
        from(jsFile)
        from(mapFile)
        registerModulesResources(project)
        into(wwwFolder)
    }
}
