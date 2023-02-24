package korge.graphics.backend.metal

import com.soywiz.kmem.*
import kotlinx.cinterop.*
import platform.Metal.*
import platform.posix.*

/**
 * wrap of MTLBufferProtocol to handle specific code related to ios/osx
 * TODO: check if there is a more idiomatic way to do this
 *
 * MTLBufferProtocol is used to store buffer send to GPU when using shaders
 */
expect class MTLBuffer internal constructor(buffer: MTLBufferProtocol) {

    val buffer: MTLBufferProtocol

    fun didModifyFullRange()

}

inline fun MTLBuffer.insert(data: ByteArray) {
    data.usePinned {
        memmove(buffer.contents(), it.startAddressOf, buffer.length)
        didModifyFullRange()
    }
}

fun MTLDeviceProtocol.newBuffer(size: ULong) = (newBufferWithLength(size, MTLResourceStorageModeManaged)
    ?: error("fail to create metal buffer"))
    .let { MTLBuffer(it) }
