package korge.graphics.backend.metal

import platform.Metal.*

actual class MTLBuffer internal actual constructor(actual val buffer: MTLBufferProtocol) {

    actual fun didModifyFullRange() {
        // Nothing to do on iOS
    }

}
