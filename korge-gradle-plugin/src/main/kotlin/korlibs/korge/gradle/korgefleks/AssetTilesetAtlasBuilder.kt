package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.TILES
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.TILESETS
import korlibs.korge.gradle.texpacker.NewTexturePacker
import java.io.File
import java.util.ArrayList


/**
 * Builds a tileset atlas from exported tiles in the specified directory.
 * The resulting tileset atlas images are stored in the game resources directory,
 * and the tileset information is added to the provided asset info map.
 * The tiles are not cropped or rotated during packing. Also, duplicate tiles are removed and
 * their references are updated accordingly to point to the same tile in the atlas.
 *
 * @param exportTilesetDir The directory containing exported tilesets.
 * @param gameResourcesDir The directory where game resources are stored.
 * @param assetInfo A linked hash map to store asset information.
 */
class AssetTilesetAtlasBuilder(
    private val exportTilesetDir: File,
    private val gameResourcesDir: File,
    private val assetInfo: LinkedHashMap<String, Any>
) {
    /**
     * Builds a tileset atlas from the exported tiles in the export tiles directory.
     * Adds the tileset atlas information to the internal asset info list.
     */
    fun buildTilesetAtlas(
        tilesetAtlasName: String,
        tileWidth : Int,
        tileHeight : Int,
        atlasWidth: Int,
        atlasHeight: Int,
        atlasPadding : Int
    ) {
        // Then build tilesets atlas
        if (exportTilesetDir.listFiles() != null && exportTilesetDir.listFiles().isNotEmpty()) {
            // First build texture atlas
            val atlasInfoList = NewTexturePacker.packTilesets(exportTilesetDir,
                padding = atlasPadding,
                tileWidth = tileWidth,
                tileHeight = tileHeight,
                textureAtlasWidth = atlasWidth,
                textureAtlasHeight = atlasHeight
            )

            // Go through all packed atlases
            val tilesets = assetInfo[TILESETS] as ArrayList<String>
            val tilesInfo = assetInfo[TILES] as LinkedHashMap<String, Any>

            atlasInfoList.forEachIndexed { idx, atlasInfo ->
                // And store their tileset atlas image as png files
                val atlasOutputFile = gameResourcesDir.resolve("${tilesetAtlasName}_${idx}.atlas.png")
                atlasOutputFile.parentFile?.let { it.mkdirs() }
                    ?: error("Could not create game resources directory: ${gameResourcesDir.path}")
                atlasInfo.writeImage(atlasOutputFile)

                // Add tileset atlas file name to asset info
                tilesets.add(atlasOutputFile.name)

                // Go through each frame in the atlas and map it to the correct asset info entry
                val frames = atlasInfo.info["frames"] as Map<String, Any>
                frames.forEach { (frameName, frameEntry) ->
                    frameEntry as Map<String, Any>

                    val tileInfo = linkedMapOf(
// TODO check if a map or list fits here better
//                        "n" to frameName,
                        "i" to idx
                    )
                    tilesInfo[frameName] = tileInfo

                }
            }
        }
    }
}
