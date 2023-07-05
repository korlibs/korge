package korlibs.memory.pack

import korlibs.memory.*

inline class Half4Pack private constructor(val data: Short4LongPack) {
    constructor(x: Half, y: Half, z: Half, w: Half) : this(Short4LongPack(
        x.rawBits.toShort(),
        y.rawBits.toShort(),
        z.rawBits.toShort(),
        w.rawBits.toShort(),
    ))
    val x: Half get() = Half.fromBits(data.x)
    val y: Half get() = Half.fromBits(data.y)
    val z: Half get() = Half.fromBits(data.z)
    val w: Half get() = Half.fromBits(data.w)
}
