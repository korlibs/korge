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
     * Assets which are added here will be grouped into a common asset bundle loaded for all worlds of the game.
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

        val taskName = "common"
        val assetConfig = AssetConfig(asepriteExe, project.projectDir, path, taskName )
        // Set default names and atlas size
        assetConfig.textureAtlasName = textureAtlasName
        assetConfig.tilesetAtlasName = tilesetAtlasName
        assetConfig.atlasWidth = atlasWidth
        assetConfig.atlasHeight = atlasHeight

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
     * Assets which are added here are grouped into world cluster specific asset bundles.
     * Cluster assets are typically used for large worlds that are divided into smaller sections (chunks)
     * to optimize loading, performance and memory consumption.
     *
     * @param worldName The name of the world for which the assets are being configured.
     * @param clusterName The name of the cluster within the world.
     * @param path The relative path to the asset directory from project root directory.
     * @param config A lambda function to configure the assets.
     *
     * @throws GradleException if the Aseprite executable is not found.
     */
    fun worldClusterAssets(
        worldName: String,
        clusterName: String,
        path: String,
        config: WorldClusterAssetConfig.() -> Unit
    ) {
        if (!File(asepriteExe).exists()) throw GradleException("Aseprite executable not found: '$asepriteExe' - Make sure to set 'asepriteExe' property in KorgeFleks extension.")

        val assetResourcePath = "${worldName}/${clusterName}"
        val assetConfig = WorldClusterAssetConfig(asepriteExe, project.projectDir, path, assetResourcePath, clusterName)
        // Set default names and atlas size
        assetConfig.textureAtlasName = textureAtlasName
        assetConfig.tilesetAtlasName = tilesetAtlasName
        assetConfig.atlasWidth = atlasWidth
        assetConfig.atlasHeight = atlasHeight

        val taskName = "${worldName}${clusterName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
            .replace("/", "").replace("_", "")
        project.tasks.createThis<Task>("${taskName}Assets") {
            group = assetGroup
            doLast {
                assetConfig.apply(config)
                assetConfig.buildAssetStore(assetResourcePath)
            }
        }
    }

    /**
     * Prepares asset processing by creating a Gradle task for handling loading of the level map of a specific world.
     * The level map defines the layout and structure of the world, including terrain, objects,
     * and other environmental features.
     *
     * @param worldName The name of the world for which the level map is being loaded.
     * @param path The relative path to the level map directory from project root directory.
     * @param config A lambda function to configure the assets.
     */
    fun worldLevelMapAssets(
        worldName: String,
        path: String,
        config: WorldLevelMapAssetConfig.() -> Unit
    ) {
        val assetName = "${worldName}LevelMap"
        val assetConfig = WorldLevelMapAssetConfig(project.projectDir, worldName, path)

        val taskName = assetName.replace("/", "").replace("_", "")
        project.tasks.createThis<Task>("${taskName}Assets") {
            group = assetGroup
            doLast {
                assetConfig.apply(config)
                assetConfig.buildAssetStore()  // start to load the level map
            }
        }
    }
}
