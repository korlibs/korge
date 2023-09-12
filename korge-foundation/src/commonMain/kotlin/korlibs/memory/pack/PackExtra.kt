package korlibs.memory.pack


data class Half8Pack(
    val h0: Float,
    val h1: Float,
    val h2: Float,
    val h3: Float,
    val h4: Float,
    val h5: Float,
    val h6: Float,
    val h7: Float,
)
val Half8Pack.h0: Float get() = this.h0
val Half8Pack.h1: Float get() = this.h1
val Half8Pack.h2: Float get() = this.h2
val Half8Pack.h3: Float get() = this.h3
val Half8Pack.h4: Float get() = this.h4
val Half8Pack.h5: Float get() = this.h5
val Half8Pack.h6: Float get() = this.h6
val Half8Pack.h7: Float get() = this.h7
fun half8PackOf(h0: Float, h1: Float, h2: Float, h3: Float, h4: Float, h5: Float, h6: Float, h7: Float): Half8Pack {
    return Half8Pack(h0, h1, h2, h3, h4, h5, h6, h7)
}


data class BFloat6Pack(
    val f0: Float,
    val f1: Float,
    val f2: Float,
    val f3: Float,
    val f4: Float,
    val f5: Float,
    val twobits: Int
)

val BFloat6Pack.bf0: Float get() = f0
val BFloat6Pack.bf1: Float get() = f1
val BFloat6Pack.bf2: Float get() = f2
val BFloat6Pack.bf3: Float get() = f3
val BFloat6Pack.bf4: Float get() = f4
val BFloat6Pack.bf5: Float get() = f5
val BFloat6Pack.twobits: Int get() = twobits

fun bfloat6PackOf(bf0: Float, bf1: Float, bf2: Float, bf3: Float, bf4: Float, bf5: Float, twobits: Int = 0): BFloat6Pack =
    BFloat6Pack(bf0, bf1, bf2, bf3, bf4, bf5, twobits)

data class BFloat3Half4Pack(
    val f0: Float,
    val f1: Float,
    val f2: Float,
    val f3: Float,
    val f4: Float,
    val f5: Float,
    val f6: Float,
)
// 21-bit BFloat precision
val BFloat3Half4Pack.b0: Float get() = f0
val BFloat3Half4Pack.b1: Float get() = f1
val BFloat3Half4Pack.b2: Float get() = f2
// 16-bit Half Float precision
val BFloat3Half4Pack.hf0: Float get() = f3
val BFloat3Half4Pack.hf1: Float get() = f4
val BFloat3Half4Pack.hf2: Float get() = f5
val BFloat3Half4Pack.hf3: Float get() = f6

fun bfloat3Half4PackOf(
    b0: Float, b1: Float, b2: Float,
    hf0: Float, hf1: Float, hf2: Float, hf3: Float
): BFloat3Half4Pack {
    return BFloat3Half4Pack(b0, b1, b2, hf0, hf1, hf2, hf3)
}

inline class Short4LongPack(val pack: Long) {
    val x: Short get() = (pack ushr 0).toShort()
    val y: Short get() = (pack ushr 16).toShort()
    val z: Short get() = (pack ushr 32).toShort()
    val w: Short get() = (pack ushr 48).toShort()
    constructor(x: Short, y: Short, z: Short, w: Short) : this(
        (x.toUShort().toLong() shl 0) or (y.toUShort().toLong() shl 16) or (z.toUShort().toLong() shl 32) or (w.toUShort().toLong() shl 48)
    )
}
