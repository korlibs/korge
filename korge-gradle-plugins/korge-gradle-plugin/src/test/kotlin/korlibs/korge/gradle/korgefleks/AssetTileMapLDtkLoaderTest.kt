package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.IMAGES
import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.TILE_MAPS
import korlibs.korge.gradle.typedresources.getResourceText
import korlibs.korge.gradle.util.fromJson
import org.junit.Test
import java.io.File


class AssetTileMapLDtkLoaderTest {

    @Test
    fun testTileMapLDtkLoader() {
        val assetInfo = linkedMapOf<String, Any>()
        assetInfo[IMAGES] = linkedMapOf<String, Any>()
        assetInfo[TILE_MAPS] = linkedMapOf<String, Any>()

        val assetLevelMapExporter = AssetLevelMapExporter(
            assetDir = File("."),
            gameResourcesDir = File("."),
            assetInfo = assetInfo,
            amountOfTiles = 1
        )

        // Adjust loading functions for testing
        assetLevelMapExporter.getFile = {
            // Just return the file itself for testing
            File(it)
        }
        assetLevelMapExporter.loadLDtkFile = { filename ->
            // Load LDtk json file from resources
            getResourceText(filename.name).fromJson() as Map<String, Any?>
        }

        val listOfTileSets = listOf(
            "tileset_test_1",
            "tileset_test_2",
            "tileset_test_3"
        )

        assetLevelMapExporter.exportTileMapLDtk(
            filename = "testLevelMap.ldtk",
            levelName = "chunk_2",
            clusterName = "common",
            tileSetList = listOfTileSets
        )



        // Check if tile map object was added to asset info correctly
        // Check if tile sets were exported correctly


    }

    @Test
    fun testLevelMapLDtkLoader() {

        // Define tilesets per cluster asset config
        val tileSetsPerClusterMap = mapOf(
            "common" to listOf(
                "tileset_test_1",
                "tileset_test_5"
            ),
            "intro" to listOf(
                "tileset_test_2",
                "tileset_test_3",
                "tileset_test_4"
            )
        )

    }

}
