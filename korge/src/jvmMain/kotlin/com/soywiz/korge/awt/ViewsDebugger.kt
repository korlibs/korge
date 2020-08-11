package com.soywiz.korge.awt

import com.soywiz.kds.*
import com.soywiz.korev.Event
import com.soywiz.korge.debug.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.Container
import com.soywiz.korio.async.*
import com.soywiz.korui.*
import com.soywiz.korui.layout.*
import java.awt.*
import java.awt.event.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.tree.*
import kotlin.coroutines.*

val View.treeNode: ViewNode by Extra.PropertyThis<View, ViewNode> { ViewNode(this) }

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
        return container?.children?.filter { it !is DummyView } ?: listOf()
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


class ViewsDebuggerComponent constructor(
    val views: Views,
    val app: UiApplication,
    rootView: View? = views.stage,
    val coroutineContext: CoroutineContext = views.coroutineContext,
    val actions: ViewsDebuggerActions = ViewsDebuggerActions(views),
) : JPanel(GridLayout(2, 1)) {
    init {
        actions.component = this
    }
    val uiProperties = UiEditProperties(app, rootView, views)
    val uiPropertiesPanel = JPanel()
        .also {
            val panel = app.wrapContainer(it)
            panel.layout = VerticalUiLayout
            //panel.button("Hello")
            panel.addChild(uiProperties)
            //it.add(JButton())
            panel.relayout()
        }
    val uiPropertiesPanelScroll = myComponentFactory.scrollPane(uiPropertiesPanel).also { add(it) }

    init {
        views.debugHighlighters.add { view ->
            //println("HIGHLIGHTING: $view")
            println("ViewsDebuggerActions.highlight: $views")
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

    private fun selectView(view: View?) {
        uiProperties.setView(view, coroutineContext)
        views.renderContext.debugAnnotateView = view
        uiProperties.relayout()
    }

    val tree: JTree = JTree(ViewNode(rootView)).apply {
        val tree = this
        addTreeSelectionListener {
            println("addTreeSelectionListener: ${it.paths.toList()}")
            if (it.paths.isNotEmpty()) {
                selectView((it.path.lastPathComponent as ViewNode).view)
            } else {
                selectView(null)
            }
        }
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
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

                        for (factory in myComponentFactory.getViewFactories(views)) {
                            popupMenu.add(myComponentFactory.createMenuItem("Add ${factory.name}").also {
                                it.isEnabled = isContainer
                                it.addActionListener {
                                    actions.attachNewView(factory.build().also {
                                        it.globalX = views.virtualWidth * 0.5
                                        it.globalY = views.virtualHeight * 0.5
                                    })
                                }
                            })
                        }

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
    val treeScroll = myComponentFactory.scrollPane(tree).also { add(it) }

    fun setRootView(root: View) {
        //this.coroutineContext = coroutineContext
        //if (views != null) this.views = views
        //properties.views = views?.views
        tree.model = DefaultTreeModel(root.treeNode)
        update()
    }

    fun update() {
        tree.updateUI()
        uiProperties.update()
    }
}
