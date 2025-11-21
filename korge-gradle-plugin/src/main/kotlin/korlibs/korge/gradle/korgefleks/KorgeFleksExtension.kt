package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.extensionGetOrCreate
import korlibs.korge.gradle.util.createThis
import korlibs.korge.gradle.util.executeSystemCommand
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

    fun loadLDtkLevel(name: String) =
        project.tasks.createThis<Task>(name) {
            group = assetGroup
            doFirst {
                println("KorgeFleksExtension: loadLDtkLevel: $name")
//                KorgeFleksAssets.parseLDtkLevel()
            }
        }

    fun commonAssets(path: String, callback: CommonAssetsConfig.() -> Unit) {
        if (!File(asepriteExe).exists()) throw GradleException("Aseprite executable not found: '$asepriteExe' " +
            "Make sure to set 'asepriteExe' property in korgeFleks extension.")

        val commonAssetsConfig = CommonAssetsConfig(asepriteExe, project.projectDir, path)

        project.tasks.createThis<Task>("commonAssets") {
            group = assetGroup
            doLast {
                commonAssetsConfig.apply(callback)
            }
        }
    }

    fun test() {
//        executeSystemCommand()
    }
}

class CommonAssetsConfig(
    val asepriteExe: String,
    val projectDir: File,
    val path: String
) {


    fun addImageAse(filename: String, output: String) {
        if (output.isBlank()) throw GradleException("No output file specified for Aseprite export of '${filename}'!")

        val aseFile = projectDir.resolve("${path}/${filename}")
        if (!aseFile.exists()) throw GradleException("Aseprite file '${aseFile}' not found!")

        val exportTilesDir = projectDir.resolve("build/assets/${path}")
        if (!exportTilesDir.exists()) exportTilesDir.mkdirs()

        val outFile = exportTilesDir.resolve("img_${output}.png")

        val cmd = arrayOf(asepriteExe, "-b", aseFile.absolutePath, "--save-as", outFile.absolutePath)
        println("Export image file: '${filename}', output: '${output}'")

        //println("Executing command: ${cmd.joinToString(" ")}")
        executeSystemCommand(cmd)
    }

    fun addImageAse(filename: String, layer: String, output: String) {}
    fun addImageAse(filename: String, layer: String, tag: String, output: String) {}
    fun addImageAse(filename: String, layer: String, tags: List<String>, output: String) {}
    fun addImageAse(filename: String, layers: List<String>, output: String) {}
    fun addImageAse(filename: String, layers: List<String>, tag: String, output: String) {}
    fun addImageAse(filename: String, layers: List<String>, tags: List<String>, output: String) {}
}
