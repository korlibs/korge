package com.soywiz.korim.vector

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.Extra
import com.soywiz.kds.IntArrayList
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.Winding

open class GraphicsPath(
	commands: IntArrayList = IntArrayList(),
	data: DoubleArrayList = DoubleArrayList(),
	winding: Winding = Winding.EVEN_ODD
) : VectorPath(commands, data, winding), SizedDrawable, Extra by Extra.Mixin() {
	override val width: Int get() = this.getBounds().width.toInt()
	override val height: Int get() = this.getBounds().height.toInt()
	override fun draw(c: Context2d) = c.path(this)
	override fun clone() = GraphicsPath(IntArrayList(commands), DoubleArrayList(data), winding)
    override fun toString(): String = "GraphicsPath(\"${this.toSvgPathString()}\")"
}

fun VectorPath.toGraphicsPath() = GraphicsPath(commands, data, winding)
