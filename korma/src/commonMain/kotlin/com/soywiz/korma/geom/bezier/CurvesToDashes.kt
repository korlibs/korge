package com.soywiz.korma.geom.bezier

import com.soywiz.kds.getCyclic

fun Curves.toDashes(pattern: DoubleArray?, offset: Double = 0.0): List<Curves> {
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
            out += splitByLength(current, current + len)
        }
        current += len
        dashNow = !dashNow
    }
    return out
}
