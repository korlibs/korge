package com.soywiz.korge.view

import com.soywiz.kmem.*
import com.soywiz.korag.AG
import com.soywiz.korge.render.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.*
import com.soywiz.korev.*
import com.soywiz.korge.internal.fastForEach
import com.soywiz.korge.internal.fastForEachReverse
import kotlin.reflect.*

inline fun Container.container(callback: @ViewsDslMarker Container.() -> Unit = {}) =
	Container().addTo(this).apply(callback)

// For Flash compatibility
//open class Sprite : Container()

/**
 * A simple container of [View]s.
 */
open class Container : View() {
	override val isContainer get() = false

	val children = arrayListOf<View>()

	/**
	 * Recursively retrieves the top ancestor in the container hierarchy.
	 *
	 * Retrieves the top ancestor of the hierarchy. In case the container is orphan this very instance is returned.
	 */
	val containerRoot: Container get() = parent?.containerRoot ?: this

	// @TODO: Untested
	/**
	 * Swaps the order of two child [View]s.
	 */
	fun swapChildren(view1: View, view2: View) {
		if (view1.parent == view2.parent && view1.parent == this) {
			val index1 = view1.index
			val index2 = view2.index
			children[index1] = view2.apply { index = index1 }
			children[index2] = view1.apply { index = index2 }
		}
	}

	// @TODO: Untested
	/**
	 * Adds a child [View] at a specific index.
	 */
	fun addChildAt(view: View, index: Int) {
		val index = index.clamp(0, this.children.size)
		view.removeFromParent()
		view.index = index
		children.add(index, view)
		for (n in index + 1 until children.size) children[n].index = n // Update other indices
		view.parent = this
		view.invalidate()
	}

	// @TODO: Untested
	/**
	 * Retrieves the index of a given child [View].
	 */
	fun getChildIndex(view: View): Int = view.index
	// @TODO: Untested

	/**
	 * Finds the [View] at a given index.
	 */
	fun getChildAt(index: Int): View = children[index]

	// @TODO: Untested
	/**
	 * Finds the first child [View] matching a given name.
	 */
	fun getChildByName(name: String): View? = children.firstOrNull { it.name == name }

	/**
	 * Removes a specific [View] from the container.
	 */
	fun removeChild(view: View?) {
		if (view?.parent == this) {
			view?.removeFromParent()
		}
	}

	/**
	 * Removes all child [View]s from the container.
	 */
	fun removeChildren() {
		children.fastForEach { child ->
			child.parent = null
			child.index = -1
		}
		children.clear()
	}

	/**
	 * Alias for [plusAssign].
	 */
	fun addChild(view: View) = this.plusAssign(view)

	/**
	 * Invalidates the container and all the child [View]s recursively.
	 */
	override fun invalidate() {
		super.invalidate()
		children.fastForEach { child ->
			if (child._requireInvalidate) {
				child.invalidate()
			}
		}
	}

	/**
	 * Adds a child [View] to the container.
	 * 
	 * If the [View] already belongs to a parent, it is removed from it and then added to the container.
	 */
	operator fun plusAssign(view: View) {
		view.removeFromParent()
		view.index = children.size
		children += view
		view.parent = this
		view.invalidate()
	}

	/**
	 * Removes a child [View] from the container.
	 */
	operator fun minusAssign(view: View) {
		if (view.parent == this) view.removeFromParent()
	}

	private val tempMatrix = Matrix()
	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
		safeForEachChildren { child ->
			child.render(ctx)
		}
	}

	/**
	 * Recursively finds the [View] displayed the given `x`, `y` coordinates.
	 *
	 * @returns The (visible) [View] displayed at the given coordinates or `null` if none is found.
	 */
	override fun hitTest(x: Double, y: Double): View? {
		children.fastForEachReverse { child ->
			if (child.visible) {
				val res = child.hitTest(x, y)
				if (res != null) return res
			}
		}
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

	/**
	 * Propagates an [Event] to all child [View]s.
	 *
	 * The [Event] is propagated to all the child [View]s of the container, iterated in reverse orted. 
	 */
	override fun <T : Event> dispatch(clazz: KClass<T>, event: T) {
		if (propagateEvents) {
			safeForEachChildrenReversed { child ->
				child.dispatch(clazz, event)
			}
		}
		super.dispatch(clazz, event)
	}

	private inline fun safeForEachChildren(crossinline callback: (View) -> Unit) {
		children.fastForEach(callback)
	}

	private inline fun safeForEachChildrenReversed(crossinline callback: (View) -> Unit) {
		children.fastForEachReverse(callback)
	}

	/**
	 * Creates a new container.
	 * @return an empty container.
	 */
	override fun createInstance(): View = Container()

	/**
	 * Performs a deep copy of the container, by copying all the child [View]s.
	 */
	override fun clone(): View {
		val out = super.clone()
		children.fastForEach { child ->
			out += child.clone()
		}
		return out
	}
}

/**
 * Alias for `parent += this`. Refer to [Container.plusAssign].
 */
fun <T : View> T.addTo(parent: Container) = this.apply { parent += this }

inline fun Container.fixedSizeContainer(width: Number, height: Number, clip: Boolean = false, callback: @ViewsDslMarker FixedSizeContainer.() -> Unit = {}) =
	FixedSizeContainer(width.toDouble(), height.toDouble(), clip).addTo(this).apply(callback)

open class FixedSizeContainer(
    override var width: Double = 100.0,
    override var height: Double = 100.0,
    var clip: Boolean = false
) : Container() {
	override fun getLocalBoundsInternal(out: Rectangle): Unit = Unit.run { out.setTo(0, 0, width, height) }

	override fun toString(): String {
		var out = super.toString()
		out += ":size=(${width.niceStr}x${height.niceStr})"
		return out
	}

    private val tempBounds = Rectangle()

    override fun renderInternal(ctx: RenderContext) {
        if (clip) {
            val c2d = ctx.ctx2d
            val bounds = getGlobalBounds(tempBounds)
            c2d.scissor(bounds) {
                super.renderInternal(ctx)
            }
        } else {
            super.renderInternal(ctx)
        }
    }
}

inline fun Container.clipContainer(width: Number, height: Number, callback: @ViewsDslMarker ClipContainer.() -> Unit = {}) =
    ClipContainer(width.toDouble(), height.toDouble()).addTo(this).apply(callback)

open class ClipContainer(
    width: Double = 100.0,
    height: Double = 100.0
) : FixedSizeContainer(width, height, clip = true)

operator fun View?.plusAssign(view: View?) {
	val container = this as? Container?
	if (view != null) container?.addChild(view)
}
