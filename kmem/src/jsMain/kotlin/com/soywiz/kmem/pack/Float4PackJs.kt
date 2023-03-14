package com.soywiz.kmem.pack

actual data class Float4Pack(var x: Float, var y: Float, var z: Float, var w: Float)
actual val Float4Pack.x: Float get() = this.x
actual val Float4Pack.y: Float get() = this.y
actual val Float4Pack.z: Float get() = this.z
actual val Float4Pack.w: Float get() = this.w
actual fun float4PackOf(x: Float, y: Float, z: Float, w: Float): Float4Pack = Float4Pack(x, y, z, w)
