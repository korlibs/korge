package com.soywiz.kmem.pack

expect class Float4Pack
expect val Float4Pack.x: Float
expect val Float4Pack.y: Float
expect val Float4Pack.z: Float
expect val Float4Pack.w: Float
expect fun float4PackOf(x: Float, y: Float, z: Float, w: Float): Float4Pack

fun Float4Pack.copy(x: Float = this.x, y: Float = this.y, z: Float = this.z, w: Float = this.w): Float4Pack = float4PackOf(x, y, z, w)
