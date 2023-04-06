package korlibs.math.geom.bezier

import korlibs.datastructure.*

fun Curves.toDashes(pattern: FloatArray?, offset: Float = 0f): List<Curves> {
    if (pattern == null) return listOf(this)

    check(!pattern.all { it <= 0.0 })
    val length = this.length
    var current = offset
    var dashNow = true
    var index = 0
    val out = arrayListOf<Curves>()
    while (current < length) {
        val len = pattern.getCyclic(index++)
        if (dashNow) {
            out += splitByLength(current.toDouble(), (current + len).toDouble())
        }
        current += len
        dashNow = !dashNow
    }
    return out
}
