package korlibs.math.awt

import korlibs.math.geom.*
import java.awt.geom.Rectangle2D

fun Rectangle.toAwt(out: Rectangle2D.Float = Rectangle2D.Float()): Rectangle2D.Float =
    out.also { it.setRect(this.x, this.y, this.width, this.height) }

fun RectangleInt.toAwt(out: java.awt.Rectangle = java.awt.Rectangle()): java.awt.Rectangle =
    out.also { out.setBounds(this.x, this.y, this.width, this.height) }

fun Rectangle2D.Float.toKorma(out: MRectangle = MRectangle()): MRectangle =
    out.also { it.setTo(this.x, this.y, this.width, this.height) }

fun java.awt.Rectangle.toKorma(out: MRectangleInt = MRectangleInt()): MRectangleInt =
    out.also { out.setTo(this.x, this.y, this.width, this.height) }

fun java.awt.geom.Dimension2D.toKorma(): Size =
    Size(this.width, this.height)

fun Size.toAwt(out: java.awt.geom.Dimension2D = java.awt.Dimension()) =
    out.also { out.setSize(this.width.toDouble(), this.height.toDouble()) }
