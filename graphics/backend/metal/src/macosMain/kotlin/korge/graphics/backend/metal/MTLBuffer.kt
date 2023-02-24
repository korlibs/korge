package korge.graphics.backend.metal

import platform.Foundation.*
import platform.Metal.*

actual class MTLBuffer internal actual constructor(actual val buffer: MTLBufferProtocol) {
    actual fun didModifyFullRange() {
        buffer.didModifyRange(NSMakeRange(0, buffer.length))
    }
}
