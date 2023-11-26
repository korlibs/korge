package korlibs.korge.ui

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.animate.*
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import kotlin.time.Duration.Companion.seconds

@KorgeExperimental
class UITreeViewNode<T>(val element: T, val items: List<UITreeViewNode<T>> = emptyList()) {
    constructor(element: T, vararg items: UITreeViewNode<T>) : this(element, items.toList())
}

@KorgeExperimental
class UITreeViewList<T>(
    val nodes: List<UITreeViewNode<T>> = listOf(),
    override val height: Double = 20.0,
    val genView: (T) -> View = { UIText("$it") }
) : UITreeViewProvider<UITreeViewNode<T>> {
    companion object {
        inline operator fun <T> invoke(
            nodes: List<UITreeViewNode<T>> = listOf(),
            height: Number = 20.0,
            noinline genView: (T) -> View = { UIText("$it") }
        ): UITreeViewList<T> = UITreeViewList(nodes, height.toDouble(), genView)
    }

    override fun getNumChildren(node: UITreeViewNode<T>?): Int {
        if (node == null) return nodes.size
        return node.items.size
    }

    override fun getChildAt(node: UITreeViewNode<T>?, index: Int): UITreeViewNode<T> {
        if (node == null) return nodes[index]
        return node.items[index]
    }

    override fun getViewForNode(node: UITreeViewNode<T>): View {
        return genView(node.element)
    }
}

@KorgeExperimental
fun <T> UITreeViewProvider<T>.getChildrenList(node: T?): List<T> = List(getNumChildren(node)) { getChildAt(node, it) }

@KorgeExperimental
interface UITreeViewProvider<T> {
    val height: Double
    fun getNumChildren(node: T?): Int
    fun getChildAt(node: T?, index: Int): T
    fun getViewForNode(node: T): View

    object Dummy : UITreeViewProvider<Any> {
        operator fun <T> invoke(): UITreeViewProvider<T> = Dummy as UITreeViewProvider<T>

        override val height: Double get() = 0.0

        override fun getNumChildren(node: Any?): Int = 0
        override fun getChildAt(node: Any?, index: Int): Any = Unit
        override fun getViewForNode(node: Any): View = DummyView()
    }
}

@KorgeExperimental
private class UITreeViewVerticalListProviderAdapter<T>(val provider: UITreeViewProvider<T>) : UIVerticalList.Provider {
    class Node<T>(val value: T, val localIndex: Int, val indentation: Int, val parent: Node<T>?) {
        val path: List<Node<T>> = (parent?.path ?: emptyList()) + listOf(this)
        var opened: Boolean = false
        var openCount: Int = 0
    }

    private val items = arrayListOf<Node<T>>()
    private var selectedNode: Node<T>? = null
    private var selectedBackground: View? = null

    override val numItems: Int get() = items.size
    override val fixedHeight: Double get() = provider.height

    companion object {
        val ICON_DOWN = NativeImageContext2d(10, 10) {
            val sw = 4.0
            val sh = 4.0
            translate(width * 0.5, height * 0.5 + sh * 0.5) {
                stroke(Colors.WHITE, 2.0) {
                    moveTo(-sw, +sh)
                    lineTo(0.0, -sh)
                    lineTo(+sw, +sh)
                }
            }
        }
    }

    override fun getItemHeight(index: Int): Double = fixedHeight
    override fun getItemView(index: Int, vlist: UIVerticalList): View {
        val node = items[index]
        val itemViews = vlist.extraCache("itemViewsCache") { LinkedHashMap<List<Node<T>>, View>() }
        return itemViews.getOrPut(node.path) {
            createItemView(index, vlist)
        }
    }
    fun createItemView(index: Int, vlist: UIVerticalList): View {
        //println("Creating new createItemView index=$index")
        val node = items[index]
        val childCount = provider.getNumChildren(node.value)
        val container = UIFillLayeredContainer()
        val background = container.solidRect(10, 10, Colors.TRANSPARENT)
        val stack = container.uiHorizontalStack(padding = 4.0)
        val child = provider.getViewForNode(node.value)
        stack.solidRect(10 * node.indentation, 10, Colors.TRANSPARENT)
        val imageContainer = stack.fixedSizeContainer(Size(10, 10))
        val icon = imageContainer.image(ICON_DOWN).centered.xy((ICON_DOWN.size * 0.5).toDouble().toVector())
        fun updateIcon(animated: Boolean) {
            val isOpen: Boolean? = when {
                childCount > 0 -> {
                    when {
                        node.opened -> true
                        else -> false
                    }
                }
                else -> null
            }

            icon.visible = isOpen != null
            val angle = if (isOpen == true) 90.degrees else 0.degrees
            if (animated) {
                icon.simpleAnimator.tween(icon::rotation[angle], time = 0.25.seconds)
            } else {
                icon.rotation = angle
            }

            background.color = when {
                selectedNode == node -> Colors["#191034"]
                else -> Colors.TRANSPARENT
            }

        }
        stack.addChild(child)
        if (childCount > 0) {
            //stack.onOutOnOver({ rect.color = Colors.RED }, { rect.color = Colors.BLUE })
        }
        stack.mouse.click {
            selectedNode = node
            if (node.hasChildren()) {
                node.toggle()
                updateIcon(animated = true)
                vlist.invalidateList()
            } else {
                vlist.invalidateList()
            }
        }
        updateIcon(animated = false)
        return container
    }

    fun getNodeAt(index: Int): Node<T> = items[index]

    // @TODO: Optimize. We can compute real index by recursively getting the index of the parent
    fun getNodeIndex(node: Node<T>): Int {
        return items.indexOf(node)
    }

    fun Node<T>.getIndex() = getNodeIndex(this)
    fun Node<T>.close() = closeNode(this)
    fun Node<T>.open() = openNode(this)
    fun Node<T>.toggle() = toggleNode(this)
    fun Node<T>?.getNumChildren() = provider.getNumChildren(this?.value)
    fun Node<T>?.hasChildren() = getNumChildren() > 0

    fun closeNode(node: Node<T>) {
        if (!node.opened) return
        val removeIndex = getNodeIndex(node) + 1
        node.opened = false
        while (true) {
            if (removeIndex >= items.size) return
            if (items[removeIndex].indentation <= node.indentation) return
            items.removeAt(removeIndex)
        }
    }

    fun openNode(node: Node<T>) {
        if (node.opened) return
        node.opened = true
        val nodeIndex = getNodeIndex(node)
        val children = provider.getChildrenList(node.value)
        node.openCount = children.size
        items.addAll(nodeIndex + 1, children.mapIndexed { index, t -> Node(t, index, node.indentation + 1, node) })
    }

    fun toggleNode(node: Node<T>) {
        if (node.opened) closeNode(node) else openNode(node)
    }

    fun init() {
        items.clear()
        provider.getChildrenList(null).fastForEachWithIndex { index, value ->
            items.add(Node(value, index, 0, null))
        }
    }
}

@KorgeExperimental
inline fun <T> Container.uiTreeView(
    provider: UITreeViewProvider<T>,
    size: Size = Size(256, 256),
    block: @ViewDslMarker Container.(UITreeView<T>) -> Unit = {}
): UITreeView<T> = UITreeView(provider, size)
    .addTo(this).also { block(it) }

@KorgeExperimental
class UITreeView<T>(
    provider: UITreeViewProvider<T>,
    size: Size = Size(128, 128),
) : UIGridFill(size, cols = 1, rows = 1) {
    val scrollable = uiScrollable {  }
    @KorgeExperimental
    internal val list = scrollable.container.uiVerticalList(UIVerticalList.Provider.Dummy)

    var provider: UITreeViewProvider<T> = UITreeViewProvider.Dummy()
        set(value) {
            if (field != value) {
                field = value
                list.provider = UITreeViewVerticalListProviderAdapter(value).also { it.init() }
            }
        }

    init {
        this.provider = provider
    }

    fun invalidateTree() {
        list.invalidateList()
    }
}
