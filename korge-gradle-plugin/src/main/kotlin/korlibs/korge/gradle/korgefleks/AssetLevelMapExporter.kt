package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.util.LocalSFile
import korlibs.korge.gradle.util.fromJson
import org.gradle.api.GradleException
import java.io.File

class AssetLevelMapExporter(
    private val assetDir: File,
    private val exportTilesDir: File,
    private val assetInfo: LinkedHashMap<String, Any>
) {
    var STACK_SIZE = 10  // Max number of stacked tiles per cell

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

    /**
     * Export a single level map from an LDtk file as tile map object
     *
     * @param filename LDtk filename
     * @param levelName Level name to export
     * @param tileSetsPerClusterMap Map of tileset names per cluster asset. This indicates the order of tilesets in the cluster, which
     *        is important to map tiles correctly from LDtk level layers to the tilesets.
     */
    fun exportTileMapLDtk(filename: String, levelName: String, tileSetsPerClusterMap: Map<String, List<String>>) {
        // Load single level map from LDtk file and export as tile map object
        println("\nLDtk level parser started for exporting single level map from LDtk...")

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
            println("  Exporting tileset '$tileSetName' from path '$tileSetPathName' ...")

            // Find tileset in cluster map and store
            var clusterName = ""
            var offset = 0
            tileSetsPerClusterMap.forEach { (cluster, listOfTileSets) ->
                listOfTileSets.forEachIndexed { idx, tileSet ->
                    if (tileSet == tileSetName) {
                        // Tileset found in this cluster
                        clusterName = cluster
                        offset = idx
                        return@forEach
                    }
                }
            }
            // Sanity checks
            if (clusterName.isEmpty()) throw GradleException("Tileset '$tileSetName' not found in tileset per cluster map!")
            if (clusterLDtkTag != clusterName) throw GradleException("Tileset '$tileSetName' has cluster tag '$clusterLDtkTag' in LDtk file but is assigned to cluster '$clusterName' in tileset per cluster map!")

            tileSetDataByUid[tileSetUid] = TileSetData(tileSetName, offset, clusterName)
        }
        println(tileSetDataByUid)

        // Definition: maximum of 16 layers per level --> Each level chunk can use up to 16 tilesets
        ldtkLevels.forEach { ldtkLevel ->
            val chunkName = ldtkLevel["identifier"] as String
            if (chunkName == levelName) {
                println("Processing '$chunkName' ...")

                val levelGridHeight = (ldtkLevel["pxHei"] as Int) / defaultGridSize  // Level height in tiles
                val levelGridWidth = (ldtkLevel["pxWid"] as Int) / defaultGridSize   // Level width in tiles

                // An array containing all Layer instances.
                // **IMPORTANT**: if the project option "*Save levels separately*" is enabled, this field will be `null`.
                // This array is **sorted in display order**: the 1st layer is the top-most and the last is behind.
                val ldtkLevelLayers = ldtkLevel["layerInstances"] as List<Map<String, Any?>>

                // Create stacked tile map data array (width * height) with max 10 stacked tiles per cell
                val stackedTileMapData: List<MutableList<Int>> = List(levelGridHeight * levelGridWidth) { MutableList(STACK_SIZE) { -1 } }
                stackedTileMapPopulated = false

                val clusterList = mutableListOf<String>()

                // Go through all layers in reverse order (from background to foreground)
                for (layerIdx in ldtkLevelLayers.size - 1 downTo 0) {
                    val ldtkLayer = ldtkLevelLayers[layerIdx]
                    val layerName = ldtkLayer["__identifier"] as String
                    println("  Layer: '$layerName'")
                    val layerGridWidth = ldtkLayer["__cWid"] as Int  // Layer width in grid cells

                    // Layer contains either auto-tiles or grid-tiles, so add
                    val autoLayerTiles = ldtkLayer["autoLayerTiles"] as List<Map<String, Any?>>
                    val gridTiles = ldtkLayer["gridTiles"] as List<Map<String, Any?>>
                    val entityInstances = ldtkLayer["entityInstances"] as List<Map<String, Any?>>

                    when {
                        entityInstances.isNotEmpty() -> {
                            // TODO
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
                        else -> println("WARNING: No tiles or entities found in layer '$layerName'!")
                    }
                }

                if (stackedTileMapPopulated) {
                    // TODO - write stacked tile map data to asset info
                    println("  Stacked tile map data exported for level '$levelName'.")

                    val chunkData = ChunkData(
                        chunkName,
                        emptyList(),
                        stackedTileMapData,
                        clusterList
                    )
                }
                println()
            }
        }
    }

    private fun stackTilesIntoTileMap(layerTiles: List<Map<String, Any?>>, stackedTileMapData: List<MutableList<Int>>, width: Int, tileSize: Int, clusterIndex: Int, tileOffset: Int) {
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

            if (px == 0 && py == 0) {
                println("  0:0 tileId=$tileId")
            }

            val tileIndex = x + y * width
            val stackedTile = stackedTileMapData[tileIndex]

            for ((idx, tile) in stackedTile.withIndex()) {
                if (tile == -1) {
                    // Empty tile found, stack tile here
                    if (idx < STACK_SIZE) {
                        // chunk index in bits 0-3, tile id in bits 4 - 19
                        val tile = (clusterIndex and 0xf) or ((tileId and 0xffff) shl 4)
                        stackedTileMapData[tileIndex][idx] = tile
                        stackedTileMapPopulated = true
                    } else println("ERROR: Stack size exceeded at position ($x,$y)! Max stack size is $STACK_SIZE tiles per cell.")
                    break
                }
            }
        }
    }

    fun exportLevelMapTiled(filename: String) {
        TODO("Not yet implemented")
    }

    fun exportLevelMapLDtk(filename: String, tileSetsPerClusterMap: Map<String, List<String>>) {
        // TODO
    }
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
object KorgeFleksAssets {

    data class TileSetInfo(
        val uid: Int,
        val tileOffset: Int,
        val width: Int,
        val height: Int,
        val tileSize: Int
    )

    private const val STACK_SIZE = 10  // Max number of stacked tiles per cell
    // Because we are stacking tiles from multiple tilesets we need to keep track of the tile offset for each tileset
    private var tileOffset = 0  // Tile offset helper - increases with each tileset processed

    fun parseLDtkLevel() {

        // Set input
        val tilesetNames = listOf("tileset_planet_highlight", "tileset_planet_foreground", "tileset_planet_background")

        println("\nLDtk level parser started...")

        val ldtkFile = File("src/commonMain/resources/world_1/level_1/assets/level_1.ldtk")
        val ldtkJson = ldtkFile.readText().fromJson() as Map<String, Any?>

        val levelWidth: Int = ldtkJson["worldGridWidth"] as Int? ?: 0  // Level width in pixels
        val levelHeight = ldtkJson["worldGridHeight"] as Int? ?: 0     // Level height in pixels
        val ldtkLevels = ldtkJson["levels"] as List<Map<String, Any?>>    // Read all LDtk levels and store them
        val defs = ldtkJson["defs"] as Map<String, Any?>
        val jsonTileSets = defs["tilesets"] as List<Map<String, Any?>>

        val tileSets: Map<Int, TileSetInfo> = tilesetNames.mapNotNull { tilesetName ->
            val tileset = jsonTileSets.firstOrNull { it["identifier"] == tilesetName }
            if (tileset != null) {
                println("Tileset '$tilesetName' (Uid: '${tileset["uid"]}') found in LDtk file.")
                val saveTileOffset = tileOffset
                tileOffset += (tileset["__cWid"] as Int) * (tileset["__cHei"] as Int)
                TileSetInfo(
                    tileset["uid"] as Int,
                    saveTileOffset,
                    tileset["__cWid"] as Int,
                    tileset["__cHei"] as Int,
                    tileset["tileGridSize"] as Int
                )
            } else {
                println("ERROR: Tileset '$tilesetName' not found in LDtk file!")
                null
            }
        }.associateBy { it.uid }

        ldtkLevels.forEach { ldtkLevel ->
            val chunkName = ldtkLevel["identifier"]
            println("Processing '$chunkName' ...")

            // Create YAML string of an entity config from LDtk
            val yamlString = StringBuilder()
            yamlString.append("entities:\n")

            var gameObjectCnt = 0

            // Calculate level position in world grid
            val levelX: Int = ldtkLevel["worldX"] as Int / levelWidth
            val levelY: Int = ldtkLevel["worldY"] as Int / levelHeight
            val levelHeight = ldtkLevel["pxHei"] as Int  // Level height in pixels
            val levelWidth = ldtkLevel["pxWid"] as Int   // Level width in pixels
            val ldtkLevels = ldtkLevel["layerInstances"] as List<Map<String, Any?>>

            // Create stacked tile map data array (width * height) with max 10 stacked tiles per cell
            // TODO remove hardcoded size
            val stackedTileMapData: List<MutableList<Int>> = List(64 * 64) { MutableList(STACK_SIZE) { 0 } }


            ldtkLevels.forEach { ldtkLayer ->
                val layerName = ldtkLayer["__identifier"] as String
                val layerGridWidth = ldtkLayer["__cWid"] as Int  // Layer width in grid cells
                //val layerGridHeight = ldtkLayer["__cHei"] as Int  // Layer height in grid cells
                val tileSize = ldtkLayer["__gridSize"] as Int

                print("  Layer: '$layerName' ... ")


                val tilesetDefUid = ldtkLayer["__tilesetDefUid"] as Int?

                // Store tiles into stacked tile map data array
                if (tilesetDefUid != null && tileSets.containsKey(tilesetDefUid)) {
                    /**
                     * An array containing all tiles generated by Auto-layer rules. The array is already sorted
                     * in display order (ie. 1st tile is beneath 2nd, which is beneath 3rd etc.).<br/><br/>
                     * Note: if multiple tiles are stacked in the same cell as the result of different rules,
                     * all tiles behind opaque ones will be discarded.
                     */
                    val autoLayerTiles = ldtkLayer["autoLayerTiles"] as List<Map<String, Any?>>
                    stackTilesIntoTileMap(autoLayerTiles, stackedTileMapData, layerGridWidth, tileSize)  // TODO - cleanup ldtkWorld.tilesetDefsById[ldtkLayer.tilesetDefUid]!!)

                    // Write stacked tile map data to YAML
                    // TODO
                    println("stacked tile map with ${autoLayerTiles.size} tiles processed.")
                }

                // Put all entity configs into one yaml file per chunk as list with local game object counter
                (ldtkLayer["entityInstances"] as List<Map<String, Any?>>).forEach { ldtkEntity ->
                    val entityName = ldtkEntity["__identifier"]

                    // Load all entity configs by first checking if field 'entityConfig' exists
                    val ldtkEntities = ldtkEntity["fieldInstances"] as List<Map<String, Any?>>
                    if (ldtkEntities.firstOrNull { it["__identifier"] == "entityConfig" } != null) {
                        val gameObjectName = if ((ldtkEntity["__tags"] as List<String>).firstOrNull { it == "unique" } != null) {
                            // Add scripts without unique count value - they are unique by name because they exist only once
                            entityName
                        } else {
                            // Add other game objects with a unique name as identifier
                            "${chunkName}_${entityName}_${gameObjectCnt++}"
                        }
                        yamlString.append("- name: ${gameObjectName}\n")
                        // Add entity config type
                        val entityConfigField = ldtkEntities.first { it["__identifier"] == "entityConfig" }
                        yamlString.append("  entityConfig: ${entityConfigField["__value"]}\n")

                        // Add position of entity = (chunk position in the level) + (position within the chunk) + (pivot point)
                        val entityPosX: Int = (ldtkEntity["px"] as List<Int>)[0] + (levelWidth * levelX)  // x position in pixels
                        val entityPosY: Int = (ldtkEntity["px"] as List<Int>)[1] + (levelHeight * levelY) // y position pixels
                        val entityPivotX: Float = (ldtkEntity["__pivot"] as List<Float>)[0]               // pivot within entity width/height [0..1]
                        val entityPivotY: Float = (ldtkEntity["__pivot"] as List<Float>)[1]

                        // Add position of entity
                        (ldtkEntity["__tags"] as List<String>).firstOrNull { it == "positionable" }?.let {
                            yamlString.append("  x: $entityPosX\n")
                            yamlString.append("  y: $entityPosY\n")
                            yamlString.append("  anchorX: ${entityPivotX * ldtkEntity["width"] as Int}\n")
                            yamlString.append("  anchorY: ${entityPivotY * ldtkEntity["height"] as Int}\n")
                        }

                        // Add all other fields of entity
                        ldtkEntities.forEach { field ->
                            if (field["__identifier"] != "entityConfig") yamlString.append("  ${field["__identifier"]}: ${field["__value"]}\n")
                        }
                        println("INFO: Game object '${ldtkEntity["__identifier"]}' loaded for '$chunkName'")
                        /*
                                                try {
                                                    // By deserializing the YAML string we get an EntityConfig object which itself registers in the EntityFactory
                                                    val entityConfig: EntityConfig =
                                                        assetStore.loader.configSerializer.yaml().decodeFromString(yamlString.toString())

                                                    // We need to store only the name of the entity config for later dynamically spawning of entities
                                                    if (entity.tags.firstOrNull { it == "unique" } == null) {
                                                        // Do not add unique entities to the list of entities - they are spawn separately
                                                        entities.add(entityConfig.name)
                                                    }
                                                    //println("INFO: Registering entity config '${entity.identifier}' for '$levelName'")
                                                } catch (e: Throwable) {
                                                    println("ERROR: Loading entity config - $e")
                                                }

                        */

                    } else println("ERROR: Game object with name '${entityName}' has no field 'entityConfig'!")
                }
            }
//            println("\n$yamlString")

            val yamlLevelMapString = StringBuilder()
            yamlLevelMapString.append("tileMap:\n- ")
            stackedTileMapData.forEachIndexed { idx, stackedTiles ->
                if (idx != 0) yamlLevelMapString.append(",")
                for ((idx, tile) in stackedTiles.withIndex()) {
                    if (idx == 0) yamlLevelMapString.append("[${tile}")
                    else if (tile != 0) yamlLevelMapString.append(",${tile}")
                }
                yamlLevelMapString.append("]")
            }

            val entityYamlFile = File("src/commonMain/resources/world_1/level_1/assets/level_data/${chunkName}.yaml")
            entityYamlFile.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) error("Failed to create directory: ${parent.path}")
                entityYamlFile.writeText(yamlString.toString())
                entityYamlFile.appendText(yamlLevelMapString.toString())
            }
        }
    }

    private fun stackTilesIntoTileMap(autoLayerTiles: List<Map<String, Any?>>, stackedTileMapData: List<MutableList<Int>>, width: Int, tileSize: Int) {  //, tilesetDef: Map<String, Any?>) {
        for (tile in autoLayerTiles) {
            val (px, py) = tile["px"] as List<Int>  // Tile x, y position in layer
            val x = px / tileSize
            val y = py / tileSize
            val dx = px % tileSize
            val dy = py % tileSize
            val tileId = tile["t"] as Int // Tile id in the tileset which identifies the tile graphic
            //val flipX = (tile["f"] as Int).hasBitSet(0)
            //val flipY = (tile["f"] as Int).hasBitSet(1)

            if ((dx != 0 || dy != 0)) {
                println("WARNING: Tile at pixel position ($px,$py) is not aligned to tile size $tileSize (dx=$dx, dy=$dy)! This is currently not supported and tile offset will be ignored!")
            }

            val tileIndex = x + y * width
            val stackedTile = stackedTileMapData[tileIndex]

            for ((idx, tile) in stackedTile.withIndex()) {
                if (tile == 0) {
                    // Empty tile found, stack tile here
                    if (idx < STACK_SIZE) stackedTileMapData[tileIndex][idx] = tileId
                    else println("ERROR: Stack size exceeded at position ($x,$y)! Max stack size is $STACK_SIZE tiles per cell.")
                    break
                }
            }
        }
    }
}

/** Check if a specific bit at [index] is set */
fun Int.hasBitSet(index: Int): Boolean = ((this ushr index) and 1) != 0
