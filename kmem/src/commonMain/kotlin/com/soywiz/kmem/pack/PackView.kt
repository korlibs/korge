package com.soywiz.kmem.pack

import com.soywiz.kmem.*

inline class Half4Pack private constructor(val data: Short4Pack) {
    constructor(x: Half, y: Half, z: Half, w: Half) : this(short4PackOf(
        x.rawBits.toShort(),
        y.rawBits.toShort(),
        z.rawBits.toShort(),
        w.rawBits.toShort(),
    ))
    val x: Half get() = Half.fromBits(data.s0)
    val y: Half get() = Half.fromBits(data.s1)
    val z: Half get() = Half.fromBits(data.s2)
    val w: Half get() = Half.fromBits(data.s3)
}
