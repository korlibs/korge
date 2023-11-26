package korlibs.image.color

import java.awt.*

fun RGBA.toAwt(): Color = Color(r, g, b, a)
fun Color.toRgba(): RGBA = RGBA(red, green, blue, alpha)
