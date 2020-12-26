package com.soywiz.korge.view

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korma.geom.*

/** Creates a new [Container], allowing to configure with [callback], and attaches the newly created container to the receiver this [Container] */
inline fun Container.container(callback: @ViewDslMarker Container.() -> Unit = {}) =
	Container().addTo(this, callback)

// For Flash compatibility
//open class Sprite : Container()

/**
 * A simple container of [View]s.
 *
 * All the [children] in this container have an associated index that determines their rendering order.
 * The first child is rendered first, and the last one is rendered last. So when children are overlapping with each other,
 * the last child will overlap the previous ones.
 *
 * You can access the children by [numChildren], [getChildAt] or [size] and [get].
 *
 * You can add new children to this container by calling [addChild] or [addChildAt].
 */
@UseExperimental(KorgeInternal::class)
open class Container : View(true) {
    @KorgeInternal
    @PublishedApi
	internal val childrenInternal: ArrayList<View> get() {
        if (_children == null) _children = arrayListOf()
        return _children as ArrayList<View>
    }

	/**
	 * Retrieves all the child [View]s.
     * Shouldn't be used if possible. You can use [numChildren] and [getChildAt] to get the children.
     * You can also use [forEachChild], [forEachChildWithIndex] and [forEachChildReversed] to iterate children
	 */
    @KorgeInternal
    val children: List<View> get() = childrenInternal

    /** Returns the first child of this container or null when the container doesn't have children */
    val firstChild: View? get() = _children?.firstOrNull()
    /** Returns the last child of this container or null when the container doesn't have children */
    val lastChild: View? get() = _children?.lastOrNull()

    /** Sorts all the children by using the specified [comparator]. */
    fun sortChildrenBy(comparator: Comparator<View>) {
        _children?.sortWith(comparator)
        forEachChildWithIndex { index: Int, child: View ->
            child.index = index
        }
    }

    /** Returns the number of children this container has */
    @Suppress("FoldInitializerAndIfToElvis")
    val numChildren: Int get() {
        val children = _children
        if (children == null) return 0
        return children.size
    }
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
			_children?.set(index1, view2)
            view2.index = index1
            _children?.set(index2, view1)
            view1.index = index2
		}
	}

    fun sendChildToFront(view: View) {
        if (view.parent === this) {
            while (view != lastChild!!) {
                swapChildren(view, children[view.index + 1])
            }
        }
    }

    fun sendChildToBack(view: View) {
        if (view.parent === this) {
            while (view != firstChild!!) {
                swapChildren(view, children[view.index - 1])
            }
        }
    }

    /**
	 * Adds the [view] [View] as a child at a specific [index].
     *
     * Remarks: if [index] is outside bounds 0..[numChildren], it will be clamped to the nearest valid value.
	 */
    @KorgeUntested
	fun addChildAt(view: View, index: Int) {
		val aindex = index.clamp(0, this.numChildren)
		view.removeFromParent()
		view.index = aindex
        val children = childrenInternal
        children.add(aindex, view)
		for (n in aindex + 1 until children.size) children[n].index = n // Update other indices
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
    fun getChildAtOrNull(index: Int): View? = _children?.getOrNull(index)

    /**
	 * Finds the first child [View] matching a given [name].
	 */
    @KorgeUntested
	fun getChildByName(name: String): View? = _children?.firstOrNull { it.name == name }

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
		_children?.fastForEach { child ->
			child.parent = null
			child.index = -1
		}
        _children?.clear()
	}

	/**
     * Adds a child [View] to the container.
     *
     * If the [View] already belongs to a parent, it is removed from it and then added to the container.
	 */
	fun addChild(view: View) = this.plusAssign(view)

    fun addChildren(views: List<View?>?) = views?.toList()?.fastForEach { it?.let { addChild(it) } }
	/**
	 * Alias for [addChild].
	 */
	operator fun plusAssign(view: View) {
		view.removeFromParent()
		view.index = numChildren
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
		forEachChild { child: View ->
			child.render(ctx)
		}
    }

    override fun renderDebug(ctx: RenderContext) {
        forEachChild { child: View ->
            child.renderDebug(ctx)
        }
        super.renderDebug(ctx)
    }

    private val bb = BoundsBuilder()
	private val tempRect = Rectangle()

	override fun getLocalBoundsInternal(out: Rectangle) {
		bb.reset()
		forEachChild { child: View ->
			child.getBounds(this, tempRect)
			bb.add(tempRect)
		}
        bb.getBounds(out)
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
        _children?.fastForEach { out += it.clone() }
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

inline fun <T : View> T.addTo(instance: Container, callback: @ViewDslMarker T.() -> Unit = {}) =
    this.addTo(instance).apply(callback)
