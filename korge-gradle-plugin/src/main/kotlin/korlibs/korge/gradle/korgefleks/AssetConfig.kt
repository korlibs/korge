package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.util.ASEInfo
import korlibs.korge.gradle.util.LocalSFile
import korlibs.korge.gradle.util.executeSystemCommand
import org.gradle.api.GradleException
import java.io.File

class AssetsConfig(
    private val asepriteExe: String,
    private val projectDir: File,
    private val assetPath: String
) {
    // Directory where exported tiles and tilesets will be stored
    private val exportTilesDir = projectDir.resolve("build/assets/${assetPath.replace("/", "-").replace("\\", "-")}-tiles")
    private val exportTilesetDir = projectDir.resolve("build/assets/${assetPath.replace("/", "-").replace("\\", "-")}-tilesets")
    // Directory where game resources are located
    private val gameResourcesDir = projectDir.resolve("src/commonMain/resources")

    init {
        // Make sure the export directories exist and that they are empty
        if (exportTilesDir.exists()) exportTilesDir.deleteRecursively()
        if (exportTilesetDir.exists()) exportTilesetDir.deleteRecursively()
        exportTilesDir.mkdirs()
        exportTilesetDir.mkdirs()
    }

    private fun getAseFile(filename: String): File {
        val aseFile = projectDir.resolve("${assetPath}/${filename}")
        if (!aseFile.exists()) throw GradleException("Aseprite file '${aseFile}' not found!")
        return aseFile
    }

    private fun checkLayersAvailable(aseFile: File, layers: List<String>) {
        val aseInfo = ASEInfo.getAseInfo(LocalSFile(aseFile))

    }

    fun addImageAse(filename: String, output: String) {
        if (output.isBlank()) throw GradleException("No output file specified for Aseprite export of '${filename}'!")

        val aseFile = getAseFile(filename)

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
