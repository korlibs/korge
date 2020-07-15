package com.soywiz.korma.awt

import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.setTo
import java.awt.geom.Rectangle2D

fun Rectangle.toAwt(out: Rectangle2D.Float = Rectangle2D.Float()): Rectangle2D.Float =
    out.also { it.setRect(this.x.toFloat(), this.y.toFloat(), this.width.toFloat(), this.height.toFloat()) }

fun RectangleInt.toAwt(out: java.awt.Rectangle = java.awt.Rectangle()): java.awt.Rectangle =
    out.also { out.setBounds(this.x, this.y, this.width, this.height) }

fun Rectangle2D.Float.toKorma(out: Rectangle = Rectangle()): Rectangle =
    out.also { it.setTo(this.x, this.y, this.width, this.height) }

fun java.awt.Rectangle.toKorma(out: RectangleInt = RectangleInt()): RectangleInt =
    out.also { out.setTo(this.x, this.y, this.width, this.height) }

fun java.awt.geom.Dimension2D.toKorma(out: Size = Size()) =
    out.also { out.setTo(this.width, this.height) }

fun Size.toAwt(out: java.awt.geom.Dimension2D = java.awt.Dimension()) =
    out.also { out.setSize(this.width, this.height) }
