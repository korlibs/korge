package com.soywiz.korge.view

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.clamp
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korge.internal.KorgeUntested
import com.soywiz.korge.render.RenderContext
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle

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
@OptIn(KorgeInternal::class)
open class Container : View(true), MutableCollection<View> {
    @PublishedApi
    internal val __children: FastArrayList<View> = FastArrayList()

    override val _children: FastArrayList<View>? get() = __children

    inline fun fastForEachChild(block: (child: View) -> Unit) {
        __children.fastForEach { child -> block(child) }
    }

    @KorgeInternal
    @PublishedApi
    internal val childrenInternal: FastArrayList<View>
        get() {
            return __children!!
        }

    /**
     * Retrieves all the child [View]s.
     * Shouldn't be used if possible. You can use [numChildren] and [getChildAt] to get the children.
     * You can also use [forEachChild], [forEachChildWithIndex] and [forEachChildReversed] to iterate children
     */
    @KorgeInternal
    val children: FastArrayList<View>
        get() = __children

    fun toChildrenList() = __children.toList()

    override fun invalidate() {
        super.invalidate()
        fastForEachChild { child ->
            if (child._requireInvalidate) {
                child.invalidate()
            }
        }
    }

    override fun invalidateColorTransform() {
        super.invalidateColorTransform()
        fastForEachChild { child ->
            if (child._requireInvalidateColor) {
                child.invalidateColorTransform()
            }
        }
    }

    /** Returns the first child of this container or null when the container doesn't have children */
    val firstChild: View? get() = __children.firstOrNull()

    /** Returns the last child of this container or null when the container doesn't have children */
    val lastChild: View? get() = __children.lastOrNull()

    /** Sorts all the children by using the specified [comparator]. */
    fun sortChildrenBy(comparator: Comparator<View>) {
        __children.sortWith(comparator)
        forEachChildWithIndex { index: Int, child: View ->
            child.index = index
        }
    }

    /** Returns the number of children this container has */
    @Suppress("FoldInitializerAndIfToElvis")
    val numChildren: Int
        get() {
            val children = _children
            if (children == null) return 0
            return children.size
        }

    /** Returns the number of children this container has */
    override val size: Int get() = numChildren

    /**
     * Recursively retrieves the top ancestor in the container hierarchy.
     *
     * Retrieves the top ancestor of the hierarchy. In case the container is orphan this very instance is returned.
     */
    val containerRoot: Container get() = parent?.containerRoot ?: this

    /**
     * Recursively retrieves the ancestor in the container hierarchy that is a [View.Reference] like the stage or null when can't be found.
     */
    val referenceParent: Container?
        get() {
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
            __children[index1] = view2
            view2.index = index1
            __children[index2] = view1
            view1.index = index2
        }
    }

    fun moveChildTo(view: View, index: Int) {
        if (view.parent != this) return
        val targetIndex = index.clamp(0, numChildren - 1)
        while (view.index < targetIndex) swapChildren(view, __children[view.index + 1])
        while (view.index > targetIndex) swapChildren(view, __children[view.index - 1])
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
        view.removeFromParent()
        val aindex = index.clamp(0, this.numChildren)
        view.index = aindex
        val children = childrenInternal
        children.add(aindex, view)
        for (n in aindex + 1 until children.size) children[n].index = n // Update other indices
        view.parent = this
        view.invalidate()
        onChildAdded(view)
    }

    protected open fun onChildAdded(view: View) {
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
    fun getChildAtOrNull(index: Int): View? = __children.getOrNull(index)

    /**
     * Finds the first child [View] matching a given [name].
     */
    @KorgeUntested
    fun getChildByName(name: String): View? = __children.firstOrNull { it.name == name }

    /**
     * Removes the specified [view] from this container.
     *
     * Returns true if the child was removed from this container.
     * Returns false, if nothing happened.
     *
     * Remarks: If the parent of [view] is not this container, this function doesn't do anything.
     */
    fun removeChild(view: View?): Boolean {
        if (view?.parent == this) {
            view?.removeFromParent()
            return true
        }
        return false
    }

    /**
     * Removes all [View]s children from this container.
     */
    fun removeChildren() {
        fastForEachChild { child ->
            child.parent = null
            child.index = -1
        }
        __children.clear()
    }

    inline fun removeChildrenIf(cond: (index: Int, child: View) -> Boolean) {
        var removedCount = 0
        forEachChildWithIndex { index, child ->
            if (cond(index, child)) {
                child._parent = null
                child._index = -1
                removedCount++
            } else {
                child._index -= removedCount
            }
        }
        for (n in 0 until removedCount) __children.removeAt(__children.size - 1)
    }

    /**
     * Adds a child [View] to the container.
     *
     * If the [View] already belongs to a parent, it is removed from it and then added to the container.
     */
    fun addChild(view: View) = addChildAt(view, numChildren)

    fun addChildren(views: List<View?>?) = views?.toList()?.fastForEach { it?.let { addChild(it) } }

    /**
     * Alias for [addChild].
     */
    operator fun plusAssign(view: View) {
        addChildAt(view, numChildren)
    }

    /** Alias for [getChildAt] */
    operator fun get(index: Int): View = getChildAt(index)

    /**
     * Alias for [removeChild].
     */
    operator fun minusAssign(view: View) {
        removeChild(view)
    }

    private val tempMatrix = Matrix()
    override fun renderInternal(ctx: RenderContext) {
        if (!visible) return
        renderChildrenInternal(ctx)
    }

    open fun renderChildrenInternal(ctx: RenderContext) {
        fastForEachChild { child: View ->
            child.render(ctx)
        }
    }

    override fun renderDebug(ctx: RenderContext) {
        fastForEachChild { child: View ->
            child.renderDebug(ctx)
        }
        super.renderDebug(ctx)
    }

    private val bb = BoundsBuilder()
    private val tempRect = Rectangle()

    override fun getLocalBoundsInternal(out: Rectangle) {
        bb.reset()
        fastForEachChild { child: View ->
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
        fastForEachChild { out += it.clone() }
        return out
    }

    override fun findViewByName(name: String): View? {
        val result = super.findViewByName(name)
        if (result != null) return result
        fastForEachChild { child: View ->
            val named = child.findViewByName(name)
            if (named != null) return named
        }
        return null
    }

    override fun contains(element: View): Boolean {
        return __children.contains(element)
    }

    override fun containsAll(elements: Collection<View>): Boolean {
        return __children.containsAll(elements)
    }

    override fun isEmpty(): Boolean {
        return __children.isEmpty()
    }

    override fun add(element: View): Boolean {
        addChild(element)
        return true
    }

    override fun addAll(elements: Collection<View>): Boolean {
        elements.forEach {
            add(it)
        }
        return true
    }

    override fun clear() {
        removeChildren()
    }

    override fun iterator(): MutableIterator<View> {
        return __children.iterator()
    }

    override fun remove(element: View): Boolean {
        return removeChild(element)
    }

    override fun removeAll(elements: Collection<View>): Boolean {
        return elements.fold(false) { acc, ele ->
            remove(ele) || acc
        }
    }

    // Retains only the elements in this collection that are contained in the specified collection.
    // Returns:
    // true if any element was removed from the collection, false if the collection was not modified.
    override fun retainAll(elements: Collection<View>): Boolean {
        val currentElements = this.toChildrenList()
        return currentElements.fold(false) { acc, it ->
            if (elements.contains(it)) {
                acc
            } else {
                remove(it) || acc
            }
        }
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

inline fun <T : View> Container.append(view: T): T {
    addChild(view)
    return view
}

inline fun <T : View> Container.append(view: T, block: T.() -> Unit): T = append(view).also(block)

fun View.bringToTop() {
    val parent = this.parent ?: return
    parent.moveChildTo(this, parent.numChildren - 1)
}

fun View.bringToBottom() {
    val parent = this.parent ?: return
    parent.moveChildTo(this, 0)
}
