package korlibs.korge.gradle.korgefleks

import com.android.build.gradle.internal.cxx.json.jsonStringOf
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.TILE_MAPS
import korlibs.korge.gradle.util.LocalSFile
import korlibs.korge.gradle.util.fromJson
import org.gradle.api.GradleException
import java.io.File

class AssetLevelMapExporter(
    private val assetDir: File,
    private val gameResourcesDir: File,
    private val assetInfo: LinkedHashMap<String, Any>,
    private val amountOfTiles: Int
) {
    private val stackSize = 10  // Max number of stacked tiles per cell

    private val levelDataDir = gameResourcesDir.resolve("level_data")

//    val assetTilesetExporter = AssetTilesetExporter(assetDir, "common")

    internal var getFile: (filename: String) -> File = {
        // Get the File object from the asset path in gradle plugin build
        val file = assetDir.resolve(it)
        if (!file.exists()) throw GradleException("LDtk file '${file}' not found!")
        file
    }
    internal var loadLDtkFile: (file: File) -> Map<String, Any?> = {
        // Load and parse LDtk file in gradle plugin build
        LocalSFile(it).read().fromJson() as Map<String, Any?>
    }

    private var stackedTileMapPopulated: Boolean = false

    /**
     * Data class for storing tileset info per cluster.
     *
     * @param tileSetName Name of the tileset. It is defined in worldClusterAssets config and as name of tile set in LDtk file.
     *                    Both names must match.
     * @param offset Offset of the tileset in the cluster. This is important to map tiles correctly from LDtk level layers to the tilesets.
     *               A tile ID in LDtk level layer will be mapped by this offset to the correct tile set in the cluster asset.
     * @param clusterName Name of the cluster asset the tileset belongs to.
     */
    data class TileSetData(
        val tileSetName: String,
        val offset: Int,
        val clusterName: String
    )

    data class ChunkData(
        val chunkName: String,
        val entities: List<Map<String, Any>>,
        val stackedTileMapData: List<MutableList<Int>>,
        val clusterList: List<String>
    )

    data class TileMapData(
        val tileMapName: String,
        val stackedTileMapData: List<MutableList<Int>>,
        val clusterList: List<String>  // Needed by renderer for offsets of tilesets in clusters
    )

    /**
     * Export a single level map from an LDtk file as tile map object
     *
     * @param filename LDtk filename
     * @param levelName Level name to export
     * @param clusterName Cluster name where the level map belongs to
     * @param tileSetList Map of tileset names per cluster asset. This indicates the order of tilesets in the cluster, which
     *        is important to map tiles correctly from LDtk level layers to the tilesets.
     */
    fun exportTileMapLDtk(filename: String, levelName: String, clusterName: String, tileSetList: List<String>) {
        // Load single level map from LDtk file and export as tile map object

        val ldtkFile = getFile(filename)
        val ldtkJson = loadLDtkFile(ldtkFile)

        val ldtkLevels = ldtkJson["levels"] as List<Map<String, Any?>>
        val defaultGridSize = ldtkJson["defaultGridSize"] as Int
        val defs = ldtkJson["defs"] as Map<String, Any?>
        val jsonTileSets = defs["tilesets"] as List<Map<String, Any?>>

        // Map for storing starting index for each tileset based on its order in the asset cluster
        val tileSetDataByUid = mutableMapOf<Int, TileSetData>()

        // Go through all tilesets and save their cluster assignment from tags and external tileSetsPerClusterMap
        jsonTileSets.forEach { jsonTileSet ->
            val tileSetName = jsonTileSet["identifier"] as String

            // Check if tags contains exactly one of the cluster, layer and name tags
            val tags = jsonTileSet["tags"] as List<String>
            val clusterLDtkTag: String = if (tags.isNotEmpty()) tags[0] else throw GradleException("Tileset '$tileSetName' has no 'cluster name' defined as tag in LDtk file!")

            val tileSetUid = jsonTileSet["uid"] as Int
            val tileSetPathName = jsonTileSet["relPath"] as String
//            println("  Exporting tileset '$tileSetName' from path '$tileSetPathName' ...")

            // Find tileset in cluster map and store
            var offset = -1
            tileSetList.forEachIndexed { idx, tileSet ->
                if (tileSet == tileSetName) {
                    // Tileset found in this cluster
                    offset = idx
                    return@forEachIndexed
                }
            }
            if (offset == -1) println("ERROR: Tileset '$tileSetName' not found in cluster '$clusterName'! Please check if the tileset name in the LDtk file matches the name in the cluster asset config and if the cluster asset is assigned to the correct level chunk!")

            tileSetDataByUid[tileSetUid] = TileSetData(tileSetName, offset, clusterName)
        }

        // Definition: maximum of 16 layers per level --> Each level chunk can use up to 16 tilesets
        ldtkLevels.forEach { ldtkLevel ->
            val chunkName = ldtkLevel["identifier"] as String
            if (chunkName == levelName) {
//                println("Processing '$chunkName' ...")

                val levelGridHeight = (ldtkLevel["pxHei"] as Int) / defaultGridSize  // Level height in tiles
                val levelGridWidth = (ldtkLevel["pxWid"] as Int) / defaultGridSize   // Level width in tiles

                // An array containing all Layer instances.
                // **IMPORTANT**: if the project option "*Save levels separately*" is enabled, this field will be `null`.
                // This array is **sorted in display order**: the 1st layer is the top-most and the last is behind.
                val ldtkLevelLayers = ldtkLevel["layerInstances"] as List<Map<String, Any?>>

                // Create stacked tile map data array (width * height) with max 10 stacked tiles per cell
                val stackedTileMapData: List<MutableList<Int>> = List(levelGridHeight * levelGridWidth) { MutableList(stackSize) { -1 } }
                stackedTileMapPopulated = false

                val clusterList = mutableListOf<String>()

                // Go through all layers in reverse order (from background to foreground)
                for (layerIdx in ldtkLevelLayers.size - 1 downTo 0) {
                    val ldtkLayer = ldtkLevelLayers[layerIdx]
                    val layerName = ldtkLayer["__identifier"] as String
//                    println("  Layer: '$layerName'")
                    val layerGridWidth = ldtkLayer["__cWid"] as Int  // Layer width in grid cells

                    // Layer contains either auto-tiles or grid-tiles, so add
                    val autoLayerTiles = ldtkLayer["autoLayerTiles"] as List<Map<String, Any?>>
                    val gridTiles = ldtkLayer["gridTiles"] as List<Map<String, Any?>>

                    when {
                        autoLayerTiles.isNotEmpty() || gridTiles.isNotEmpty() -> {
                            // Get the used tileset for this layer and pass its index in the cluster for correct tile mapping
                            val tilesetDefUid = ldtkLayer["__tilesetDefUid"] as Int?
                            if (tilesetDefUid != null && tileSetDataByUid.containsKey(tilesetDefUid)) {
                                // Add cluster name to clusterList if not already present
                                val clusterName = tileSetDataByUid[tilesetDefUid]!!.clusterName
                                if (!clusterList.contains(clusterName)) {
                                    clusterList.add(clusterName)
                                }
                                // Get index of cluster in cluster list
                                val clusterIndex = clusterList.indexOf(clusterName)
                                val tileOffset = tileSetDataByUid[tilesetDefUid]!!.offset

                                stackTilesIntoTileMap(autoLayerTiles + gridTiles, stackedTileMapData, layerGridWidth, defaultGridSize, clusterIndex, tileOffset)
                            }
                        }
                        else -> println("WARNING: No tiles or entities found in layer '$layerName'!")
                    }
                }

                if (stackedTileMapPopulated) {
                    println("Export LDtk file: '${filename}', layer: '${levelName}'")

                    // Tile map object consists of
                    // - tile map name
                    // - stackedTileMapData
                    // - list of clusters used in this tile map (tile sets)

                    // Build stacked tile map which consists of only populated tiles (without -1 entries)
                    val stackedTileMap = mutableListOf<List<Int>>()
                    stackedTileMapData.forEach { stackedTiles ->
                        val tiles = mutableListOf<Int>()
                        stackedTiles.forEach { tile ->
                            if (tile != -1) {
                                tiles.add(tile)
                            }
                        }
                        stackedTileMap.add(tiles)
                    }

                    val tileMaps = assetInfo[TILE_MAPS] as LinkedHashMap<String, Any>
                    tileMaps[chunkName] = linkedMapOf(
                        "m" to stackedTileMap,
                        "w" to levelGridWidth,
                        "h" to levelGridHeight,
                        "g" to defaultGridSize,
                        "c" to clusterList
                    )
//                    println()
                }
            }
        }
    }

    private fun stackTilesIntoTileMap(
        layerTiles: List<Map<String, Any?>>,
        stackedTileMapData: List<MutableList<Int>>,
        stackedTileMapWidth: Int,
        tileSize: Int,
        clusterIndex: Int,
        tileOffset: Int
    ) {
        for (tile in layerTiles) {
            val (px, py) = tile["px"] as List<Int>  // Tile x, y position in layer
            val x = px / tileSize
            val y = py / tileSize
            val dx = px % tileSize
            val dy = py % tileSize
            // Tile id in the tileset which identifies the tile graphic
            // plus offset which is determined by the tileset order in the cluster
            val tileId = (tile["t"] as Int) + tileOffset
            //val flipX = (tile["f"] as Int).hasBitSet(0)  -- not supported yet
            //val flipY = (tile["f"] as Int).hasBitSet(1)

            if ((dx != 0 || dy != 0)) println("WARNING: Tile at pixel position ($px,$py) is not aligned to tile size $tileSize" +
                " (dx=$dx, dy=$dy)! This is not supported and tile offset will be ignored!")

            val tileIndex = y * stackedTileMapWidth + x
            val stackedTile = stackedTileMapData[tileIndex]

            for ((idx, tile) in stackedTile.withIndex()) {
                if (tile == -1) {
                    // Empty tile found, stack tile here
                    if (idx < stackSize) {
                        // chunk index in bits 0-3, tile id in bits 4 - 19
                        val tile = (clusterIndex and 0xf) or ((tileId and 0xffff) shl 4)
                        stackedTileMapData[tileIndex][idx] = tile
                        stackedTileMapPopulated = true
                    } else println("ERROR: Stack size exceeded at position ($x,$y)! Max stack size is $stackSize tiles per cell.")
                    break
                }
            }
        }
    }

    fun exportLevelMapLDtk(filename: String, tileSetsPerClusterMap: Map<String, List<String>>, simplifyJson: Boolean, layerName: String = "default") {
        // Load all level maps from LDtk file and export each as level chunk object
        println("Export LDtk file: '${filename}'")

        val ldtkFile = getFile(filename)
        val ldtkJson = loadLDtkFile(ldtkFile)

        val ldtkLevels = ldtkJson["levels"] as List<Map<String, Any?>>
        val defaultGridSize = ldtkJson["defaultGridSize"] as Int
        val defaultLevelWidth = ldtkJson["defaultLevelWidth"]?.let { it as Int } ?: 0
        val defaultLevelHeight = ldtkJson["defaultLevelHeight"]?.let { it as Int } ?: 0

        val defs = ldtkJson["defs"] as Map<String, Any?>
        val jsonTileSets = defs["tilesets"] as List<Map<String, Any?>>
        val levelWidth: Int = ldtkJson["worldGridWidth"] as Int? ?: 0  // Level width in pixels
        val levelHeight = ldtkJson["worldGridHeight"] as Int? ?: 0     // Level height in pixels

        // Map for storing starting index for each tileset based on its order in the asset cluster
        val tileSetDataByUid = mutableMapOf<Int, TileSetData>()

        // Go through all tilesets and save their cluster assignment from tags and external tileSetsPerClusterMap
        jsonTileSets.forEach { jsonTileSet ->
            val tileSetName = jsonTileSet["identifier"] as String
            val tileSetUid = jsonTileSet["uid"] as Int
            // Find tileset in cluster map and store
            var clusterName = ""
            var offset = 0
            tileSetsPerClusterMap.forEach { (cluster, listOfTileSets) ->
                listOfTileSets.forEachIndexed { idx, tileSet ->
                    if (tileSet == tileSetName) {
                        // Tileset found in this cluster
                        clusterName = cluster
                        offset = idx * amountOfTiles
                        return@forEach
                    }
                }
            }

            if (clusterName.isNotEmpty()) tileSetDataByUid[tileSetUid] = TileSetData(tileSetName, offset, clusterName)
            else println("  Ignoring tileset: '$tileSetName'")
        }

        // Create Grid-vania map of all level chunks with their chunk numbers - this is used to get the neighboring chunk numbers
        val gridVaniaMap = mutableMapOf<Pair<Int, Int>, Int>()  // Map of chunk coordinates to chunk number
        ldtkLevels.forEach { ldtkLevel ->
            val chunkName = ldtkLevel["identifier"] as String
            val chunkNumber = chunkName.substringAfterLast("_").toIntOrNull() ?: error("Chunk name '$chunkName' does not contain a valid chunk number!")
            val levelX: Int = ldtkLevel["worldX"] as Int / levelWidth
            val levelY: Int = ldtkLevel["worldY"] as Int / levelHeight
            gridVaniaMap[Pair(levelX, levelY)] = chunkNumber
        }

        var maxLevelX = 0
        var maxLevelY = 0

        // Get size of grid-vania map
        gridVaniaMap.forEach { (pair, _) ->
            val levelX = pair.first
            val levelY = pair.second
            if (levelX > maxLevelX) maxLevelX = levelX
            if (levelY > maxLevelY) maxLevelY = levelY
        }

        // Save common level map data
        val levelMapInfo = mutableMapOf<String, Any>()
        levelMapInfo["v"] = listOf(1, 0, 1)
        levelMapInfo["x"] = maxLevelX + 1  // Add 1 to get the total width and height of the grid-vania map
        levelMapInfo["y"] = maxLevelY + 1
        levelMapInfo["w"] = defaultLevelWidth / defaultGridSize
        levelMapInfo["h"] = defaultLevelHeight / defaultGridSize
        levelMapInfo["t"] = defaultGridSize

        val commonChunkJsonFile = levelDataDir.resolve("common.json")
        commonChunkJsonFile.parentFile?.let { parent ->
            if (!parent.exists() && !parent.mkdirs()) error("CommonChunkJSonFile - Failed to create directory: ${parent.path}")
            val jsonString = jsonStringOf(levelMapInfo)
            // Simplify JSON string by removing unnecessary spaces and line breaks
            val simplifiedJsonString = if (simplifyJson) jsonString.replace(Regex("\\s+"), "")
            else jsonString
            commonChunkJsonFile.writeText(simplifiedJsonString)
        }

        // Definition: maximum of 16 layers per level --> Each level chunk can use up to 16 tilesets
        ldtkLevels.forEach { ldtkLevel ->
            // Game object counter per chunk
            var gameObjectCnt = 0

            val chunkInfo = mutableMapOf<String, Any>()
            val chunkLevelMap: MutableMap<String, Any> = mutableMapOf()  // MutableList<List<Int>> = mutableListOf()
            val chunkEntities: MutableList<MutableMap<String, Any>> = mutableListOf()

            chunkInfo["e"] = chunkEntities

            // Calculate level position in world grid
            val levelX: Int = ldtkLevel["worldX"] as Int / levelWidth
            val levelY: Int = ldtkLevel["worldY"] as Int / levelHeight
//            val levelHeight = ldtkLevel["pxHei"] as Int  // Level height in pixels
//            val levelWidth = ldtkLevel["pxWid"] as Int   // Level width in pixels

            val chunkName = ldtkLevel["identifier"] as String

            println("  Processing '$chunkName' ...")

            // Get chunk number from chunk name
            val chunkNumber = chunkName.substringAfterLast("_").toIntOrNull() ?: error("Chunk name '$chunkName' does not contain a valid chunk number!")

            val levelGridHeight = (ldtkLevel["pxHei"] as Int) / defaultGridSize  // Level height in tiles
            val levelGridWidth = (ldtkLevel["pxWid"] as Int) / defaultGridSize   // Level width in tiles

            // An array containing all Layer instances.
            // **IMPORTANT**: if the project option "*Save levels separately*" is enabled, this field will be `null`.
            // This array is **sorted in display order**: the 1st layer is the top-most and the last is behind.
            val ldtkLevelLayers = ldtkLevel["layerInstances"] as List<Map<String, Any?>>

            // Create stacked tile map data array (width * height) with max 10 stacked tiles per cell
            val stackedTileMapData: List<MutableList<Int>> = List(levelGridHeight * levelGridWidth) { MutableList(stackSize) { -1 } }
            stackedTileMapPopulated = false

            val clusterList = mutableListOf<String>()

            // Go through all layers in reverse order (from background to foreground)
            for (layerIdx in ldtkLevelLayers.size - 1 downTo 0) {
                val ldtkLayer = ldtkLevelLayers[layerIdx]
                val layerName = ldtkLayer["__identifier"] as String
                val layerGridWidth = ldtkLayer["__cWid"] as Int  // Layer width in grid cells

                // Layer contains either auto-tiles or grid-tiles, so add
                val autoLayerTiles = ldtkLayer["autoLayerTiles"] as List<Map<String, Any?>>
                val gridTiles = ldtkLayer["gridTiles"] as List<Map<String, Any?>>
                val entityInstances = ldtkLayer["entityInstances"] as List<Map<String, Any?>>

                when {
                    entityInstances.isNotEmpty() -> {
                        entityInstances.forEach { ldtkEntity ->
                            // Put all entity configs into one JSON file per chunk as list with local game object counter
                            val entityName = ldtkEntity["__identifier"]?.let { it as String } ?: "undefined_entity"
                            // Load all entity configs by first checking if field 'entityConfig' exists
                            val ldtkEntities = ldtkEntity["fieldInstances"] as List<Map<String, Any?>>

                            if (ldtkEntities.firstOrNull { it["__identifier"] == "entityConfig" } != null) {
                                val gameObjectName: String =
                                    if ((ldtkEntity["__tags"] as List<String>).firstOrNull { it == "unique" } != null) {
                                        // Add scripts without unique count value - they are unique by name because they exist only once
                                        entityName
                                    } else {
                                        // Add other game objects with a unique name as identifier
                                        "${chunkName}_${entityName}_${gameObjectCnt++}"
                                    }
                                // Add entity config type
                                val entityConfigField = ldtkEntities.first { it["__identifier"] == "entityConfig" }
                                val entityConfigType = entityConfigField["__value"]?.let { it as String } ?: error("Entity config field '__value' is null for entity '$entityName' in chunk '$chunkName'!")

                                val chunkEntity = mutableMapOf<String, Any>()
                                chunkEntities.add(chunkEntity)
                                chunkEntity["type"] = entityConfigType
                                chunkEntity["name"] = gameObjectName

                                // Add position of entity = (chunk position in the level) + (position within the chunk) + (pivot point)
                                // x and y position in pixels relative to the top left corner of the chunk, levelX and levelY are the position of the chunk in the world grid, so they are not needed for entity position within the chunk
                                val entityPosX: Int = (ldtkEntity["px"] as List<Int>)[0] + (levelWidth * levelX)  // x position in pixels
                                val entityPosY: Int = (ldtkEntity["px"] as List<Int>)[1] + (levelHeight * levelY)  // y position in pixels
                                val entityPivotX: Float = (ldtkEntity["__pivot"] as List<Float>)[0]  // pivot within entity width/height [0..1]
                                val entityPivotY: Float = (ldtkEntity["__pivot"] as List<Float>)[1]

                                // Add position of entity
                                (ldtkEntity["__tags"] as List<String>).firstOrNull { it == "positionable" }?.let {
                                    chunkEntity["x"] = entityPosX
                                    chunkEntity["y"] = entityPosY
                                    chunkEntity["anchorX"] = (entityPivotX * ldtkEntity["width"] as Int).toInt()
                                    chunkEntity["anchorY"] = (entityPivotY * ldtkEntity["height"] as Int).toInt()
                                }
                                // Add all other fields of entity
                                ldtkEntities.forEach { field ->
                                    if (field["__identifier"] != "entityConfig") {
                                        chunkEntity[field["__identifier"] as String] = field["__value"] as Any
                                    }
                                }
                            } else println("ERROR: Game object with name '${entityName}' has no field 'entityConfig'!")
                        }
                    }

                    autoLayerTiles.isNotEmpty() || gridTiles.isNotEmpty() -> {
                        // Get the used tileset for this layer and pass its index in the cluster for correct tile mapping
                        val tilesetDefUid = ldtkLayer["__tilesetDefUid"] as Int?
                        if (tilesetDefUid != null && tileSetDataByUid.containsKey(tilesetDefUid)) {
                            // Add cluster name to clusterList if not already present
                            val clusterName = tileSetDataByUid[tilesetDefUid]!!.clusterName
                            if (!clusterList.contains(clusterName)) {
                                clusterList.add(clusterName)
                            }
                            // Get index of cluster in cluster list
                            val clusterIndex = clusterList.indexOf(clusterName)
                            val tileOffset = tileSetDataByUid[tilesetDefUid]!!.offset

                            stackTilesIntoTileMap(autoLayerTiles + gridTiles, stackedTileMapData, layerGridWidth, defaultGridSize, clusterIndex, tileOffset)
                        }
                    }
//                    else -> println("WARNING: No tiles or entities found in layer '$layerName'!")
                }
            }

            val chunkTop: Int = gridVaniaMap[Pair(levelX, levelY - 1)] ?: 0
            val chunkBottom: Int = gridVaniaMap[Pair(levelX, levelY + 1)] ?: 0
            val chunkLeft: Int = gridVaniaMap[Pair(levelX - 1, levelY)] ?: 0
            val chunkRight: Int = gridVaniaMap[Pair(levelX + 1, levelY)] ?: 0

            // Chunk position in world grid
            chunkInfo["x"] = levelX
            chunkInfo["y"] = levelY
            // Neighbor chunks
            chunkInfo["t"] = chunkTop
            chunkInfo["b"] = chunkBottom
            chunkInfo["l"] = chunkLeft
            chunkInfo["r"] = chunkRight

            chunkInfo["ls"] = chunkLevelMap

            if (stackedTileMapPopulated) {
                // Build stacked tile map which consists of only populated tiles (without -1 entries)
                val stackedTileMap = mutableListOf<List<Int>>()
                stackedTileMapData.forEach { stackedTiles ->
                    val tiles = mutableListOf<Int>()
                    stackedTiles.forEach { tile ->
                        if (tile != -1) {
                            tiles.add(tile)
                        }
                    }
                    stackedTileMap.add(tiles)
                }
                chunkLevelMap[layerName] = linkedMapOf(
                    "s" to 1f,  // Scroll factor for parallax effect - not supported yet, default is 1 (no parallax)
                    // factor 1f == normal speed, < 1f slower, > 1f faster than normal speed
                    // normal == 64x64 tiles, < 1f ==> 48x48 tiles, > 1f ==> 90x90 tiles
                    // Effectively scroll speed of all world layers shall be aligned so that chunks move together.
                    // Scroll factor is calculated based on layer size in tiles compared to normal tile size of 64x64.
                    // So if a layer has 48x48 tiles, scroll factor is 48/64 = 0.75f, if a layer has 90x90 tiles, scroll factor is 90/64 = 1.4f
                    "m" to stackedTileMap,
                    "c" to clusterList
                )
            }
            if (chunkEntities.isNotEmpty()) {
                println("  Add '${chunkEntities.size}' entities to chunk JSON file.")
            }

            val chunkJsonFile = levelDataDir.resolve("${chunkName}.json")
            chunkJsonFile.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) error("ChunkJSonFile - Failed to create directory: ${parent.path}")
                val jsonString = jsonStringOf(chunkInfo)
                // Simplify JSON string by removing unnecessary spaces and line breaks
                val simplifiedJsonString = if (simplifyJson) jsonString.replace(Regex("\\s+"), "")
                else jsonString
                chunkJsonFile.writeText(simplifiedJsonString)
            }
        }
    }
}
