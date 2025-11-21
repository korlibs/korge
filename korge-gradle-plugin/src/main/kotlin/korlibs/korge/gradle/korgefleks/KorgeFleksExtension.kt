package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.extensionGetOrCreate
import korlibs.korge.gradle.util.createThis
import korlibs.korge.gradle.util.executeSystemCommand
import org.gradle.api.Project
import org.gradle.api.Task


fun Project.korgeFleks(callback: KorgeFleksExtension.() -> Unit) = korgeFleks.apply(callback)
val Project.korgeFleks: KorgeFleksExtension get() = extensionGetOrCreate("korgeFleks")

open class KorgeFleksExtension(
    val project: Project,
) {
    private val assetGroup = "assets"
    // Define any configuration properties or methods for the extension here

    fun loadLDtkLevel(name: String) =
        project.tasks.createThis<Task>(name) {
            group = assetGroup
            doFirst {
                println("KorgeFleksExtension: loadLDtkLevel: $name")
//                KorgeFleksAssets.parseLDtkLevel()
            }
        }

    fun test() {
//        executeSystemCommand()
    }
}
