package korlibs.korge.render

import korlibs.korge.internal.KorgeInternal
import korlibs.image.bitmap.BmpSlice
import korlibs.math.geom.MRectangle

/**
 * A [texture] wrap that includes [scale] and [bounds] information.
 * Used internally for atlases.
 */
@KorgeInternal
data class TextureWithBitmapSlice(
	val texture: BmpSlice,
	val bitmapSlice: BmpSlice,
	val scale: Double,
	val bounds: MRectangle
)