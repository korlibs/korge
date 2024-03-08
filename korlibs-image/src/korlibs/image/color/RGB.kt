package korlibs.image.color

import korlibs.memory.extract8
import korlibs.memory.insert8

open class RGB(val rOffset: Int, val gOffset: Int, val bOffset: Int) : ColorFormat24() {
	override fun getR(v: Int): Int = v.extract8(rOffset)
	override fun getG(v: Int): Int = v.extract8(gOffset)
	override fun getB(v: Int): Int = v.extract8(bOffset)
	override fun getA(v: Int): Int = 0xFF

	override fun pack(r: Int, g: Int, b: Int, a: Int): Int = 0.insert8(r, rOffset).insert8(g, gOffset).insert8(b, bOffset)

	companion object : RGB(rOffset = 0, gOffset = 8, bOffset = 16)
}

object BGR : RGB(rOffset = 16, gOffset = 8, bOffset = 0)
