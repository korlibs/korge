package korlibs.korge.gradle.korgefleks

import com.android.build.gradle.internal.cxx.json.jsonStringOf
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.TILE_MAPS
import korlibs.korge.gradle.util.LocalSFile
import korlibs.korge.gradle.util.fromJson
import org.gradle.api.GradleException
import java.io.File


class AssetLevelMapExporter(
    private val assetDir: File,
    gameResourcesDir: File,
    private val assetInfo: LinkedHashMap<String, Any>,
    private val amountOfTiles: Int
) {
    private val stackSize = 10  // Max number of stacked tiles per cell

    private val levelDataDir = gameResourcesDir.resolve("level_data")
    private val targetCollisionShapesFile = levelDataDir.resolve("collision_shapes.png")
    private lateinit var ldtkPath: File

//    val assetTilesetExporter = AssetTilesetExporter(assetDir, "common")

    internal var getFile: (filename: String) -> File = {
        // Get the File object from the asset path in gradle plugin build
        val file = assetDir.resolve(it)
        if (!file.exists()) throw GradleException("LDtk file '${file}' not found!")
        ldtkPath = file.parentFile  // Get the path of the LDtk file to resolve relative paths for external tilesets
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
     * @param tileIdOffset Offset of the tileset in the cluster. This is important to map tiles correctly from LDtk level layers to the tilesets.
     *               A tile ID in LDtk level layer will be mapped by this offset to the correct tile set in the cluster asset.
     * @param clusterName Name of the cluster asset the tileset belongs to.
     * @param collisionTiles A list of tile IDs and their mapping to collision shapes/types.
     */
    data class TileSetData(
        val tileSetName: String,
        val tileIdOffset: Int,
        val clusterName: String,
        val collisionTiles: Map<Int, Int>
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
            val tileSetUid = jsonTileSet["uid"] as Int
            // Find tileset in cluster map and store
            var tileIdOffset = -1
            tileSetList.forEachIndexed { idx, tileSet ->
                // Sanity check for white spaces in tileset names
                if (tileSet.contains(" ") || tileSetName.contains(" ")) error("Tileset name '${tileSet}' " +
                    "contains white spaces! Please remove white spaces from the tileset name in the gradle task configuration!")
                if (tileSetName.contains(" ")) error("Tileset name '${tileSetName}' contains white spaces! " +
                    "Please remove white spaces from the tileset name in the LDtk file!")

                if (tileSet == tileSetName) {
                    // Tileset found in this cluster
                    tileIdOffset = idx
                    return@forEachIndexed
                }
            }
            if (tileIdOffset == -1) error("Tileset '$tileSetName' not found in cluster '$clusterName'! Please check if the tileset name in the LDtk" +
                " file matches the name in the cluster asset config and if the cluster asset is assigned to the correct level chunk!")

            tileSetDataByUid[tileSetUid] = TileSetData(tileSetName, tileIdOffset, clusterName, mapOf())  // Tile maps do not contain collision info, so empty map is passed here
        }

        // Definition: maximum of 16 layers per level --> Each level chunk can use up to 16 tilesets
        ldtkLevels.forEach { ldtkLevel ->
            val chunkName = ldtkLevel["identifier"] as String
            if (chunkName == levelName) {
                val levelGridHeight = (ldtkLevel["pxHei"] as Int) / defaultGridSize  // Level height in tiles
                val levelGridWidth = (ldtkLevel["pxWid"] as Int) / defaultGridSize   // Level width in tiles

                // An array containing all Layer instances.
                // **IMPORTANT**: if the project option "*Save levels separately*" is enabled, this field will be `null`.
                // This array is **sorted in display order**: the 1st layer is the top-most and the last is behind.
                val ldtkLevelLayers = ldtkLevel["layerInstances"] as List<Map<String, Any?>>? ?: error("Level '${levelName}' does not contain any layers! " +
                    "Please disable 'Save levels separately' in LDtk configuration!")

                // Create stacked tile map data array (width * height) with max 10 stacked tiles per cell
                val stackedTileMapData: List<MutableList<Int>> = List(levelGridHeight * levelGridWidth) { mutableListOf() }
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
                                val tileSetData = tileSetDataByUid[tilesetDefUid]!!

                                stackTilesIntoTileMap(autoLayerTiles + gridTiles, stackedTileMapData, layerGridWidth, defaultGridSize, clusterIndex, tileSetData)
                            }
                        }
                        else -> println("WARNING: No tiles or entities found in layer '$layerName'!")
                    }
                }

                if (stackedTileMapPopulated) {
                    println("Export LDtk file: '${filename}', layer: '${levelName}'")
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
                }
            }
        }
    }

    fun exportLevelMapLDtk(filename: String, tileSetsPerClusterMap: Map<String, List<String>>, simplifyJson: Boolean, layerName: String = "default") {
        // Load all level maps from LDtk file and export each as level chunk object
        println("Export LDtk file: '${filename}'")

        val ldtkFile = getFile(filename)
        val ldtkJson = loadLDtkFile(ldtkFile)

        val ldtkLevels = ldtkJson["levels"] as? List<Map<String, Any?>> ?: emptyList()
        val defaultGridSize = ldtkJson["defaultGridSize"] as? Int ?: error("LDtk JSON file does not contain 'defaultGridSize' definition!")
        val defaultLevelWidth = ldtkJson["defaultLevelWidth"] as? Int ?: error("LDtk JSON file does not contain 'defaultLevelWidth' definition!")
        val defaultLevelHeight = ldtkJson["defaultLevelHeight"] as? Int ?: error("LDtk JSON file does not contain 'defaultLevelHeight' definition!")
        val levelWidth = ldtkJson["worldGridWidth"] as? Int ?: error("LDtk JSON file does not contain 'worldGridWidth' definition!")  // Level width in pixels
        val levelHeight = ldtkJson["worldGridHeight"] as? Int ?: error("LDtk JSON file does not contain 'worldGridHeight' definition!")     // Level height in pixels

        val defs = ldtkJson["defs"] as? Map<String, Any?> ?: error("LDtk JSON file does not contain 'defs' definition!")
        val jsonTileSets = defs["tilesets"] as? List<Map<String, Any?>> ?: error("LDtk JSON file does not contain 'defs->tilesets' definition!")
        val enums = defs["enums"]?.let { it as List<Map<String, Any?>> } ?: error("LDtk JSON file does not contain 'defs->enums' definition!")

        // Find collision config enum and save its values in a map for later use when processing the tilesets and layers of the levels - this is needed to get the collision shape/type info for each tile
        val nameToCollisionId = mutableMapOf<String, Int>()
        val idToCollisionShape = mutableListOf<List<Int>>()
        var collisionTileSetFileName = ""
        enums.firstOrNull { map -> (map["identifier"] as? String) == "collisionConfig" }?.let { collisionEnum ->
            val enumValues = collisionEnum["values"] as? List<Map<String, Any?>> ?: error("Collision config enum does not contain 'values' definition!")
            //println("Collision config enum values:")
            // Add first collision ID for empty collision (no collision) with empty shape definition, so that the collision ID 0 can be used
            // for tiles without collision, and it is guaranteed that it does not overlap with any real collision shape definition from the enum values
            nameToCollisionId["empty"] = 0
            idToCollisionShape.add(listOf(-1))
            //println("  0: 'empty' - (x=-1) - no collision")

            var i = 1
            enumValues.forEach { enumValue ->
                val collisionName = enumValue["id"] as? String ?: error("Collision config enum value does not contain 'id' definition!")
                enumValue["tileRect"]?.let { collisionRect ->
                    collisionRect as Map<String, Any?>
                    // Save tile set name which contains the collision shapes
                    if (collisionTileSetFileName.isEmpty()) {
                        collisionRect["tilesetUid"]?.let { tileSetUid ->
                            tileSetUid as Int
                            collisionTileSetFileName = jsonTileSets.firstOrNull { it["uid"] as? Int == tileSetUid }?.get("relPath") as? String
                                ?: error("No matching tileset found in 'defs->tilesets' for tileSetUid '$tileSetUid' defined in collision config enum value! Please check if the tileset with the collision shapes is included in the LDtk file!")
                            //println("Tile set file name for collision shapes: '$collisionTileSetFileName'")
                        }
                    }
                    // Get collision shape info from tileRect definition in collision config enum value
                    val cx = collisionRect["x"] as? Int ?: error("Collision config enum value 'tileRect' does not contain 'x' definition!")
                    val cy = collisionRect["y"] as? Int ?: error("Collision config enum value 'tileRect' does not contain 'y' definition!")

                    nameToCollisionId[collisionName] = i
                    idToCollisionShape.add(listOf(cx, cy))
                    //println("  $i: '$collisionName' - (x=$cx, y=$cy)")
                    i++
                }
            }
        }
        // Check if tile set file name for collision shapes was found in collision config enum
        if (collisionTileSetFileName.isEmpty()) error("No tile set file name found for collision shapes in collision config enum! Please check if the tileset with the collision shapes is included in the LDtk file!")

        // Map for storing starting index for each tileset based on its order in the asset cluster
        val tileSetDataByUid = mutableMapOf<Int, TileSetData>()

        // Go through all tilesets and save their cluster assignment from tags and external tileSetsPerClusterMap
        jsonTileSets.forEach { jsonTileSet ->
            val tileSetName = jsonTileSet["identifier"]?.let { it as String } ?: error("Identifier of tile set from LDtk JSON file is 'null'!")
            val tileSetUid = jsonTileSet["uid"]?.let { it as Int } ?: error("Uid of tile set '$tileSetName' from LDtk JSON file is 'null'!")

            // Process collision data for tiles
            val enumTags = jsonTileSet["enumTags"]?.let { it as List<Any> }
            val collisionTiles = mutableMapOf<Int, Int>()
            enumTags?.forEach { enumTag ->
                enumTag as Map<String, Any>
                enumTag["enumValueId"]?.let { enumValueId ->
                    enumValueId as String
                    // Get collision ID for name of collision enum
                    val collisionTile = nameToCollisionId[enumValueId] ?: return@let  // Skip if enum value ID is not found in collision config enum - it means that this collision shape has no tile assigned
                    //println("enumValueId: $enumValueId (collision ID: $collisionTile)")
                    enumTag["tileIds"]?.let { tileIds ->
                        tileIds as List<Int>
                        //println("tileIds: $tileIds")
                        tileIds.forEach { tileId ->
                            collisionTiles[tileId] = collisionTile
                        }
                    }
                }
            }

            // Find tileset in cluster map and store
            var clusterName = ""
            var offset = -1
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
            if (clusterName.isNotEmpty()) tileSetDataByUid[tileSetUid] = TileSetData(tileSetName, offset, clusterName, collisionTiles)
            //else println("  Ignoring tileset: '$tileSetName'")
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
        levelMapInfo["c"] = idToCollisionShape

        val commonChunkJsonFile = levelDataDir.resolve("common.json")
        commonChunkJsonFile.parentFile?.let { parent ->
            if (!parent.exists() && !parent.mkdirs()) error("CommonChunkJSonFile - Failed to create directory: ${parent.path}")
            val jsonString = jsonStringOf(levelMapInfo)
            // Simplify JSON string by removing unnecessary spaces and line breaks
            // JSON quoted strings shall not contain white spaces
            val simplifiedJsonString = if (simplifyJson) jsonString.replace(Regex("\\s+"), "")
            else jsonString
            commonChunkJsonFile.writeText(simplifiedJsonString)

            // Copy over tile set files for collision shapes to level data directory - this is needed to load the tile set in the game for processing the collision shapes of the tiles
            val sourceCollisionShapesFile = ldtkPath.resolve(collisionTileSetFileName)
            sourceCollisionShapesFile.copyTo(targetCollisionShapesFile, overwrite = true)
        }

        // Definition: maximum of 16 layers per level --> Each level chunk can use up to 16 tilesets
        ldtkLevels.forEach { ldtkLevel ->
            // Game object counter per chunk
            var gameObjectCnt = 0

            val chunkInfo = mutableMapOf<String, Any>()
            val chunkLevelMap: MutableMap<String, Any> = mutableMapOf()  // MutableList<List<Int>> = mutableListOf()
            val chunkEntities: MutableList<MutableMap<String, Any>> = mutableListOf()
            val chunkSpawnEntities: MutableList<String> = mutableListOf()

            chunkInfo["e"] = chunkEntities
            chunkInfo["s"] = chunkSpawnEntities

            // Calculate level position in world grid
            val levelX: Int = ldtkLevel["worldX"] as Int / levelWidth
            val levelY: Int = ldtkLevel["worldY"] as Int / levelHeight
            val chunkName = ldtkLevel["identifier"] as String

            //println("  Processing '$chunkName' ...")

            // Get chunk number from chunk name
            val chunkNumber = chunkName.substringAfterLast("_").toIntOrNull() ?: error("Chunk name '$chunkName' does not contain a valid chunk number!")

            val levelGridHeight = (ldtkLevel["pxHei"] as Int) / defaultGridSize  // Level height in tiles
            val levelGridWidth = (ldtkLevel["pxWid"] as Int) / defaultGridSize   // Level width in tiles

            // An array containing all Layer instances.
            // **IMPORTANT**: if the project option "*Save levels separately*" is enabled, this field will be `null`.
            // This array is **sorted in display order**: the 1st layer is the top-most and the last is behind.
            val ldtkLevelLayers = ldtkLevel["layerInstances"] as List<Map<String, Any?>>

            // Create stacked tile map data array (width * height) with max 10 stacked tiles per cell
            val stackedTileMapData: List<MutableList<Int>> =  List(levelGridHeight * levelGridWidth) { mutableListOf() }
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
                                // x, y position in pixels
                                val entityPosX: Int = ((ldtkEntity["px"]?.let { it as List<Int> })?.get(0) ?: error("Entity px is null for entity '$entityName' in chunk '$chunkName'!")) + (levelWidth * levelX)
                                val entityPosY: Int = ((ldtkEntity["px"]?.let { it as List<Int> })?.get(1) ?: error("Entity py is null for entity '$entityName' in chunk '$chunkName'!")) + (levelHeight * levelY)
                                // pivot within entity width/height [0..1]
                                val entityPivotX: Float = (ldtkEntity["__pivot"]?.let { it as List<Number> })?.get(0)?.toFloat() ?: error("Entity pivot x is null for entity '$entityName' in chunk '$chunkName'!")
                                val entityPivotY: Float = (ldtkEntity["__pivot"]?.let { it as List<Number> })?.get(1)?.toFloat() ?: error("Entity pivot y is null for entity '$entityName' in chunk '$chunkName'!")

                                // Add position of entity
                                (ldtkEntity["__tags"] as List<String>).firstOrNull { it == "positionable" }?.let {
                                    chunkEntity["x"] = entityPosX
                                    chunkEntity["y"] = entityPosY
                                    chunkEntity["anchorX"] = (entityPivotX * ldtkEntity["width"] as Int).toInt()
                                    chunkEntity["anchorY"] = (entityPivotY * ldtkEntity["height"] as Int).toInt()
                                }
                                // Add all other fields of entity
                                ldtkEntities.forEach { field ->
                                    val fieldName = field["__identifier"] as String
                                    if (fieldName != "entityConfig") {
                                        chunkEntity[fieldName] = field["__value"] ?: error("Entity field '__value' is null for field '$fieldName' in entity '$entityName' of chunk '$chunkName'!")
                                    }
                                }
                                // Add entity to list of game objects which are spawned automatically by the WorldChunkSystem
                                if ((ldtkEntity["__tags"] as List<String>).firstOrNull { it == "gameobject" } != null) {
                                    chunkSpawnEntities.add(gameObjectName)
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
                            val tileSetData = tileSetDataByUid[tilesetDefUid]!!

                            stackTilesIntoTileMap(autoLayerTiles + gridTiles, stackedTileMapData, layerGridWidth, defaultGridSize, clusterIndex, tileSetData)
                        }
                    }
//                    else -> println("WARNING: No tiles or entities found in layer '$layerName'!")
                }
            }

            // chunk 0 is not a valid chunk number and indicates "no" chunk, so we use 0 as default value for non-existing neighboring chunks
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

            // Flatten collision info for each stacked tile list into the first tile entry
            stackedTileMapData.forEach { stackedTiles ->
                stackedTiles.reversed().forEach { tile ->
                    val collision = tile and 0xff00000  // Get bits 20-27 for collision index
                    if (collision != 0) {
                        // Collision found, set collision info in first tile entry and break
                        stackedTiles[0] = stackedTiles[0] or collision  // Set collision info in first tile entry
                        return@forEach
                    }
                }
            }

            if (stackedTileMapPopulated) {
                // We add here the stacked tile map to the chunk info for a layer - so in future we can support multiple layers
                // per chunk and add parallax scrolling factors for each layer if needed. For now, we just use one layer with
                // the name "default" and a scroll factor of 1 (no parallax).
                chunkLevelMap[layerName] = linkedMapOf(
                    "s" to 1f,  // Scroll factor for parallax effect - not supported yet, default is 1 (no parallax)
                    // factor 1f == normal speed, < 1f slower, > 1f faster than normal speed
                    // normal == 64x64 tiles, < 1f ==> 48x48 tiles, > 1f ==> 90x90 tiles
                    // Effectively scroll speed of all world layers shall be aligned so that chunks move together.
                    // Scroll factor is calculated based on layer size in tiles compared to normal tile size of 64x64.
                    // So if a layer has 48x48 tiles, scroll factor is 48/64 = 0.75f, if a layer has 90x90 tiles, scroll factor is 90/64 = 1.4f
                    "m" to stackedTileMapData,
                    "c" to clusterList
                )
            }
            //if (chunkEntities.isNotEmpty()) {
            //    println("  Add '${chunkEntities.size}' entities to chunk JSON file.")
            //}

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

    private fun stackTilesIntoTileMap(
        layerTiles: List<Map<String, Any?>>,
        stackedTileMapData: List<MutableList<Int>>,
        stackedTileMapWidth: Int,
        tileSize: Int,
        clusterIndex: Int,
        tileSetData: TileSetData
    ) {
        for (tile in layerTiles) {
            val (px, py) = tile["px"]?.let {  // Tile x, y position in layer
                it as List<Int>
                if (it.size == 2) it else error("Tile position 'px' does not contain exactly 2 values for x and y position!")
            } ?: error("Tile position 'px' is null in tile set '${tileSetData.tileSetName}' of cluster '${tileSetData.clusterName}'!")
            val x = px / tileSize
            val y = py / tileSize
            val dx = px % tileSize
            val dy = py % tileSize
            // Tile id in the tileset which identifies the tile graphic
            // plus offset which is determined by the tileset order in the cluster
            val rawTileId = tile["t"]?.let { it as Int } ?: error("Tile id 't' is null for tile at position ($px,$py) in " +
                "tile set '${tileSetData.tileSetName}' of cluster '${tileSetData.clusterName}'!")
            //val flipX = (tile["f"] as Int).hasBitSet(0)  -- not supported yet
            //val flipY = (tile["f"] as Int).hasBitSet(1)

            if ((dx != 0 || dy != 0)) println("WARNING: Tile at pixel position ($px,$py) is not aligned to tile size $tileSize " +
                "(dx=$dx, dy=$dy)! This is not supported and tile offset will be ignored!")

            val collision = tileSetData.collisionTiles[rawTileId] ?: 0  // Get collision shape/type for tile if exists, otherwise default to 0 (no collision)

            val tileId = rawTileId + tileSetData.tileIdOffset
            val tileIndex = y * stackedTileMapWidth + x

            // Tile number consists of:
            //  4 bits 0-3: chunk index (16 tilesets per cluster)
            // 16 bits 4-19: tile id (65536 tiles per cluster = 4096 tiles per tileset with 16 tilesets per cluster)
            //  8 bits 20-27: collision info (256 collision types overall)
            //  4 bits 28-31: reserved for future use (e.g. flipX, flipY, animation frame, etc.)
            val tileIdx = (clusterIndex and 0xf) or ((tileId and 0xffff) shl 4) or ((collision and 0xff) shl 20)
            stackedTileMapData[tileIndex].add(tileIdx)
            stackedTileMapPopulated = true
        }
    }
}
