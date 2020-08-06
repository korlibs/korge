package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korev.MouseEvent
import com.soywiz.korio.lang.*
import java.awt.*
import java.awt.event.*
import javax.swing.*
import java.awt.event.ComponentEvent
import java.util.*
import javax.swing.tree.*

actual val DEFAULT_UI_FACTORY: UiFactory = AwtUiFactory()

open class AwtUiFactory : UiFactory {
    override fun createWindow() = AwtWindow(this)
    override fun createContainer() = AwtContainer(this)
    override fun createScrollPanel() = AwtScrollPanel(this)
    override fun createButton() = AwtButton(this)
    override fun createLabel() = AwtLabel(this)
    override fun createTextField() = AwtTextField(this)
    override fun <T> createComboBox() = AwtComboBox<T>(this)
    override fun createTree() = AwtTree(this)
}

private val awtToWrappersMap = WeakMap<Component, AwtComponent>()

open class AwtComponent(override val factory: AwtUiFactory, val component: Component) : UiComponent, Extra by Extra.Mixin() {
    init {
        awtToWrappersMap[component] = this
    }

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        component.setBounds(x, y, width, height)
    }

    override var parent: UiContainer?
        get() = awtToWrappersMap[component.parent] as? UiContainer?
        set(p) {
            if (p == null) {
                component.parent?.remove(component)
            } else {
                //(p as AwtContainer).childContainer.add(component)
                (p as AwtContainer).container.add(component)
            }
        }

    override var index: Int
        get() = super.index
        set(value) {}

    override var visible: Boolean
        get() = component.isVisible
        set(value) = run { component.isVisible = value }

    override fun onMouseEvent(handler: (MouseEvent) -> Unit): Disposable {
        val event = MouseEvent()

        fun dispatch(e: java.awt.event.MouseEvent, type: MouseEvent.Type) {
            event.button = MouseButton[e.button]
            event.x = e.x
            event.y = e.y
            event.type = type
            handler(event)
        }

        val listener = object : MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) = dispatch(e, MouseEvent.Type.CLICK)
        }

        component.addMouseListener(listener)
        return Disposable {
            component.removeMouseListener(listener)
        }
    }
}

open class AwtContainer(factory: AwtUiFactory, val container: Container = JPanel(), val childContainer: Container = container) : AwtComponent(factory, container), UiContainer {
    init {
        container.layout = null
    }

    override val numChildren: Int get() = childContainer.componentCount
    override fun getChild(index: Int): UiComponent = awtToWrappersMap[childContainer.getComponent(index)] ?: error("Can't find component")
}

open class JFixedSizeContainer : JPanel() {
    init {
        this.layout = null
    }
    var myPreferredSize = Dimension(2000, 2000)
    override fun isPreferredSizeSet(): Boolean = true
    override fun preferredSize(): Dimension = myPreferredSize
}

open class AwtScrollPanel(
    factory: AwtUiFactory,
    val view: JFixedSizeContainer = AwtContainer(factory, JFixedSizeContainer()).container as JFixedSizeContainer,
    val scrollPanel: JScrollPane = JScrollPane(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS)
) : AwtContainer(factory, scrollPanel, view), UiScrollPanel {
    init {
        //view.preferredSize = Dimension(2000, 2000)
        //view.size = Dimension(2000, 2000)
        //scrollPanel.viewport.extentSize = Dimension(2000, 2000)
        //scrollPanel.viewport.viewSize = Dimension(2000, 2000)
    }
}

open class AwtWindow(factory: AwtUiFactory, val frame: JFrame = JFrame()) : AwtContainer(factory, frame, frame.contentPane), UiWindow {
    init {
        frame.contentPane.layout = null
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    }

    override var title: String
        get() = frame.title
        set(value) {
            frame.title = value
        }

    override fun onResize(handler: (ReshapeEvent) -> Unit): Disposable {
        val listener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                handler(ReshapeEvent(frame.x, frame.y, frame.contentPane.width, frame.contentPane.height))
            }
        }
        frame.addComponentListener(listener)
        return Disposable {
            frame.removeComponentListener(listener)
        }
    }
}

open class AwtButton(factory: AwtUiFactory, val button: JButton = JButton()) : AwtComponent(factory, button), UiButton {
    override var text: String
        get() = button.text
        set(value) = run { button.text = value }
}

open class AwtLabel(factory: AwtUiFactory, val label: JLabel = JLabel()) : AwtComponent(factory, label), UiLabel {
    override var text: String
        get() = label.text
        set(value) = run { label.text = value }
}

val UiTreeNode.awt by Extra.PropertyThis<UiTreeNode, AwtTreeNode>() { AwtTreeNode(this) }

data class AwtTreeNode(val node: UiTreeNode) : TreeNode {
    override fun getChildAt(childIndex: Int): TreeNode? = node.children?.get(childIndex)?.awt
    override fun getChildCount(): Int = node.children?.size ?: 0
    override fun getParent(): TreeNode? = node.parent?.let { it.awt }
    override fun getIndex(node: TreeNode?): Int = this.node.children?.indexOf(node) ?: -1
    override fun getAllowsChildren(): Boolean = node.children != null
    override fun isLeaf(): Boolean = node.children == null
    override fun children(): Enumeration<*> = Vector(node.children ?: listOf()).elements()
    override fun toString(): String = node.toString()
}

open class AwtTree(factory: AwtUiFactory, val tree: JTree = JTree()) : AwtComponent(factory, tree), UiTree {
    override var root: UiTreeNode?
        get() = ((tree.model as DefaultTreeModel).root as? AwtTreeNode?)?.node
        set(value) {
            tree.model = DefaultTreeModel(value?.awt)
        }
}

open class AwtTextField(factory: AwtUiFactory, val textField: JTextField = JTextField()) : AwtComponent(factory, textField), UiTextField {
    override var text: String
        get() = textField.text
        set(value) = run { textField.text = value }
}

open class AwtComboBox<T>(factory: AwtUiFactory, val comboBox: JComboBox<T> = JComboBox<T>()) : AwtComponent(factory, comboBox), UiComboBox<T> {
    override var items: List<T>
        get() {
            val model = comboBox.model
            return (0 until model.size).map { model.getElementAt(it) }
        }
        set(value) {
            comboBox.model = DefaultComboBoxModel((value as List<Any>).toTypedArray()) as DefaultComboBoxModel<T>
        }

    override var selectedItem: T?
        get() = comboBox.selectedItem as T?
        set(value) = run { comboBox.selectedItem = value }

}
