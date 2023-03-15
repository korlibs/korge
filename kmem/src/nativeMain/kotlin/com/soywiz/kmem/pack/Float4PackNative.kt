package com.soywiz.kmem.pack

actual typealias Float4Pack = Vector128
actual val Float4Pack.f0: Float get() = this.getFloatAt(0)
actual val Float4Pack.f1: Float get() = this.getFloatAt(1)
actual val Float4Pack.f2: Float get() = this.getFloatAt(2)
actual val Float4Pack.f3: Float get() = this.getFloatAt(3)
actual fun float4PackOf(f0: Float, f1: Float, f2: Float, f3: Float): Float4Pack = vectorOf(f0, f1, f2, f3)

/*
actual data class Float4Pack(val x: Float, val y: Float, val z: Float, val w: Float)
actual val Float4Pack.f0: Float get() = this.x
actual val Float4Pack.f1: Float get() = this.y
actual val Float4Pack.f2: Float get() = this.z
actual val Float4Pack.f3: Float get() = this.w
actual fun float4PackOf(f0: Float, f1: Float, f2: Float, f3: Float): Float4Pack = Float4Pack(f0, f1, f2, f3)
*/
