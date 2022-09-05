package com.soywiz.korge3d

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korev.EventResult
import com.soywiz.korge.component.Component
import com.soywiz.korge.component.ComponentType

@Korge3DExperimental
open class Container3D : View3D() {
	val children = arrayListOf<View3D>()

    inline fun fastForEachChild(block: (View3D) -> Unit) {
        children.fastForEach(block)
    }

    fun removeChild(child: View3D) {
		children.remove(child)
        __updateChildListenerCount(child, add = false)
	}

	fun addChild(child: View3D) {
		child.removeFromParent()
		children += child
		child._parent = this
		child.transform.parent = this.transform
        __updateChildListenerCount(child, add = true)
	}

	operator fun plusAssign(child: View3D) = addChild(child)

	override fun render(ctx: RenderContext3D) {
        fastForEachChild {
			it.render(ctx)
		}
	}

    //override fun <T : TEvent<T>> dispatchChildren(type: EventType<T>, event: T, result: EventResult?) {
    //    // @TODO: What if we mutate the list now
    //    fastForEachChild {
    //        if (it.onEventCount(type) > 0) it.dispatch(type, event, result)
    //    }
    //}

    override fun <T : Component> getComponentOfTypeRecursiveChildren(type: ComponentType<T>, out: FastArrayList<T>, results: EventResult?) {
        fastForEachChild {
            val childEventListenerCount = it.getComponentCountInDescendants(type)
            if (childEventListenerCount > 0) {
                it.getComponentOfTypeRecursive(type, out, results)
            }
        }
    }
}
