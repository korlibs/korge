package com.soywiz.korge3d

import com.soywiz.kds.iterators.fastForEach

@Korge3DExperimental
open class Container3D : View3D() {
	val children = arrayListOf<View3D>()

	fun removeChild(child: View3D) {
		children.remove(child)
	}

	fun addChild(child: View3D) {
		child.removeFromParent()
		children += child
		child._parent = this
		child.transform.parent = this.transform
	}

	operator fun plusAssign(child: View3D) = addChild(child)

	override fun render(ctx: RenderContext3D) {
		children.fastForEach {
			it.render(ctx)
		}
	}
}
