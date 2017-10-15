package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.numeric.niceStr
import kotlin.reflect.KClass

open class Container(views: Views) : View(views) {
	val children = arrayListOf<View>()

	fun removeChildren() {
		for (child in children) {
			child.parent = null
			child.index = -1
		}
		children.clear()
	}

	fun addChild(view: View) = this.plusAssign(view)

	override fun invalidate() {
		validGlobal = false
		for (child in children) {
			if (!child.validGlobal) continue
			child.validGlobal = false
			child.invalidate()
		}
	}

	operator fun plusAssign(view: View) {
		view.removeFromParent()
		view.index = children.size
		children += view
		view.parent = this
		view.invalidate()
	}

	operator fun minusAssign(view: View) {
		if (view.parent == this) view.removeFromParent()
	}

	private val tempMatrix = Matrix2d()
	override fun render(ctx: RenderContext, m: Matrix2d) {
		if (!visible) return
		val isGlobal = (m === globalMatrix)
		safeForEachChildren { child ->
			if (isGlobal) {
				child.render(ctx, child.globalMatrix)
			} else {
				tempMatrix.multiply(child.localMatrix, m)
				child.render(ctx, tempMatrix)
			}
		}
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		for (child in children.reversed().filter(View::visible)) return child.hitTest(x, y) ?: continue
		return null
	}

	override fun hitTestBoundingInternal(x: Double, y: Double): View? {
		for (child in children.reversed().filter(View::visible)) return child.hitTestBounding(x, y) ?: continue
		return null
	}

	private val bb = BoundsBuilder()
	private val tempRect = Rectangle()

	override fun getLocalBoundsInternal(out: Rectangle) {
		bb.reset()
		safeForEachChildren { child ->
			child.getBounds(child, tempRect)
			bb.add(tempRect)
		}
		bb.getBounds(out)
	}

	override fun updateInternal(dtMs: Int) {
		super.updateInternal(dtMs)
		safeForEachChildren { child ->
			child.update(dtMs)
		}
		//for (child in children.toList()) child.update(dtMs)
	}

	override fun <T : Any> dispatch(event: T, clazz: KClass<out T>) {
		super.dispatch(event, clazz)
		safeForEachChildren { child ->
			child.dispatch(event, clazz)
		}
	}

	inline protected fun safeForEachChildren(crossinline callback: (View) -> Unit) {
		var n = 0
		while (n < children.size) {
			callback(children[n])
			n++
		}
	}

	inline protected fun safeForEachChildrenReversed(crossinline callback: (View) -> Unit) {
		var n = 0
		while (n < children.size) {
			callback(children[children.size - n - 1])
			n++
		}
	}

	override fun createInstance(): View = Container(views)

	override fun clone(): View {
		val out = super.clone()
		for (child in children) out += child.clone()
		return out
	}
}

open class FixedSizeContainer(views: Views, override var width: Double = 100.0, override var height: Double = 100.0) : Container(views) {
	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(0, 0, width, height)
	}

	override fun toString(): String {
		var out = super.toString()
		out += ":size=(${width.niceStr}x${height.niceStr})"
		return out
	}
}

operator fun View?.plusAssign(view: View?) {
	val container = this as? Container?
	if (view != null) container?.addChild(view)
}
