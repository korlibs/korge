package korlibs.metal

import platform.Foundation.NSMakeRange

actual fun MTLBuffer.didModifyFullRange() {
    buffer.didModifyRange(NSMakeRange(0, buffer.length))
}
