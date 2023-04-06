package korlibs.math.geom.vector

import korlibs.datastructure.*

data class StrokeInfo(
    val thickness: Float = 1f,
    val pixelHinting: Boolean = false,
    val scaleMode: LineScaleMode = LineScaleMode.NORMAL,
    val startCap: LineCap = LineCap.BUTT,
    val endCap: LineCap = LineCap.BUTT,
    val join: LineJoin = LineJoin.MITER,
    val miterLimit: Float = 20f,
    val dash: IFloatArrayList? = null,
    val dashOffset: Float = 0f
)
