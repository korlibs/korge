package korlibs.korge.gradle.korgefleks

import java.io.File


/**
 * Exports tileset images from asset directory to export tileset directory for atlas packing.
 *
 * @param assetDir The directory containing the original asset files.
 * @param exportTilesetDir The directory where exported tileset images will be stored.
 * @param tileSetFiles A mutable list to store references to the exported tileset files.
 */
class AssetTilesetExporter(
    private val assetDir: File,
    private val exportTilesetDir: File,
    private val tileSetFiles: MutableList<File>
) {
    /**
     * Exports single tiles from a png tileset file and stores them in a tileset atlas.
     * Adds exported tiles and tileset images to internal asset info list.
     */
    fun addTilesetImagePng(filename: String) {
        println("Export tileset png file: '${filename}'")

        val tilesetFile = assetDir.resolve(filename)

        // Store tileset image file in export tileset directory for atlas packing (take only file name without path)
        val exportTilesetFile = exportTilesetDir.resolve(tilesetFile.name)

        // Store reference to tileset file for later atlas packing - We need to remember the order of tilesets
        tileSetFiles.add(exportTilesetFile)

        // Make sure export directory exists
        exportTilesetFile.parentFile?.mkdirs()
            ?: error("Could not create export tileset directory: ${exportTilesetDir.path}")

        // Check if file already exists in export directory
        if (exportTilesetFile.exists()) println("ERROR: Tileset file '${exportTilesetFile.name}' already exists in export directory and will be overwritten. Please check if this is intended.")

        // Copy tileset file to export directory
        tilesetFile.copyTo(exportTilesetFile, overwrite = true)

        // AssetTilesetBuilder will cut the tileset into individual tiles and create a "compressed" new tileset atlas of all
        // given tilesets
    }
}
