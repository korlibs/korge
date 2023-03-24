package korlibs.render

import korlibs.graphics.*

interface DeviceDimensionsProvider {
    val devicePixelRatio: Double get() = 1.0
    /** Approximate on iOS */
    val pixelsPerInch: Double get() = 96.0 * devicePixelRatio
    /** Approximate on iOS */
    val pixelsPerCm: Double get() = pixelsPerInch / INCH_TO_CM

    val pixelsPerLogicalInchRatio: Double get() = pixelsPerInch / AG.defaultPixelsPerInch


    // Use this in the debug handler, while allowing people to access raw devicePixelRatio without the noise of window scaling
    // I really dont know if "/" or "*" or right but in my mathematical mind "pixelsPerLogicalInchRatio" must increase and not decrease the scale
    // maybe it is pixelsPerLogicalInchRatio / devicePixelRatio ?
    open val computedPixelRatio: Double get() = devicePixelRatio * pixelsPerLogicalInchRatio

    companion object {
        val INCH_TO_CM = 2.54
    }

    object DEFAULT : DeviceDimensionsProvider
}