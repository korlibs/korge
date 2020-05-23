package com.soywiz.korge.view

import com.soywiz.kds.iterators.*
import com.soywiz.kds.iterators.fastForEachReverse
import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korma.geom.*
import kotlin.reflect.*

/** Creates a new [Container], allowing to configure with [callback], and attaches the newly created container to the receiver this [Container] */
inline fun Container.container(callback: @ViewsDslMarker Container.() -> Unit = {}) =
	Container().addTo(this).apply(callback)

// For Flash compatibility
//open class Sprite : Container()

/**
 * A simple container of [View]s.
 *
 * All the [children] in this container has an associated index that determines its rendering order.
 * The first child is rendered first, and the last, last. So when the childs are overlapping, the last child will overlap the previous ones.
 *
 * You can access the children by [numChildren], [getChildAt] or [size] and [get].
 *
 * You can add new children to this container by calling [addChild] or [addChildAt].
 */
@UseExperimental(KorgeInternal::class)
open class Container : View() {
	final override val isContainer get() = true

    @KorgeInternal
    @PublishedApi
	internal val childrenInternal = arrayListOf<View>()

	/**
	 * Retrieves all the child [View]s.
     * Shouldn't be used if possible. You can use [numChildren] and [getChildAt] to get the children.
     * You can also use [forEachChildren], [forEachChildrenWithIndex] and [forEachChildrenReversed] to iterate children
	 */
    @KorgeInternal
    val children: List<View> get() = childrenInternal

    /** Returns the first child of this container or null when the container doesn't have children */
    val firstChild: View? get() = childrenInternal.firstOrNull()
    /** Returns the last child of this container or null when the container doesn't have children */
    val lastChild: View? get() = childrenInternal.lastOrNull()

    /** Sorts all the children by using the specified [comparator]. */
    fun sortChildrenBy(comparator: Comparator<View>) {
        childrenInternal.sortWith(comparator)
        forEachChildrenWithIndex { index, child ->
            child.index = index
        }
    }

    /** Iterates all the children of this container in normal order of rendering. */
    inline fun forEachChildren(callback: (child: View) -> Unit) = childrenInternal.fastForEach(callback)
    /** Iterates all the children of this container in normal order of rendering. Providing an index in addition to the child to the callback. */
    inline fun forEachChildrenWithIndex(callback: (index: Int, child: View) -> Unit) = childrenInternal.fastForEachWithIndex(callback)
    /** Iterates all the children of this container in reverse order of rendering. */
    inline fun forEachChildrenReversed(callback: (child: View) -> Unit) = childrenInternal.fastForEachReverse(callback)

    /** Returns the number of children this container has */
    val numChildren: Int get() = children.size
    /** Returns the number of children this container has */
    val size: Int get() = numChildren

	/**
	 * Recursively retrieves the top ancestor in the container hierarchy.
	 *
	 * Retrieves the top ancestor of the hierarchy. In case the container is orphan this very instance is returned.
	 */
	val containerRoot: Container get() = parent?.containerRoot ?: this

    /**
     * Recursively retrieves the ancestor in the container hierarchy that is a [View.Reference] like the stage or null when can't be found.
     */
    val referenceParent: Container? get() {
        if (parent is Reference) return parent
        return parent?.referenceParent
    }

    /**
	 * Swaps the order of two child [View]s [view1] and [view2].
     * If [view1] or [view2] are not part of this container, this method doesn't do anything.
	 */
    @KorgeUntested
	fun swapChildren(view1: View, view2: View) {
		if (view1.parent == view2.parent && view1.parent == this) {
			val index1 = view1.index
			val index2 = view2.index
			childrenInternal[index1] = view2.apply { index = index1 }
			childrenInternal[index2] = view1.apply { index = index2 }
		}
	}

	/**
	 * Adds the [view] [View] as a child at a specific [index].
     *
     * Remarks: if [index] is outside bounds 0..[numChildren], it will be clamped to the nearest valid value.
	 */
    @KorgeUntested
	fun addChildAt(view: View, index: Int) {
		val aindex = index.clamp(0, this.childrenInternal.size)
		view.removeFromParent()
		view.index = aindex
		childrenInternal.add(aindex, view)
		for (n in aindex + 1 until childrenInternal.size) childrenInternal[n].index = n // Update other indices
		view.parent = this
		view.invalidate()
	}

	/**
	 * Retrieves the index of a given child [View].
	 */
    @KorgeUntested
	fun getChildIndex(view: View): Int = view.index

	/**
	 * Finds the [View] at a given index.
     * Remarks: if [index] is outside bounds 0..[numChildren] - 1, an [IndexOutOfBoundsException] will be thrown.
	 */
	fun getChildAt(index: Int): View = childrenInternal[index]

    /**
     * Finds the [View] at a given index. If the index is not valid, it returns null.
     */
    fun getChildAtOrNull(index: Int): View? = childrenInternal.getOrNull(index)

    /**
	 * Finds the first child [View] matching a given [name].
	 */
    @KorgeUntested
	fun getChildByName(name: String): View? = childrenInternal.firstOrNull { it.name == name }

	/**
	 * Removes the specified [view] from this container.
     *
     * Remarks: If the parent of [view] is not this container, this function doesn't do anything.
	 */
	fun removeChild(view: View?) {
		if (view?.parent == this) {
			view?.removeFromParent()
		}
	}

	/**
	 * Removes all [View]s children from this container.
	 */
	fun removeChildren() {
		childrenInternal.fastForEach { child ->
			child.parent = null
			child.index = -1
		}
		childrenInternal.clear()
	}

	/**
     * Adds a child [View] to the container.
     *
     * If the [View] already belongs to a parent, it is removed from it and then added to the container.
	 */
	fun addChild(view: View) = this.plusAssign(view)

	/**
	 * Invalidates the container and all the child [View]s recursively.
	 */
	override fun invalidate() {
		super.invalidate()
		childrenInternal.fastForEach { child ->
			if (child._requireInvalidate) {
				child.invalidate()
			}
		}
	}

	/**
	 * Alias for [addChild].
	 */
	operator fun plusAssign(view: View) {
		view.removeFromParent()
		view.index = childrenInternal.size
		childrenInternal += view
		view.parent = this
		view.invalidate()
	}

    /** Alias for [getChildAt] */
    operator fun get(index: Int): View = getChildAt(index)

	/**
	 * Alias for [removeChild].
	 */
	operator fun minusAssign(view: View) = removeChild(view)

	private val tempMatrix = Matrix()
	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
		forEachChildren { child ->
			child.render(ctx)
		}
	}

	/**
	 * Recursively finds the [View] displayed the given `x`, `y` coordinates.
	 *
	 * @returns The (visible) [View] displayed at the given coordinates or `null` if none is found.
	 */
	override fun hitTest(x: Double, y: Double): View? {
		childrenInternal.fastForEachReverse { child ->
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
		forEachChildren { child ->
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
			forEachChildrenReversed { child ->
				child.dispatch(clazz, event)
			}
		}
		super.dispatch(clazz, event)
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
		childrenInternal.fastForEach { child ->
			out += child.clone()
		}
		return out
	}
}

/**
 * Alias for `parent += this`. Refer to [Container.plusAssign].
 */
fun <T : View> T.addTo(parent: Container): T {
    parent += this
    return this
}

/** Adds the specified [view] to this view only if this view is a [Container]. */
operator fun View?.plusAssign(view: View?) {
	val container = this as? Container?
	if (view != null) container?.addChild(view)
}
