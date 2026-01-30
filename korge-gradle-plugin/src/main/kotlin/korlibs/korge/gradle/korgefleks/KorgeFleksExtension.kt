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
     * Assets which are added here will be grouped into a common asset bundle loaded for all worlds and levels.
     * Common assets are typically used for shared resources like UI elements, sounds, and other global assets.
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
    ) {
        if (!File(asepriteExe).exists()) throw GradleException("Aseprite executable not found: '$asepriteExe' - Make sure to set 'asepriteExe' property in KorgeFleks extension.")

        val assetName = "common"
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
                assetConfig.apply(config)
                assetConfig.buildAssetStore()
            }
        }
    }

    /**
     * Prepares asset processing by creating a Gradle task for handling world assets.
     * Assets which are added here in the root of worldAssets { ... } will be used for the entire world,
     * across all chunks within that world.
     *
     * TODO
     * It is possible to define assets specific to world chunks using the chunkAssets lambda function.
     * Chunk assets are typically used for large worlds that are divided into smaller sections (chunks)
     * to optimize loading, performance and memory consumption.
     *
     * @param path The relative path to the asset directory.
     * @param world The world number for which the assets are being configured.
     * @param config A lambda function to configure the assets.
     *
     * @throws GradleException if the Aseprite executable is not found.
     */
    fun worldClusterAssets(
        path: String,
        world: Int,
        clusterName: String,
        config: WorldAssetConfig.() -> Unit
    ) {
        if (!File(asepriteExe).exists()) throw GradleException("Aseprite executable not found: '$asepriteExe' - Make sure to set 'asepriteExe' property in KorgeFleks extension.")

        val assetResourcePath = "world_${world}/${clusterName}"
        val assetConfig = WorldAssetConfig(asepriteExe, project.projectDir, path, assetResourcePath )
        // Set default names and atlas size
        assetConfig.textureAtlasName = textureAtlasName
        assetConfig.tilesetAtlasName = tilesetAtlasName
        assetConfig.atlasWidth = atlasWidth
        assetConfig.atlasHeight = atlasHeight

        val assetName = "world${world}_${clusterName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
        val taskName = assetName.replace("/", "").replace("_", "")
        project.tasks.createThis<Task>("${taskName}Assets") {
            group = assetGroup
            doLast {
                assetConfig.apply(config)
                assetConfig.buildAssetStore()
            }
        }
    }
}
