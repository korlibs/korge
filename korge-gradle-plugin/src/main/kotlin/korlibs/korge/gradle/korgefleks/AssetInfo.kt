package korlibs.korge.gradle.korgefleks


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
        var frame: Frame = Frame( 0, 0, 0, 0, 0),
        var targetX: Int = 0,  // offset from the top-left corner of the original sprite if cropped
        var targetY: Int = 0,
        var duration: Float = 0f  // frame duration in milliseconds
    )

    data class AssetInfoNinePatch(
        val frame: Frame  // not cropped
    )

    data class AssetInfoPixelFonts(
        val frame: Frame  // not cropped
    )

    data class Frame(
        val index: Int,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )
}
