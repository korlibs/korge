package korlibs.image.bitmap

import korlibs.image.color.RgbaArray

open class Palette(
    val colors: RgbaArray,
    val names: Array<String?>? = null,
    val changeStart: Int = 0,
    val changeEnd: Int = 0
) {
    override fun toString(): String = buildString {
        append("Palette(")
        for (n in 0 until colors.size) {
            val color = colors[n]
            val name = names?.get(n)
            if (n > 0) append(", ")
            if (name != null) {
                append(name)
                append(": ")
            }
            append(color.toString())
        }
        append(")")
    }
}
