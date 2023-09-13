package korlibs.render

import korlibs.graphics.*

interface DeviceDimensionsProvider {
    val devicePixelRatio: Float get() = 1f
    /** Approximate on iOS */
    val pixelsPerInch: Float get() = (96f * devicePixelRatio)
    /** Approximate on iOS */
    val pixelsPerCm: Float get() = (pixelsPerInch / INCH_TO_CM)

    val pixelsPerLogicalInchRatio: Float get() = (pixelsPerInch / AG.defaultPixelsPerInch)


    // Use this in the debug handler, while allowing people to access raw devicePixelRatio without the noise of window scaling
    // I really dont know if "/" or "*" or right but in my mathematical mind "pixelsPerLogicalInchRatio" must increase and not decrease the scale
    // maybe it is pixelsPerLogicalInchRatio / devicePixelRatio ?
    open val computedPixelRatio: Float get() = devicePixelRatio * pixelsPerLogicalInchRatio

    companion object {
        val INCH_TO_CM = 2.54f
    }

    object DEFAULT : DeviceDimensionsProvider
}
