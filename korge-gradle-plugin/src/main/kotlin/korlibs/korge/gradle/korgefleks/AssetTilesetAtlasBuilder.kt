package korlibs.korge.gradle.korgefleks

import com.android.build.gradle.internal.cxx.json.jsonStringOf
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.TILES
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.TILESETS
import korlibs.korge.gradle.texpacker.NewTexturePacker
import java.io.File


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
    private val assetInfo: LinkedHashMap<String, Any>,
    private val tileSetFiles: List<File>,
    private val clusterAssetInfoDir: File
) {
    /**
     * Builds a tileset atlas from the exported tiles in the export tiles directory.
     * Adds the tileset atlas information to the internal asset info list.
     *
     * Each tileset is expected to have the same tile width and height.
     * Also, each tileset is expected to contain 4096 tiles (64x64) by default, but this can be changed via the amountOfTiles parameter.
     */
    fun buildTilesetAtlas(
        clusterName: String,
        tilesetAtlasName: String,
        tileWidth: Int,
        tileHeight: Int,
        atlasWidth: Int,
        atlasHeight: Int,
        atlasPadding: Int,
        amountOfTiles: Int = 64 * 64 // Default to 4096 tiles per tileset
    ) {
        // Then build tilesets atlas
        if (exportTilesetDir.listFiles() != null && exportTilesetDir.listFiles().isNotEmpty()) {
            // First build texture atlas
            val atlasInfoList = NewTexturePacker.packTilesets(
                tileSetFiles,
                padding = atlasPadding,
                tileWidth = tileWidth,
                tileHeight = tileHeight,
                textureAtlasWidth = atlasWidth,
                textureAtlasHeight = atlasHeight
            )
            // Tileset names list contains the original tileset names in the order they were packed into the atlas
            // This is used below to check if the tiles are put in the correct order into the fileFrameInfo list
            val tileSetFilenames: List<String> = tileSetFiles.map { it.nameWithoutExtension }

            println("tileset names of packed atlases:")
            tileSetFilenames.forEach { name -> println(" - ${name}") }

            // Store tileset names list into asset info json file for each asset cluster - it is needed to load correct tileset for each level map layer later
            val clusterAssetInfoJsonFile = clusterAssetInfoDir.resolve("${clusterName}.json")
            clusterAssetInfoJsonFile.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) error("Failed to create directory: ${parent.path}")
                val jsonString = jsonStringOf(tileSetFilenames)
                clusterAssetInfoJsonFile.writeText(jsonString)
            }

            // Create map of tilesets for counting how many tilesets were processed
            val tileSetMap = tileSetFilenames.associateWith { 0 }.toMutableMap()

            // Go through all packed atlases
            val tilesets = assetInfo[TILESETS] as ArrayList<String>
            val tileFramesInfo = Array(amountOfTiles * tileSetFilenames.size) { intArrayOf() }

            atlasInfoList.forEachIndexed { idx, atlasInfo ->
                // And store their tileset atlas image as png files
                val atlasOutputFile = gameResourcesDir.resolve("${tilesetAtlasName}_${idx}.atlas.png")
                atlasOutputFile.parentFile?.let { it.mkdirs() }
                    ?: error("TilesetBuilder - could not create game resources directory: ${gameResourcesDir.path}")
                atlasInfo.writeImage(atlasOutputFile)

                // Add tileset atlas file name to asset info
                tilesets.add(atlasOutputFile.name)

                // Go through each frame in the atlas and map it to the correct asset info entry
                val frames = atlasInfo.info["frames"] as Map<String, Any>
                frames.forEach { (frameName, frameEntry) ->
                    frameEntry as Map<String, Any>
                    val frame = frameEntry["frame"] as Map<String, Int>

                    // Split frame name into index number and tileset name parts
                    val regex = "^(\\d+)_".toRegex()
                    val frameNumberString = regex.find(frameName)?.groupValues?.get(1)
                        ?: error("TilesetBuilder - frame name '${frameName}' is not in expected format '<index>_<name>'!")
                    val tileIndex = frameNumberString.toIntOrNull()
                        ?: error("TilesetBuilder - frame name is not a valid tile index number: '${frameName}'!")
                    val tilesetName = frameName.replace(regex, "")

                    // Sanity check - make sure tileset name exists in tileset names list
                    if (!tileSetFilenames.contains(tilesetName)) {
                        error("TilesetBuilder - tileset name '${tilesetName}' from frame '${frameName}' not found in tileset names list!")
                    }
                    tileSetMap[tilesetName] = tileSetMap[tilesetName]!! + 1

                    val frameWidth = frame["w"] ?: error("TilesetBuilder - frame width is 'null' for tile '${frameName}'!")
                    val frameHeight = frame["h"] ?: error("TilesetBuilder - frame height is 'null' for tile '${frameName}'!")
                    val emptyFrame = (frameWidth == 0) && (frameHeight == 0)
                    // Mark empty frames with x=0 and y=0 in the tile info

                    // Set frame info: [textureIndex, x, y, [tileIndex]]  -- frame index is used for debugging only
                    val tileInfo = intArrayOf(
                        idx,  // Save index to texture atlas where the frame is located
                        if (emptyFrame) 0 else frame["x"] ?: error("TilesetBuilder - frame x is 'null' for tile '${frameName}'!"),
                        if (emptyFrame) 0 else frame["y"] ?: error("TilesetBuilder - frame y is 'null' for tile '${frameName}'!"),
                        tileIndex  // [Optional] tile index for debugging purposes
                    )
                    // Put the tile info into the correct position in the tileFramesInfo array
                    tileFramesInfo[tileIndex] = tileInfo
                }

                // Check if all tilesets consists of the expected amount of tiles
                tileSetMap.forEach { (name, count) ->
                    if (count != amountOfTiles) {
                        error("TilesetBuilder - tileset '${name}' contains ${count} tiles, expected ${amountOfTiles} tiles!")
                    }
                }

                // Finally, store tileset info
                assetInfo[TILES] = linkedMapOf(
                    "w" to tileWidth,
                    "h" to tileHeight,
                    "t" to tileSetFilenames,  // TODO not really needed - remove later after testing finished
                    "f" to tileFramesInfo
                )
            }
        }
    }
}
