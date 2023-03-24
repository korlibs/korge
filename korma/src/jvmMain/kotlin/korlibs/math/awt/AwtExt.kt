package korlibs.math.awt

import korlibs.math.geom.MRectangle
import korlibs.math.geom.MRectangleInt
import korlibs.math.geom.MSize
import java.awt.geom.Rectangle2D

fun MRectangle.toAwt(out: Rectangle2D.Float = Rectangle2D.Float()): Rectangle2D.Float =
    out.also { it.setRect(this.x.toFloat(), this.y.toFloat(), this.width.toFloat(), this.height.toFloat()) }

fun MRectangleInt.toAwt(out: java.awt.Rectangle = java.awt.Rectangle()): java.awt.Rectangle =
    out.also { out.setBounds(this.x, this.y, this.width, this.height) }

fun Rectangle2D.Float.toKorma(out: MRectangle = MRectangle()): MRectangle =
    out.also { it.setTo(this.x, this.y, this.width, this.height) }

fun java.awt.Rectangle.toKorma(out: MRectangleInt = MRectangleInt()): MRectangleInt =
    out.also { out.setTo(this.x, this.y, this.width, this.height) }

fun java.awt.geom.Dimension2D.toKorma(out: MSize = MSize()) =
    out.also { out.setTo(this.width, this.height) }

fun MSize.toAwt(out: java.awt.geom.Dimension2D = java.awt.Dimension()) =
    out.also { out.setSize(this.width, this.height) }