package com.soywiz.kmem.pack

actual typealias Float4Pack = Vector128

actual val Float4Pack.x: Float get() = this.getFloatAt(0)
actual val Float4Pack.y: Float get() = this.getFloatAt(1)
actual val Float4Pack.z: Float get() = this.getFloatAt(2)
actual val Float4Pack.w: Float get() = this.getFloatAt(3)
actual fun float4PackOf(x: Float, y: Float, z: Float, w: Float): Float4Pack = vectorOf(x, y, z, w)
