package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.extensionGetOrCreate
import korlibs.korge.gradle.util.createThis
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File


fun Project.korgeFleks(callback: KorgeFleksExtension.() -> Unit) = korgeFleks.apply(callback)
val Project.korgeFleks: KorgeFleksExtension get() = extensionGetOrCreate("korgeFleks")

open class KorgeFleksExtension(
    val project: Project,
) {
    private val assetGroup = "assets"
    // Define any configuration properties or methods for the extension here

    var asepriteExe: String = ""  // = "C:/Tools/Aseprite/Aseprite.exe",

    fun commonAssets(path: String, callback: AssetsConfig.() -> Unit) {
        if (!File(asepriteExe).exists()) throw GradleException("Aseprite executable not found: '$asepriteExe' " +
            "Make sure to set 'asepriteExe' property in korgeFleks extension.")

        val commonAssetsConfig = AssetsConfig(asepriteExe, project.projectDir, path)

        project.tasks.createThis<Task>("commonAssets") {
            group = assetGroup
            doLast {
                commonAssetsConfig.apply(callback)
            }
        }
    }

    // TODO: Implement LDtk level parsing and loading
    fun loadLDtkLevel(name: String) =
        project.tasks.createThis<Task>(name) {
            group = assetGroup
            doFirst {
                println("KorgeFleksExtension: loadLDtkLevel: $name")
//                KorgeFleksAssets.parseLDtkLevel()
            }
        }


}
