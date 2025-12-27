package korlibs.korge.gradle.korgefleks

import java.io.File


/**
 * Exports tileset images from asset directory to export tileset directory for atlas packing.
 *
 * @param assetDir The directory containing the original asset files.
 * @param exportTilesetDir The directory where exported tileset images will be stored.
 */
class AssetTilesetExporter(
    private val assetDir: File,
    private val exportTilesetDir: File
) {
    /**
     * Exports single tiles from a png tileset file and stores them in a tileset atlas.
     * Adds exported tiles and tileset images to internal asset info list.
     */
    fun addTilesetImagePng(filename: String) {
        val tilesetFile = assetDir.resolve(filename)

        // Store tileset image file in export tileset directory for atlas packing (take only file name without path)
        val exportTilesetFile = exportTilesetDir.resolve(tilesetFile.name)

        // Make sure export directory exists
        exportTilesetFile.parentFile?.let { it.mkdirs() }
            ?: error("Could not create export tileset directory: ${exportTilesetDir.path}")

        // Check if file already exists in export directory
        if (exportTilesetFile.exists()) println("ERROR: Tileset file '${exportTilesetFile.name}' already exists in export directory and will be overwritten. Please check if this is intended.")

        // Copy tileset file to export directory
        tilesetFile.copyTo(exportTilesetFile, overwrite = true)

        // AssetTilesetBuilder will cut the tileset into individual tiles and create a "compressed" new tileset atlas of all
        // given tilesets
    }
}
