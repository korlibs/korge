package korlibs.math.geom

import korlibs.math.annotations.KormaMutableApi

@KormaMutableApi
inline class MVector4Int(val v: MVector4) {
    var x: Int get() = v.x.toInt(); set(value) { v.x = value.toFloat() }
    var y: Int get() = v.y.toInt(); set(value) { v.y = value.toFloat() }
    var z: Int get() = v.z.toInt(); set(value) { v.z = value.toFloat() }
    var w: Int get() = v.w.toInt(); set(value) { v.w = value.toFloat() }
}
