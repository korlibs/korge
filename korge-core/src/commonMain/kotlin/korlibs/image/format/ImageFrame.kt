package korlibs.image.format

import korlibs.datastructure.Extra
import korlibs.datastructure.fastArrayListOf
import korlibs.time.TimeSpan
import korlibs.time.seconds
import korlibs.image.bitmap.*

/**
 * This class defines one frame of a sprite object like e.g. an aseprite image file.
 * It contains info about all layer images which are used in that frame.
 *
 * @param index The index of the frame within the sprite (e.g. aseprite file).
 * @param time  When this frame is used in an animation this defines the time the frame should be displayed.
 * @param layerData This is a list of all layers which this frame contains.
 */
open class ImageFrame(
    val index: Int,
    val time: TimeSpan = 0.seconds,
    val layerData: List<ImageFrameLayer> = emptyList(),
) : Extra by Extra.Mixin() {
    companion object {
        operator fun invoke(
            bitmap: Bitmap,
            time: TimeSpan = 0.seconds,
            targetX: Int = 0,
            targetY: Int = 0,
            main: Boolean = true,
            includeInAtlas: Boolean = true,
            name: String? = null,
            index: Int = 0,
        ): ImageFrame =
            ImageFrame(
                index, time,
                fastArrayListOf(
                    ImageFrameLayer(
                        ImageLayer(0, null, ImageLayer.Type.NORMAL),
                        bitmap.slice(name = name),
                        targetX,
                        targetY,
                        main,
                        includeInAtlas
                    )
                )
            )
    }

    val first: ImageFrameLayer? = layerData.firstOrNull()

    val slice: BmpSlice get() = first?.slice ?: Bitmaps.transparent
    val targetX: Int get() = first?.targetX ?: 0
    val targetY: Int get() = first?.targetY ?: 0
    val main: Boolean get() = first?.main ?: false
    val includeInAtlas: Boolean get() = first?.includeInAtlas ?: true

    val duration: TimeSpan get() = time
    val width: Int get() = slice.width
    val height: Int get() = slice.height
    val area: Int get() = slice.area
    val bitmap: Bitmap get() = first?.bitmap ?: Bitmaps.transparent.bmp
    val name: String? get() = slice.name

    override fun toString(): String = "ImageFrame($slice, time=$time, targetX=$targetX, targetY=$targetY, main=$main)"
}

val Iterable<ImageFrame>.area: Int get() = this.sumOf { it.area }
