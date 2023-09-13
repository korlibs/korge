package korlibs.metal

import kotlinx.cinterop.*
import platform.Foundation.NSMakeRange

actual fun MTLBuffer.didModifyFullRange() {
    buffer.didModifyRange(NSMakeRange(0.convert(), buffer.length))
}
