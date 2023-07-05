package korlibs.memory.pack

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

data class Float2(val x: Float, val y: Float)
@Deprecated("") val Float2.f0: Float get() = x
@Deprecated("") val Float2.f1: Float get() = y
@Deprecated("") fun float2PackOf(f0: Float, f1: Float): Float2 = Float2(f0, f1)
@Deprecated("") typealias Float2Pack = Float2

data class Float3(val x: Float, val y: Float, val z: Float)

data class Float4(val x: Float, val y: Float, val z: Float, val w: Float)
@Deprecated("") val Float4.f0: Float get() = this.x
@Deprecated("") val Float4.f1: Float get() = this.y
@Deprecated("") val Float4.f2: Float get() = this.z
@Deprecated("") val Float4.f3: Float get() = this.w
@Deprecated("") fun float4PackOf(f0: Float, f1: Float, f2: Float, f3: Float): Float4 = Float4(f0, f1, f2, f3)
@Deprecated("") typealias Float4Pack = Float4

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

data class Int2(val x: Int, val y: Int)
@Deprecated("") val Int2.i0: Int get() = x
@Deprecated("") val Int2.i1: Int get() = y
@Deprecated("") fun int2PackOf(i0: Int, i1: Int): Int2 = Int2(i0, i1)
@Deprecated("") typealias Int2Pack = Int2

data class Int3(val x: Int, val y: Int, val z: Int)

data class Int4(val x: Int, val y: Int, val z: Int, val w: Int)
@Deprecated("") val Int4.i0: Int get() = this.x
@Deprecated("") val Int4.i1: Int get() = this.y
@Deprecated("") val Int4.i2: Int get() = this.z
@Deprecated("") val Int4.i3: Int get() = this.w
@Deprecated("") fun int4PackOf(i0: Int, i1: Int, i2: Int, i3: Int): Int4 = Int4(i0, i1, i2, i3)
@Deprecated("") typealias Int4Pack = Int4

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

data class Short2(val x: Short, val y: Short)

data class Short3(val x: Short, val y: Short, val z: Short)

//inline class Float2Pack(val data: Long)
//val Float2Pack.f0: Float get() = Float.fromBits(data.low)
//val Float2Pack.f1: Float get() = Float.fromBits(data.high)
//fun float2PackOf(f0: Float, f1: Float): Float2Pack = Float2Pack(Long.fromLowHigh(f0.toRawBits(), f1.toRawBits()))

data class Short4(val x: Short, val y: Short, val z: Short, val w: Short)
@Deprecated("") val Short4.s0: Short get() = x
@Deprecated("") val Short4.s1: Short get() = y
@Deprecated("") val Short4.s2: Short get() = z
@Deprecated("") val Short4.s3: Short get() = w
@Deprecated("") fun short4PackOf(s0: Short, s1: Short, s2: Short, s3: Short): Short4 = Short4(s0, s1, s2, s3)
@Deprecated("") typealias Short4Pack = Short4
