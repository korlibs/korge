package com.soywiz.korge.awt

import com.soywiz.kds.*
import com.soywiz.korev.Event
import com.soywiz.korge.debug.UiEditProperties
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewLeaf
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korma.geom.*
import com.soywiz.korui.UiApplication
import com.soywiz.korui.layout.UiFillLayout
import kotlinx.coroutines.*
import java.awt.EventQueue
import java.awt.GridLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.JMenu
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import kotlin.collections.AbstractList
import kotlin.coroutines.CoroutineContext

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
    val uiPropertiesPanelScroll = myComponentFactory.scrollPane(uiPropertiesPanel).also { add(it) }

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
                        val popupMenu = myComponentFactory.createPopupMenu()

                        val subMenuAdd = JMenu("Add")

                        for (factory in myComponentFactory.getViewFactories(views)) {
                            subMenuAdd.add(myComponentFactory.createMenuItem(factory.name).also {
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

                        popupMenu.add(myComponentFactory.createSeparator())
                        popupMenu.add(myComponentFactory.createMenuItem("Cut").also {
                            it.addActionListener {
                                actions.requestCut()
                            }
                        })
                        popupMenu.add(myComponentFactory.createMenuItem("Copy").also {
                            it.addActionListener {
                                actions.requestCopy()
                            }
                        })
                        popupMenu.add(myComponentFactory.createMenuItem("Paste").also {
                            it.addActionListener {
                                actions.requestPaste()
                            }
                        })
                        popupMenu.add(myComponentFactory.createSeparator())
                        popupMenu.add(myComponentFactory.createMenuItem("Duplicate", KeyEvent.CTRL_DOWN_MASK or KeyEvent.VK_D).also {
                            it.addActionListener {
                                launchImmediately(coroutineContext) {
                                    actions.duplicate()
                                }
                            }
                        })
                        popupMenu.add(myComponentFactory.createSeparator())
                        popupMenu.add(myComponentFactory.createMenuItem("Remove view", KeyEvent.VK_DELETE).also {
                            it.addActionListener {
                                actions.removeCurrentNode()
                            }
                        })
                        popupMenu.add(myComponentFactory.createSeparator())
                        popupMenu.add(myComponentFactory.createMenuItem("Send to back").also {
                            it.addActionListener {
                                actions.sendToBack()
                            }
                        })
                        popupMenu.add(myComponentFactory.createMenuItem("Bring to front").also {
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
    val treeScroll = myComponentFactory.scrollPane(tree).also {
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
}
