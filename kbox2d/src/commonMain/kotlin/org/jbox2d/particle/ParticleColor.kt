package org.jbox2d.particle

import org.jbox2d.common.Color3f

/**
 * Small color object for each particle
 *
 * @author dmurph
 */
class ParticleColor {

    var r: Byte = 0

    var g: Byte = 0

    var b: Byte = 0

    var a: Byte = 0

    val isZero: Boolean
        get() = r.toInt() == 0 && g.toInt() == 0 && b.toInt() == 0 && a.toInt() == 0

    constructor() {
        r = 127.toByte()
        g = 127.toByte()
        b = 127.toByte()
        a = 50.toByte()
    }

    constructor(r: Byte, g: Byte, b: Byte, a: Byte) {
        set(r, g, b, a)
    }

    constructor(color: Color3f) {
        set(color)
    }

    fun set(color: Color3f) {
        r = (255 * color.x).toInt().toByte()
        g = (255 * color.y).toInt().toByte()
        b = (255 * color.z).toInt().toByte()
        a = 255.toByte()
    }

    fun set(color: ParticleColor) {
        r = color.r
        g = color.g
        b = color.b
        a = color.a
    }

    fun set(r: Byte, g: Byte, b: Byte, a: Byte) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }
}
