package korlibs.korge.gradle.korgefleks

import java.awt.Rectangle


data class AssetInfo(
    val images: MutableMap<String, ImageFrames> = mutableMapOf(),
    val ninePatches: MutableMap<String, MutableList<AssetInfoNinePatch>> = mutableMapOf(),
    val pixelFonts: MutableMap<String, MutableList<AssetInfoPixelFonts>> = mutableMapOf()
) {
    data class ImageFrames(
        val frames: MutableList<ImageFrame> = mutableListOf(),
        var width: Int = 0,  // virtual size of the sprite - can be different from frame.width
        var height: Int = 0  // and frame.height if cropped)
    )

    data class ImageFrame(
        var frame: Rectangle = Rectangle(0, 0, 0, 0),
        var targetX: Int = 0,  // offset from the top-left corner of the original sprite if cropped
        var targetY: Int = 0,
        var duration: Float = 0f  // frame duration in milliseconds
    )

    data class AssetInfoNinePatch(
        val frame: Rectangle  // not cropped
    )

    data class AssetInfoPixelFonts(
        val frame: Rectangle  // not cropped
    )
}
