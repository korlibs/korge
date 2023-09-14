package korlibs.metal

import korlibs.logger.*
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

private val logger by lazy { Logger("MTLBuffer") }

expect fun MTLBuffer.didModifyFullRange()

fun MTLBuffer.insert(data: ByteArray) {
    data.usePinned {
        logger.debug { "will insert ByteArray of size ${data.size} into a buffer of size ${buffer.length}" }
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
