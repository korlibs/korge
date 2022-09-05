package com.soywiz.korge.view

import com.soywiz.kds.FastArrayList
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.clamp
import com.soywiz.korev.EventResult
import com.soywiz.korge.component.Component
import com.soywiz.korge.component.ComponentType
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
 * You can access the children by using the [children] collection, or the [numChildren], [getChildAt] and [get] properties.
 *
 * You can add new children to this container by calling [addChild] or [addChildAt].
 */
@OptIn(KorgeInternal::class)
open class Container : View(true) {
    private val __children: FastArrayList<View> = FastArrayList()

    /**
     * A collection with all the children [View]s.
     */
    val children: ContainerCollection = ContainerCollection(this, __children)
    @Deprecated("", ReplaceWith("children"))
    val childrenCollection: ContainerCollection get() = children
    @Deprecated("", ReplaceWith("children"))
    val collection: ContainerCollection get() = children

    @PublishedApi
    override val _children: List<View>? get() = __children

    inline fun fastForEachChild(block: (child: View) -> Unit) {
        children.fastForEach { child -> block(child) }
    }

    //@KorgeInternal @PublishedApi internal val childrenInternal: FastArrayList<View> get() = __children!!

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
        invalidateZIndexChildren()
    }

    /** Returns the number of children this container has */
    @Suppress("FoldInitializerAndIfToElvis")
    val numChildren: Int get() = __children.size

    /** Returns the number of children this container has */
    @Deprecated("", ReplaceWith("numChildren"))
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
        if (view1.parent != view2.parent || view1.parent != this) return
        invalidateZIndexChildren()
        val index1 = view1.index
        val index2 = view2.index
        __children[index1] = view2
        view2.index = index1
        __children[index2] = view1
        view1.index = index2
    }

    fun moveChildTo(view: View, index: Int) {
        if (view.parent != this) return
        val targetIndex = index.clamp(0, numChildren - 1)
        while (view.index < targetIndex) swapChildren(view, __children[view.index + 1])
        while (view.index > targetIndex) swapChildren(view, __children[view.index - 1])
    }

    fun sendChildToFront(view: View) {
        if (view.parent !== this) return
        while (view != lastChild!!) {
            swapChildren(view, children[view.index + 1])
        }
    }

    fun sendChildToBack(view: View) {
        if (view.parent !== this) return
        while (view != firstChild!!) {
            swapChildren(view, children[view.index - 1])
        }
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
    fun getChildAt(index: Int): View = __children[index]

    /**
     * Finds the [View] at a given index. If the index is not valid, it returns null.
     */
    fun getChildAtOrNull(index: Int): View? = __children.getOrNull(index)

    /**
     * Finds the first child [View] matching a given [name].
     */
    @KorgeUntested
    fun getChildByName(name: String): View? = __children.firstOrNull { it.name == name }

    fun removeChildAt(index: Int): Boolean =
        removeChild(getChildAtOrNull(index))

    // @TODO: Optimize
    fun removeChildAt(index: Int, count: Int) {
        repeat(count) { removeChildAt(index) }
        invalidateZIndexChildren()
    }

    // @TODO: Optimize
    fun swapChildrenAt(indexA: Int, indexB: Int) {
        val a = getChildAtOrNull(indexA) ?: return
        val b = getChildAtOrNull(indexB) ?: return
        swapChildren(a, b)
    }

    // @TODO: Optimize
    fun swapChildrenAt(indexA: Int, indexB: Int, count: Int) {
        repeat(count) { swapChildrenAt(indexA + it, indexB + it) }
    }

    // @TODO: Optimize
    fun moveChildrenAt(from: Int, to: Int, count: Int = 1) {
        val children = (0 until count).mapNotNull {
            getChildAtOrNull(from).also { removeChildAt(from) }
        }
        val finalTo = if (from < to) to - count else to
        children.fastForEach { child -> addChildAt(child, finalTo) }
        invalidateZIndexChildren()
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
        invalidateZIndexChildren()
    }

    inline fun removeChildrenIf(cond: (index: Int, child: View) -> Boolean): Boolean {
        val children = this._children!! as FastArrayList<View>
        var removedCount = 0
        forEachChildWithIndex { index, child ->
            if (cond(index, child)) {
                child._parent = null
                child._index = -1
                removedCount++
            } else {
                child._index -= removedCount
                children[child._index] = child
            }
        }
        repeat(removedCount) { children.removeAt(children.size - 1) }
        invalidateZIndexChildren()
        return removedCount > 0
    }

    /**
     * Adds a child [View] to the container.
     *
     * If the [View] already belongs to a parent, it is removed from it and then added to the container.
     */
    fun addChild(view: View) = addChildAt(view, numChildren)

    fun addChildren(views: List<View?>?) {
        if (views == null) return
        views.fastForEach { if (it != null) addChild(it) }
    }

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
        fastForEachChildRender { child: View ->
            child.render(ctx)
        }
    }

    override fun renderDebug(ctx: RenderContext) {
        fastForEachChildRender { child: View ->
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

    object ZIndexComparator : Comparator<View> {
        override fun compare(a: View, b: View): Int = a.zIndex.compareTo(b.zIndex)
    }

    @PublishedApi internal var __childrenZIndexValid = false
    @PublishedApi internal var __childrenZIndexValidOrder = false
    @PublishedApi internal val __childrenZIndex = FastArrayList<View>()

    @PublishedApi internal fun shouldSortChildren(): Boolean {
        __children.fastForEach { if (it.zIndex != 0.0) { return true } }
        return false
    }

    /**
     * Iterates children in render order
     */
    // @TODO: Instead of resort everytime that something changes, let's keep an index in the zIndex collection
    inline fun fastForEachChildRender(block: (child: View) -> Unit) {
        if (!__childrenZIndexValid) {
            __childrenZIndexValid = true
            __childrenZIndex.clear()
            __childrenZIndex.addAll(this.children)
            if (shouldSortChildren()) __childrenZIndexValidOrder = false
            //println("fastForEachChildRender[$this] __childrenZIndex: ${__childrenZIndex.size}")
            //println("invalidated")
        }
        if (!__childrenZIndexValidOrder) {
            __childrenZIndexValidOrder = true
            __childrenZIndex.sortWith(ZIndexComparator)
        }
        //println(__childrenZIndex.map { it.zIndex })
        __childrenZIndex.fastForEach { child -> block(child) }
        //__children.fastForEach { child -> block(child) }
    }

    // @TODO: Instead of resort everytime that something changes, let's keep an index in the zIndex collection
    @PublishedApi internal fun invalidateZIndexChildren() {
        this.__childrenZIndexValid = false
        invalidateContainer()
    }

    // @TODO: Instead of resort everytime that something changes, let's keep an index in the zIndex collection
    @PublishedApi internal fun updatedChildZIndex(child: View, oldZIndex: Double, newZIndex: Double) {
        if (child.parent != this) return
        __childrenZIndexValidOrder = false
        invalidateContainer()
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Base methods that update the collection
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected fun invalidateContainer() {
        stage?.views?.invalidatedView(this)
    }

    /**
     * Adds the [view] [View] as a child at a specific [index].
     *
     * Remarks: if [index] is outside bounds 0..[numChildren], it will be clamped to the nearest valid value.
     */
    fun addChildAt(view: View, index: Int) {
        view.parent?.invalidateZIndexChildren()
        view.removeFromParent()
        val aindex = index.clamp(0, this.numChildren)
        view.index = aindex
        val children = __children
        children.add(aindex, view)
        for (n in aindex + 1 until children.size) children[n].index = n // Update other indices
        view.parent = this
        view.invalidate()
        onChildAdded(view)
        invalidateZIndexChildren()
        invalidateContainer()
        __updateChildListenerCount(view, add = true)
    }

    /**
     * Removes the specified [view] from this container.
     *
     * Returns true if the child was removed from this container.
     * Returns false, if nothing happened.
     *
     * Remarks: If the parent of [view] is not this container, this function doesn't do anything.
     */
    fun removeChild(view: View?): Boolean {
        //if (view == null) return false
        if (view?.parent !== this) return false
        for (i in view.index + 1 until numChildren) __children[i].index--
        __children.removeAt(view.index)
        view.parent = null
        view.index = -1
        invalidateZIndexChildren()
        __updateChildListenerCount(view, add = false)
        invalidateContainer()
        return true
    }

    /**
     * Replaces this child [old] with a [new] view.
     *
     * [old] must be part of this Container
     */
    fun replaceChild(old: View, new: View): Boolean {
        if (old === new) return false
        if (old.parent != this) return false

        __updateChildListenerCount(old, add = false)
        if (new.parent !== this) __updateChildListenerCount(new, add = true)

        invalidateContainer()
        invalidateZIndexChildren()
        new.parent?.__children?.remove(new)
        old.parent!!.__children[old.index] = new
        new.index = old.index
        new.parent = old.parent
        old.parent = null
        new.invalidate()
        old.index = -1
        return true
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Listeners
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //override fun <T : TEvent<T>> dispatchChildren(type: EventType<T>, event: T, result: EventResult?) {
    //    // @TODO: What if we mutate the list now
    //    fastForEachChild {
    //        val childEventListenerCount = it.onEventCount(type)
    //        if (childEventListenerCount > 0) {
    //            it.dispatch(type, event, result)
    //        }
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

/**
 * Allows to safely interact with the children of a [container] instance.
 */
class ContainerCollection internal constructor(val container: Container, children: List<View>) : MutableCollection<View>, List<View> by children {
    override val size: Int get() = container.numChildren

    override fun contains(element: View): Boolean = element.parent === container
    override fun containsAll(elements: Collection<View>): Boolean = elements.all { it.parent === container }
    override fun isEmpty(): Boolean = container.numChildren == 0

    override fun add(element: View): Boolean {
        container.addChild(element)
        return true
    }

    override fun addAll(elements: Collection<View>): Boolean {
        if (elements.isEmpty()) return false
        for (element in elements) container.addChild(element)
        return true
    }

    override fun clear() {
        container.removeChildren()
    }

    override fun iterator(): MutableIterator<View> = object : MutableIterator<View> {
        var index = 0
        override fun hasNext(): Boolean = (index < container.numChildren)
        override fun next(): View = container.getChildAt(index++)
        // @TODO: Cannot optimize because we need to move the rest of the children, and we might not complete the iteration
        override fun remove() { container.removeChildAt(--index) }
    }

    override fun remove(element: View): Boolean = container.removeChild(element)

    override fun removeAll(elements: Collection<View>): Boolean {
        val set = elements.toSet()
        return container.removeChildrenIf { _, view -> view in set }
    }

    override fun retainAll(elements: Collection<View>): Boolean {
        val set = elements.toSet()
        return container.removeChildrenIf { _, view -> view !in set }
    }
}

/** Alias for `parent += this`. Refer to [Container.plusAssign]. */
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
