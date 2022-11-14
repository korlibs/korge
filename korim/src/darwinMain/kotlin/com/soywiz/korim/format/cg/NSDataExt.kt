package com.soywiz.korim.format.cg

import com.soywiz.kmem.*
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.*

fun NSData.toByteArray(): ByteArray = ByteArray(this.length.toInt()).also { bytes ->
    bytes.usePinned { bytesPin ->
        memcpy(bytesPin.startAddressOf, this.bytes, this.length)
    }
}
