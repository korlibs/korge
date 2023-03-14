package com.soywiz.kmem.pack

expect class Float4Pack
expect val Float4Pack.f0: Float
expect val Float4Pack.f1: Float
expect val Float4Pack.f2: Float
expect val Float4Pack.f3: Float
expect fun float4PackOf(f0: Float, f1: Float, f2: Float, f3: Float): Float4Pack

fun Float4Pack.copy(f0: Float = this.f0, f1: Float = this.f1, f2: Float = this.f2, f3: Float = this.f3): Float4Pack =
    float4PackOf(f0, f1, f2, f3)
