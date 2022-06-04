package com.soywiz.korim.vector

import com.soywiz.kds.Extra
import com.soywiz.korma.geom.vector.IVectorPath
import com.soywiz.korma.geom.vector.VectorPath

data class GraphicsPath(
    val path: VectorPath = VectorPath()
) : IVectorPath by path, SizedDrawable, Extra by Extra.Mixin() {
	override val width: Int get() = this.path.getBounds().width.toInt()
	override val height: Int get() = this.path.getBounds().height.toInt()
	override fun draw(c: Context2d) = c.path(path)
	fun clone() = GraphicsPath(path.clone())
    override fun toString(): String = "GraphicsPath(\"${this.path.toSvgPathString()}\")"
}

fun VectorPath.toGraphicsPath(): GraphicsPath = GraphicsPath(this)
fun VectorPath.draw(c: Context2d) {
    c.path(this)
}
fun Context2d.draw(path: VectorPath) {
    this.path(path)
}
