@file:OptIn(ExperimentalNativeApi::class, ExperimentalNativeApi::class)

package korlibs.memory

import korlibs.memory.arrays.*
import kotlinx.cinterop.*
import kotlin.experimental.*

class NBufferTempAddress {
    val pool = arrayListOf<Pinned<ByteArray>>()
    companion object {
        val ARRAY1 = ByteArray(1)
    }
    fun Buffer.unsafeAddress(): CPointer<ByteVar> {
        val byteArray = this.data.data
        val rbyteArray = if (byteArray.size > 0) byteArray else ARRAY1
        val pin = rbyteArray.pin()
        pool += pin
        return pin.addressOf(this.byteOffset)
    }

    fun start() {
        pool.clear()
    }

    fun dispose() {
        // Kotlin-native: Try to avoid allocating an iterator (lists not optimized yet)
        for (n in 0 until pool.size) pool[n].unpin()
        //for (p in pool) p.unpin()
        pool.clear()
    }

    inline operator fun <T> invoke(callback: NBufferTempAddress.() -> T): T {
        start()
        try {
            return callback()
        } finally {
            dispose()
        }
    }
}
