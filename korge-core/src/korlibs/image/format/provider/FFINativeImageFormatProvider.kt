package korlibs.image.format.provider

import korlibs.image.format.*
import korlibs.platform.*

val FFINativeImageFormatProviderOpt: BaseNativeImageFormatProvider? by lazy {
    when {
        Platform.isMac -> FFICoreGraphicsImageFormatProvider
        Platform.isWindows -> FFIGdiNativeImageFormatProvider
        Platform.isLinux -> FFISDLImageNativeImageFormatProvider
        else -> null
    }
}

val FFINativeImageFormatProvider get() = FFINativeImageFormatProviderOpt
    ?: error("Unsupported platform '${Platform.os}' for FFINativeImageFormatProvider")
