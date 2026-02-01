package korlibs.korge.gradle.korgefleks

import korlibs.korge.gradle.korgefleks.AssetConfig.Companion.IMAGES
import korlibs.korge.gradle.typedresources.getResourceText
import korlibs.korge.gradle.util.fromJson
import org.junit.Test
import java.io.File


class AssetTileMapLDtkLoaderTest {

    @Test
    fun testTileMapLDtkLoader() {
        val assetInfo = linkedMapOf<String, Any>()
        assetInfo[IMAGES] = linkedMapOf<String, Any>()

        val assetLevelMapExporter = AssetLevelMapExporter(
            assetDir = File("."),
            exportTilesDir = File("."),
            assetInfo = assetInfo
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

        // Define tilesets per cluster asset config
        val tileSetsPerClusterMap = mapOf(
            "common" to listOf(
                "tileset_test_1",
                "tileset_test_5"
            ),
            "intro" to listOf(
                "tileset_name_2",
                "tileset_name_3",
                "tileset_name_4"
            )
        )

        assetLevelMapExporter.exportTileMapLDtk(
            filename = "testLevelMap.ldtk",
            levelName = "chunk_0",
            tileSetsPerClusterMap = tileSetsPerClusterMap
        )

        // Check if tile map object was added to asset info correctly
        // Check if tile sets were exported correctly


    }
}
