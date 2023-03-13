package com.soywiz.kmem.pack

import com.soywiz.kmem.*

internal data class JsHalf4Pack(val x: Half, val y: Half, val z: Half, val w: Half)
internal actual inline fun packHalf4(x: Half, y: Half, z: Half, w: Half): Long = JsHalf4Pack(x, y, z, w).asDynamic()
internal actual inline fun unpackHalf4X(v: Long): Half = v.unsafeCast<JsHalf4Pack>().x
internal actual inline fun unpackHalf4Y(v: Long): Half = v.unsafeCast<JsHalf4Pack>().y
internal actual inline fun unpackHalf4Z(v: Long): Half = v.unsafeCast<JsHalf4Pack>().z
internal actual inline fun unpackHalf4W(v: Long): Half = v.unsafeCast<JsHalf4Pack>().w
