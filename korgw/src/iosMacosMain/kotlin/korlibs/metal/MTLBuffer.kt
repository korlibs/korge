package korlibs.metal

import korlibs.memory.*
import kotlinx.cinterop.*
import platform.Metal.*
import platform.posix.*

/**
 * wrap of MTLBufferProtocol to handle specific code related to ios/osx
 * TODO: check if there is a more idiomatic way to do this
 *
 * MTLBufferProtocol is used to store buffer send to GPU when using shaders
 */
value class MTLBuffer internal constructor(val buffer: MTLBufferProtocol)

expect fun MTLBuffer.didModifyFullRange()

inline fun MTLBuffer.insert(data: ByteArray) {
    data.usePinned {
        memmove(buffer.contents(), it.startAddressOf, buffer.length)
        didModifyFullRange()
    }
}

/**
 * TODO: may not working on ios "In iOS and tvOS, the managed storage mode is not available."
 * https://developer.apple.com/documentation/metal/mtlstoragemode/managed
 */
fun MTLDeviceProtocol.newBuffer(size: ULong) = (newBufferWithLength(size, MTLResourceStorageModeManaged)
    ?: error("fail to create metal buffer"))
    .let { MTLBuffer(it) }
