package com.soywiz.kgl.internal

internal class CachedInt(initial: Int) {
    var current = initial
    inline operator fun invoke(value: Int, callback: () -> Unit) {
        if (current != value) {
            current = value
            callback()
        }
    }
}

internal class CachedInt2(i1: Int, i2: Int) {
    var c1 = i1
    var c2 = i2
    inline operator fun invoke(i1: Int, i2: Int, callback: () -> Unit) {
        if (c1 != i1 || c2 != i2) {
            c1 = i1
            c2 = i2
            callback()
        }
    }
}

internal class CachedInt4(i1: Int, i2: Int, i3: Int, i4: Int) {
    var c1 = i1
    var c2 = i2
    var c3 = i3
    var c4 = i4
    inline operator fun invoke(i1: Int, i2: Int, i3: Int, i4: Int, callback: () -> Unit) {
        if (c1 != i1 || c2 != i2 || c3 != i3 || c4 != i4) {
            c1 = i1
            c2 = i2
            c3 = i3
            c4 = i4
            callback()
        }
    }
}

internal class CachedFloat(initial: Float) {
    var current = initial
    inline operator fun invoke(value: Float, callback: () -> Unit) {
        if (current != value) {
            current = value
            callback()
        }
    }
}

internal class CachedFloat2(i1: Float, i2: Float) {
    var c1 = i1
    var c2 = i2
    inline operator fun invoke(i1: Float, i2: Float, callback: () -> Unit) {
        if (c1 != i1 || c2 != i2) {
            c1 = i1
            c2 = i2
            callback()
        }
    }
}

//internal class CachedDouble(val initial: Double) {
//    var current = initial
//    inline operator fun invoke(value: Double, callback: () -> Unit) {
//        if (current != value) {
//            current = value
//            callback()
//        }
//    }
//}
//
//internal class CachedObject<T>(val initial: T) {
//    var current = initial
//    inline operator fun invoke(value: T, callback: () -> Unit) {
//        if (current != value) {
//            current = value
//            callback()
//        }
//    }
//}
