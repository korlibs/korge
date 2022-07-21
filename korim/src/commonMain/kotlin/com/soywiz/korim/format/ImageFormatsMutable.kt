package com.soywiz.korim.format

import com.soywiz.kds.atomic.kdsFreeze
import com.soywiz.kds.lock.Lock

class ImageFormatsMutable : ImageFormats() {
    val lock = Lock()

    fun register(vararg formats: ImageFormat) {
        lock { this._formats = kdsFreeze(this._formats + formats) }
    }

    fun unregister(vararg formats: ImageFormat) {
        lock { this._formats = kdsFreeze(this._formats - formats.toSet()) }
    }

    // @TODO: This is not thread-safe, if we call this from two different threads at once strange things might happen
    inline fun <T> temporalRegister(vararg formats: ImageFormat, callback: () -> T): T {
        val oldFormats = lock { this._formats.toSet() }
        try {
            register(*formats)
            return callback()
        } finally {
            lock { this._formats = kdsFreeze(oldFormats) }
        }
    }
}
