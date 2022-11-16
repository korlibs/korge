package com.soywiz.korge.awt

import com.soywiz.kds.*
import com.soywiz.korev.Event
import com.soywiz.korge.internal.*
import com.soywiz.korge.particle.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.tiled.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Image
import com.soywiz.korge.view.ktree.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.*
import java.util.*
import javax.swing.*
import javax.swing.tree.*
import kotlin.collections.AbstractList
import kotlin.coroutines.*

val View.treeNode: ViewNode by Extra.PropertyThis { ViewNode(this) }

class ViewNode(val view: View?) : TreeNode {
    val container = view as? Container?
    override fun toString(): String {
        if (view == null) return "null"
        return StringBuilder().apply {
            val nodeName = if (view.name != null) view.name else "#${view.index}"
            append("$nodeName (${view::class.simpleName})")
        }.toString()
    }

    override fun isLeaf(): Boolean = (container == null) || (view is ViewLeaf)
    fun childrenList(): List<View> {
        if (view is ViewLeaf) return listOf()
        //return container?.children?.filter { it !is DummyView } ?: EmptyList() // TOO SLOW
        return container?.children ?: emptyList()
    }

    override fun getChildAt(childIndex: Int): TreeNode? = childrenList().getOrNull(childIndex)?.treeNode
    override fun getChildCount(): Int = childrenList().size
    override fun getParent(): TreeNode? = view?.parent?.treeNode
    override fun getIndex(node: TreeNode?): Int = childrenList().indexOf((node as? ViewNode?)?.view)
    override fun getAllowsChildren(): Boolean = container != null
    @OptIn(KorgeInternal::class)
    override fun children() = Vector<Any>(childrenList()).elements() as Enumeration<out TreeNode>
}

class ViewDebuggerChanged(val view: View?) : Event()

fun TreePath.withTree(tree: JTree): TreePathWithTree = TreePathWithTree(this, tree)

class TreePathWithTree(val path: TreePath, val tree: JTree) : AbstractList<TreePathWithTree>() {
    val model: TreeModel get() = tree.model
    val node: Any? get() = path.lastPathComponent
    val parent: TreePathWithTree? by lazy {
        if (path.path.size >= 2) {
            path.parentPath.withTree(tree)
        } else {
            null
        }
    }
    val expanded: Boolean get() = tree.isExpanded(path)
    val index: Int get() = model.getIndexOfChild(parent!!.node, node)
    override val size: Int get() = model.getChildCount(node)
    override fun get(index: Int): TreePathWithTree {
        if (index !in indices) throw IndexOutOfBoundsException()
        val newNode = model.getChild(node, index)
        return TreePath(arrayOf(*path.path, newNode)).withTree(tree)
    }
    fun siblingOffset(offset: Int): TreePathWithTree? {
        val parent = this.parent ?: return null
        val nextIndex = index + offset
        return if (nextIndex in 0 until parent.size) parent[nextIndex] else null
    }
    val prevSibling: TreePathWithTree? get() = siblingOffset(-1)
    val nextSibling: TreePathWithTree? get() = siblingOffset(+1)
    val firstChild: TreePathWithTree? get() = if (isNotEmpty()) this[0] else null
    val nextSiblingOrNext: TreePathWithTree? get() = nextSibling ?: parent?.nextSiblingOrNext
    val next: TreePathWithTree? get() = firstChild ?: nextSiblingOrNext

    fun scroll(top: Boolean = false) {
        val bounds = tree.getPathBounds(path) ?: return
        if (top) bounds.height = tree.visibleRect.height
        tree.scrollRectToVisible(bounds)
    }

    fun select() {
        tree.selectionPath = path
    }

    fun selectAndScroll() {
        select()
        scroll()
    }
}

internal class ViewsDebuggerComponent constructor(
    val views: Views,
    val app: UiApplication,
    rootView: View? = views.stage,
    val coroutineContext: CoroutineContext = views.coroutineContext,
    val actions: ViewsDebuggerActions = ViewsDebuggerActions(views),
    val displayTree: Boolean = true
) : JPanel(GridLayout(if (displayTree) 2 else 1, 1)) {
    init {
        actions.component = this
    }
    val uiProperties = UiEditProperties(app, rootView, views)
    val uiPropertiesPanel = JPanel()
        .also {
            val panel = app.wrapContainer(it)
            //panel.layout = VerticalUiLayout
            panel.layout = UiFillLayout
            //panel.button("Hello")
            panel.addChild(uiProperties)
            //it.add(JButton())
            panel.relayout()
        }
    val uiPropertiesPanelScroll = scrollPane(uiPropertiesPanel).also { add(it) }

    init {
        views.debugHighlighters.add { view ->
            EventQueue.invokeLater {
                //println("HIGHLIGHTING: $view")
                println("ViewsDebuggerActions.highlight: $views")
                update()
                val treeNode = view?.treeNode
                if (treeNode != null) {
                    val path = TreePath((tree.model as DefaultTreeModel).getPathToRoot(treeNode))
                    println("   - $path")
                    tree.expandPath(path)
                    //tree.clearSelection()
                    tree.selectionPath = path
                    tree.scrollPathToVisible(path)
                    //tree.repaint()
                } else {
                    tree.clearSelection()
                    selectView(null)
                }
                update()
            }
        }
    }

    private fun selectView(view: View?) {
        uiProperties.setView(view)
        views.renderContext.debugAnnotateView = view
        uiProperties.relayout()
    }

    val tree: JTree = JTree(ViewNode(rootView)).apply {
        val tree = this
        addTreeSelectionListener {
            //println("addTreeSelectionListener: ${it.paths.toList()}")
            if (it.paths.isNotEmpty()) {
                selectView((it.path.lastPathComponent as ViewNode).view)
            } else {
                selectView(null)
            }
        }
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_RIGHT) {
                    val selectionPath = tree.selectionPath?.withTree(tree)
                    if (selectionPath != null && (selectionPath.expanded || selectionPath.isEmpty())) {
                        selectionPath.next?.selectAndScroll()
                    }
                }
                if (e.keyCode == KeyEvent.VK_DELETE) {
                    actions.removeCurrentNode()
                }
            }
        })
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val row = tree.getClosestRowForLocation(e.x, e.y)
                    tree.setSelectionRow(row)
                    val view = selectedView
                    val isContainer = view is Container

                    if (view != null) {
                        val popupMenu = createPopupMenu()

                        val subMenuAdd = JMenu("Add")

                        for (factory in getViewFactories(views)) {
                            subMenuAdd.add(createMenuItem(factory.name).also {
                                it.isEnabled = isContainer
                                it.addActionListener {
                                    actions.attachNewView(factory.build().also {
                                        it.globalX = views.virtualWidth * 0.5
                                        it.globalY = views.virtualHeight * 0.5
                                    })
                                }
                            })
                        }
                        popupMenu.add(subMenuAdd)

                        popupMenu.add(createSeparator())
                        popupMenu.add(createMenuItem("Cut").also {
                            it.addActionListener {
                                actions.requestCut()
                            }
                        })
                        popupMenu.add(createMenuItem("Copy").also {
                            it.addActionListener {
                                actions.requestCopy()
                            }
                        })
                        popupMenu.add(createMenuItem("Paste").also {
                            it.addActionListener {
                                actions.requestPaste()
                            }
                        })
                        popupMenu.add(createSeparator())
                        popupMenu.add(createMenuItem("Duplicate", KeyEvent.CTRL_DOWN_MASK or KeyEvent.VK_D).also {
                            it.addActionListener {
                                launchImmediately(coroutineContext) {
                                    actions.duplicate()
                                }
                            }
                        })
                        popupMenu.add(createSeparator())
                        popupMenu.add(createMenuItem("Remove view", KeyEvent.VK_DELETE).also {
                            it.addActionListener {
                                actions.removeCurrentNode()
                            }
                        })
                        popupMenu.add(createSeparator())
                        popupMenu.add(createMenuItem("Send to back").also {
                            it.addActionListener {
                                actions.sendToBack()
                            }
                        })
                        popupMenu.add(createMenuItem("Bring to front").also {
                            it.addActionListener {
                                actions.sendToFront()
                            }
                        })
                        popupMenu.show(e.component, e.x, e.y)
                    }
                }
            }
        })
    }
    val selectedView: View? get() = actions.selectedView
    val treeScroll = JScrollPane(tree).also {
        if (displayTree) {
            add(it)
        }
    }

    fun setRootView(root: View) {
        //this.coroutineContext = coroutineContext
        //if (views != null) this.views = views
        //properties.views = views?.views
        tree.model = DefaultTreeModel(root.treeNode)
        update()
    }

    fun updateTimer() {
        EventQueue.invokeLater {
            if (uiProperties.currentView != null && uiProperties.currentView?.stage == null) {
                uiProperties.setView(null)
                views.renderContext.debugAnnotateView = null
            } else {
                update()
            }
        }
    }

    fun update() {
        //tree.model.tree
        //tree.treeDidChange()
        tree.updateUI()
        uiProperties.update()
    }



    private fun scrollPane(view: Component): JScrollPane =
        JScrollPane(view, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)

    data class ViewFactory(val name: String, val build: () -> View)

    private fun getViewFactories(views: Views): List<ViewFactory> = ArrayList<ViewFactory>().also { list ->
        list.add(ViewFactory("Image") { Image(Bitmaps.white).apply { setSize(100.0, 100.0) } })
        list.add(ViewFactory("VectorImage") { VectorImage.createDefault().apply { setSize(100.0, 100.0) } })
        list.add(ViewFactory("SolidRect") { SolidRect(100, 100, Colors.WHITE) })
        list.add(ViewFactory("Ellipse") { Ellipse(50.0, 50.0, Colors.WHITE).center() })
        list.add(ViewFactory("Container") { Container() })
        list.add(ViewFactory("TreeViewRef") { TreeViewRef() })
        list.add(ViewFactory("ParticleEmitter") { ParticleEmitterView(ParticleEmitter()) })
        list.add(ViewFactory("TiledMapViewRef") { TiledMapViewRef() })
        list.add(ViewFactory("9-Patch") { NinePatchEx(NinePatchBmpSlice(Bitmap32(62, 62))) })
        for (registration in views.ktreeSerializer.registrationsExt) {
            list.add(ViewFactory(registration.name) { registration.factory() })
        }
    }

    private fun createPopupMenu(): JPopupMenu = JPopupMenu()
    private fun createSeparator(): JSeparator = JSeparator()
    private fun createMenuItem(text: String, mnemonic: Int? = null, icon: Icon? = null): JMenuItem = when {
        mnemonic != null -> JMenuItem(text, mnemonic)
        icon != null -> JMenuItem(text, icon)
        else -> JMenuItem(text)
    }
}
