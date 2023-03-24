package korlibs.math.geom

import korlibs.math.annotations.KormaMutableApi

@KormaMutableApi
sealed interface IRay {
    val point: MPoint
    val direction: MVector2D
}

@KormaMutableApi
data class MRay(override val point: MPoint, override val direction: MVector2D) : IRay
