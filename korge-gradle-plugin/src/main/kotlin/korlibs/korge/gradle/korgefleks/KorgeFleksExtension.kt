package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.extensionGetOrCreate
import korlibs.korge.gradle.util.createThis
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.File
import javax.inject.Inject


fun Project.korgeFleks(callback: KorgeFleksExtension.() -> Unit) = korgeFleks.apply(callback)
val Project.korgeFleks: KorgeFleksExtension get() = extensionGetOrCreate("korgeFleks")

/**
 * KorgeFleks Gradle extension for managing assets and configurations.
 *
 * @param project The Gradle project instance.
 *
 * Properties:
 * - asepriteExe: Path to the Aseprite executable.
 * - textureAtlasName: Default name for the texture atlas (texture).
 * - tilesetAtlasName: Default name for the tileset atlas (tileset).
 * - atlasWidth: Default width for the atlases (2048).
 * - atlasHeight: Default height for the atlases (2048).
 */
open class KorgeFleksExtension(
    @Inject val project: Project,
) {
    private val assetGroup = "assets"

    var asepriteExe: String = ""
    var textureAtlasName: String = "texture"
    var tilesetAtlasName: String = "tileset"
    var atlasWidth: Int = 2048
    var atlasHeight: Int = 2048

    /**
     * Prepares asset processing by creating a Gradle task for handling common assets.
     *
     * Atlas names and sizes can be customized via parameters, with defaults provided from the KorgeFleks extension's properties.
     *
     * @param path The relative path to the asset directory.
     * @param config A lambda function to configure the assets.
     *
     * @throws GradleException if the Aseprite executable is not found.
     */
    fun commonAssets(
        path: String,
        config: AssetConfig.() -> Unit
    ) = processAssets(path, callback = config)

    fun worldAssets(
        path: String,
        world: Int,
        config: AssetConfig.() -> Unit
    ) = processAssets(path, world, callback = config)

    fun worldLevelAssets(
        path: String,
        world: Int,
        level: Int,
        config: AssetConfig.() -> Unit
    ) = processAssets(path, world, level, callback = config)

    fun worldLevelChunkAssets(
        path: String,
        world: Int,
        level: Int,
        config: AssetConfig.() -> Unit
    ) = processAssets(path, world, level, true, config)

    private fun processAssets(
        path: String,
        world: Int? = null,
        level: Int? = null,
        chunk: Boolean = false,
        callback: AssetConfig.() -> Unit,
    ) {
        if (!File(asepriteExe).exists()) throw GradleException("Aseprite executable not found: '$asepriteExe' - Make sure to set 'asepriteExe' property in KorgeFleks extension.")

        // Sanity check for special chunk assets
        if (chunk && (world == null || level == null)) {
            throw GradleException("KorgeFleksExtension: worldLevelChunkAssets function called without worldNum and levelNum!")
        }

        // TODO here we need to check how we cluster assets from chunks into asset bundles which are loaded together
        val special = if (chunk) "/chunk" else ""

        val assetName = if (world != null && level != null) "world_${world}/level_${level}${special}"
        else if (world != null) "world_$world"
        else if (level != null) throw GradleException("KorgeFleksExtension: worldAssets: levelNum specified without worldNum!")
        else "common"
        val assetConfig = AssetConfig(asepriteExe, project.projectDir, path, assetName )
        // Set default names and atlas size
        assetConfig.textureAtlasName = textureAtlasName
        assetConfig.tilesetAtlasName = tilesetAtlasName
        assetConfig.atlasWidth = atlasWidth
        assetConfig.atlasHeight = atlasHeight

        val taskName = assetName.replace("/", "").replace("_", "")
        project.tasks.createThis<Task>("${taskName}Assets") {
            group = assetGroup
            doLast {
                assetConfig.apply(callback)
                assetConfig.buildAtlases()
            }
        }
    }
}
