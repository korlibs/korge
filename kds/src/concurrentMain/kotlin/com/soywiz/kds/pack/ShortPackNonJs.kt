package com.soywiz.kds.pack

import com.soywiz.kds.internal.*
import com.soywiz.kds.internal.packShort2

internal actual inline fun packShort4(x: Short, y: Short, z: Short, w: Short): Long =
    packInt2(packShort2(x, y), packShort2(z, w))
internal actual inline fun unpackShort4X(v: Long): Short = unpackInt2X(v).toShort()
internal actual inline fun unpackShort4Y(v: Long): Short = (unpackInt2X(v) shr 16).toShort()
internal actual inline fun unpackShort4Z(v: Long): Short = unpackInt2Y(v).toShort()
internal actual inline fun unpackShort4W(v: Long): Short = (unpackInt2Y(v) shr 16).toShort()
