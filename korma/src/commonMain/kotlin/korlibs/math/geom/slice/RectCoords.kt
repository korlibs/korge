package korlibs.math.geom.slice

data class RectCoords(
    override val tlX: Float, override val tlY: Float,
    override val trX: Float, override val trY: Float,
    override val brX: Float, override val brY: Float,
    override val blX: Float, override val blY: Float,
) : SliceCoords {
    fun flippedX(): RectCoords = transformed(SliceOrientation.ORIGINAL.flippedX())
    fun flippedY(): RectCoords = transformed(SliceOrientation.ORIGINAL.flippedY())
    fun rotatedLeft(offset: Int = +1): RectCoords = transformed(SliceOrientation(rotation = SliceRotation.R0.rotatedLeft(offset)))
    fun rotatedRight(offset: Int = +1): RectCoords = transformed(SliceOrientation(rotation = SliceRotation.R0.rotatedRight(offset)))
}


//inline class RectCoords(val data: FloatArray) {
//    constructor(
//        tlX: Float, tlY: Float,
//        trX: Float, trY: Float,
//        brX: Float, brY: Float,
//        blX: Float, blY: Float,
//    ) : this(floatArrayOf(tlX, tlY, trX, trY, brX, brY, blX, blY))
//
//    init { check(data.size == 8) }
//    fun x(index: Int): Float = data[index * 2]
//    fun y(index: Int): Float = data[index * 2 + 1]
//    val tlX: Float get() = x(0)
//    val tlY: Float get() = y(0)
//    val trX: Float get() = x(1)
//    val trY: Float get() = y(1)
//    val brX: Float get() = x(2)
//    val brY: Float get() = y(2)
//    val blX: Float get() = x(3)
//    val blY: Float get() = y(3)
//
//    fun transform(orientation: ImageOrientation): RBmpCoords {
//        val I = orientation.indices
//        return RBmpCoords(
//            x(I[0]), y(I[0]),
//            x(I[1]), y(I[1]),
//            x(I[2]), y(I[2]),
//            x(I[3]), y(I[3]),
//        )
//    }
//}
