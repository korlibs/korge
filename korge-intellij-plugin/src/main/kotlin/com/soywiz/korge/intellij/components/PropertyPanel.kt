package com.soywiz.korge.intellij.components

import com.intellij.ui.components.*
import com.soywiz.kmem.*
import com.soywiz.korge.intellij.editor.*
import com.soywiz.korge.intellij.editor.tiled.*
import com.soywiz.korge.intellij.editor.tiled.dialog.*
import com.soywiz.korge.intellij.ui.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.border.*
import kotlin.math.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.system.*

fun Styled<out Container>.createPropertyPanelWithEditor(
    editor: Component,
    rootNode: EditableNode
) {
    verticalStack {
        fill()
        horizontalStack {
            fill()
            verticalStack {
                minWidth = 32.pt
                fill()
                add(editor.styled {
                    fill()
                })
            }
            verticalStack {
                minWidth = 360.pt
                width = minWidth
                fillHeight()
                add(PropertyPanel(rootNode).styled {
                    fill()
                })
            }
        }
    }
}

fun RGBAf.editableNodes(variance: Boolean = false) = listOf(
    this::rd.toEditableProperty(if (variance) -1.0 else 0.0, +1.0, name = "red"),
    this::gd.toEditableProperty(if (variance) -1.0 else 0.0, +1.0, name = "green"),
    this::bd.toEditableProperty(if (variance) -1.0 else 0.0, +1.0, name = "blue"),
    this::ad.toEditableProperty(if (variance) -1.0 else 0.0, +1.0, name = "alpha")
)
fun com.soywiz.korma.geom.Point.editableNodes() = listOf(
    this::x.toEditableProperty(-1000.0, +1000.0),
    this::y.toEditableProperty(-1000.0, +1000.0)
)

class PropertyPanel(val rootNode: EditableNode) : JBScrollPane(JPanel().apply {
    this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
}, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED) {
    val contentPane = viewport.view as JPanel
    init {
        //contentPane.preferredSize = Dimension(500, 400)

        contentPane.add(rootNode.toComponent())
        //contentPane.add(Box.Filler(Dimension(0, 0), Dimension(0, 1024), Dimension(0, 1024)))
        contentPane.add(Box.createVerticalGlue())
    }
}

@JvmName("toEditablePropertyDouble")
fun KMutableProperty0<Double>.toEditableProperty(
    min: Double? = null, max: Double? = null,
    transformedMin: Double? = null, transformedMax: Double? = null,
    name: String? = null
): EditableNumericProperty<Double> {
    val prop = this
    val range = this.annotations.filterIsInstance<DoubleSupportedRange>().firstOrNull()

    val editMin = min ?: range?.min ?: 0.0
    val editMax = max ?: range?.max ?: 1000.0

    val realMin = transformedMin ?: editMin
    val realMax = transformedMax ?: editMax

    return EditableNumericProperty(
        name = name ?: this.name,
        clazz = this.returnType.jvmErasure as KClass<Double>,
        initialValue = this.get().convertRange(realMin, realMax, editMin, editMax),
        minimumValue = editMin,
        maximumValue = editMax,
    ).also {
        it.onChange {
            prop.set(it.convertRange(editMin, editMax, realMin, realMax))
        }
    }
}

@JvmName("toEditablePropertyInt")
fun KMutableProperty0<Int>.toEditableProperty(min: Int? = null, max: Int? = null): EditableNumericProperty<Int> {
    val prop = this
    val range = this.annotations.filterIsInstance<IntSupportedRange>().firstOrNull()
    return EditableNumericProperty(
        name = this.name,
        clazz = this.returnType.jvmErasure as KClass<Int>,
        initialValue = this.get(),
        minimumValue = min ?: range?.min,
        maximumValue = max ?: range?.max,
    ).also {
        it.onChange { prop.set(it) }
    }
}

annotation class IntSupportedRange(val min: Int, val max: Int)
annotation class DoubleSupportedRange(val min: Double, val max: Double)

fun EditableNode.toComponent(indentation: Int = 0): Component {
    return when (this) {
        is EditableSection -> {
            Section(indentation, this.title, this.list.map { it.toComponent(indentation + 1) })
        }
        is EditableNodeList -> {
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                for (item in this@toComponent.list) {
                    add(item.toComponent())
                }
            }
        }
        is EditableNumericProperty<*> -> {
            EditableNumberValue(this, indentation)
        }
        else -> {
            TODO()
        }
    }
}

interface EditableNode {
}

class EditableNodeList(val list: List<EditableNode>) : EditableNode {
    constructor(vararg list: EditableNode) : this(list.toList())
    constructor(block: MutableList<EditableNode>.() -> Unit) : this(ArrayList<EditableNode>().apply(block))
}

class EditableSection(val title: String, val list: List<EditableNode>) : EditableNode {
    constructor(title: String, vararg list: EditableNode) : this(title, list.toList())
    constructor(title: String, block: MutableList<EditableNode>.() -> Unit) : this(title, ArrayList<EditableNode>().apply(block))
}

data class EditableNumericProperty<T : Number>(
    val name: String,
    val clazz: KClass<T>,
    val initialValue: T,
    val minimumValue: T? = null,
    val maximumValue: T? = null,
    val step: T? = null
) : EditableNode {
    val onChange = Signal<T>()
    //var prev: T = initialValue
    var value: T = initialValue
        set(newValue) {
            //this.prev = this.value
            onChange(newValue)
            field = newValue
        }
}

class Section(val indentation: Int, val title: String, val components: List<Component>) : JPanel() {
    val header: SectionHeader = SectionHeader(title, indentation, true).apply {
        val header = this
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                header.opened = !header.opened
                body.isVisible = header.opened
            }
        })
    }
    val body: SectionBody = SectionBody(components)
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(header)
        add(body)
    }
}

private val INDENTATION_SIZE = 24

class SectionHeader(val title: String, val indentation: Int, initiallyOpened: Boolean) : JPanel(BorderLayout()) {
    val label = JLabel("").apply {
        font = Font("monospaced", Font.BOLD, 15)
        this.border = CompoundBorder(this.border, EmptyBorder(10, 10 + INDENTATION_SIZE * indentation, 10, 10))
        //this.isOpaque = true
        //this.background = Color.BLUE
    }

    var opened: Boolean = true
        set(value) {
            field = value
            val labelSymbol = if (value) "⯆" else "⯈"
            label.text = "$labelSymbol $title"
        }

    init {
        maximumSize = Dimension(1024, 48)
        opened = initiallyOpened
        add(label, BorderLayout.CENTER)
    }
}

class SectionBody(components: List<Component>) : JPanel() {
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        for (comp in components) {
            add(comp)
        }

        //val obj = MyObject()

        //for (n in 0 until 5) {
        //    add(EditableNumberValue(obj::x.toEditableProperty()))
        //}
        //add(EditableNumberValue(obj::red.toEditableProperty()))
    }
}

class EditableNumberValue(val editProp: EditableNumericProperty<out Number>, val indentation: Int) : JPanel(GridLayout(1, 2)) {
    val FULL_CHANGE_WIDTH = 200
    var value = editProp.value.toDouble()
    val minimum = editProp.minimumValue?.toDouble() ?: -10000.0
    val maximum = editProp.maximumValue?.toDouble() ?: +10000.0
    val length = maximum - minimum
    lateinit var valueTextField: EditableLabel

    fun updateValue(value: Double) {
        this.value = value.coerceIn(minimum, maximum)
        if (editProp.clazz == Int::class) {
            valueTextField.text = this.value.toInt().toString()
            (editProp as EditableNumericProperty<Int>).value = this.value.toInt()
        } else {
            valueTextField.text = this.value.toStringDecimal(2)
            (editProp as EditableNumericProperty<Double>).value = this.value
        }
    }

    init {
        //preferredSize = Dimension(1024, 32)
        maximumSize = Dimension(1024, 32)
        add(JLabel(editProp.name).apply {
            this.border = CompoundBorder(this.border, EmptyBorder(10, 10 + INDENTATION_SIZE * indentation, 10, 10))
            //this.isOpaque = true
            //this.background = Color.RED
            this.cursor = Cursor(Cursor.E_RESIZE_CURSOR)
            val origin = Point(0, 0)
            var startValue = value
            this.addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    //super.mousePressed(e)
                    origin.location = e.point
                    startValue = value
                }
            })
            this.addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseDragged(e: MouseEvent) {
                    //super.mouseDragged(e)
                    val dx = e.point.x - origin.x
                    //val dy = e.point.y - origin.y
                    val deltaAbsolute = dx.absoluteValue.toDouble().convertRange(0.toDouble(), +FULL_CHANGE_WIDTH.toDouble(), 0.0, length)
                    val adjustedDeltaAbsolute = if (length > FULL_CHANGE_WIDTH) deltaAbsolute.pow(0.75) else deltaAbsolute
                    val delta = adjustedDeltaAbsolute.withSign(dx.sign)
                    println("(startValue=$startValue, dx=$dx, delta=$delta, minimum=$minimum, maximum=$maximum, FULL_CHANGE_WIDTH=$FULL_CHANGE_WIDTH)")
                    updateValue(startValue + delta)
                }
            })
        })
        add(EditableLabel("") {
            val double = it.toDoubleOrNull()
            if (double != null) {
                updateValue(double)
            } else {
                updateValue(value)
            }
        }.also { valueTextField = it })
        updateValue(value)
    }
}

class EditableLabel(initialText: String, val textChanged: (String) -> Unit) : JPanel(BorderLayout()) {
    val panel = this
    val label: JLabel = JLabel(initialText).apply {
        this.border = CompoundBorder(this.border, EmptyBorder(10, 10, 10, 10))
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = useTextField()
        })
    }

    val textField: JTextField = JTextField(initialText).apply {
        this.border = CompoundBorder(this.border, EmptyBorder(0, 2, 0, 2))
        //this.border = EmptyBorder(8, 8, 8, 8)
        val textField = this
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_ENTER -> useLabel()
                    else -> super.keyPressed(e)
                }
            }
        })
        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) = selectAll()
            override fun focusLost(e: FocusEvent) = useLabel()
        })
    }

    fun useTextField() {
        panel.removeAll()
        panel.add(textField, BorderLayout.CENTER)
        panel.revalidate()
        panel.repaint()
        textField.requestFocus()
    }

    fun useLabelNoChangeText() {
        panel.removeAll()
        panel.add(label, BorderLayout.CENTER)
        panel.revalidate()
        panel.repaint()
    }

    fun useLabel() {
        useLabelNoChangeText()
        label.text = textField.text
        textChanged(textField.text)
    }

    var text: String
        get() = label.text
        set(value) {
            if (this.components.firstOrNull() != label) {
                //useLabelNoChangeText()
                useLabel()
            }
            label.text = value
            textField.text = value
        }

    init {
        this.isFocusable = true
        add(label, BorderLayout.CENTER)
        //add(textField, BorderLayout.CENTER)
        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                useTextField()
            }

            override fun focusLost(e: FocusEvent) {
                super.focusLost(e)
            }
        })
    }
}


object PropertyPanelSample {
    @JvmStatic
    fun main(args: Array<String>) {
        val f = JFrame("hello")
        //f.contentPane.layout = BoxLayout(f.contentPane, BoxLayout.Y_AXIS)

        val obj = MyObject()

        val rootSection = EditableNodeList {
            add(EditableSection("Position", obj::x.toEditableProperty(), obj::y.toEditableProperty()))
            add(EditableSection("Color", obj::red.toEditableProperty(), obj::blue.toEditableProperty(), obj::green.toEditableProperty()))
        }

        //f.add(PropertyPanel(rootSection))
        f.contentPane.layout = LinearLayout(Direction.VERTICAL)
        f.contentPane.styled.createPropertyPanelWithEditor(JPanel(), rootSection)

        f.apply {
            val component = this
            isFocusable = true
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    //println("CLICKED")
                    component.requestFocus()
                }
            })
        }

        f.size = Dimension(512, 512)
        f.isVisible = true
        f.setLocationRelativeTo(null)
        //f.pack()
        f.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(event: WindowEvent) {
                exitProcess(0)
            }
        })
    }

    class MyObject {
        @Suppress("UnusedUnaryOperator")
        @DoubleSupportedRange(-1.0, +1.0)
        var x: Double = 0.0

        @Suppress("UnusedUnaryOperator")
        @DoubleSupportedRange(-1.0, +1.0)
        var y: Double = 0.0

        @IntSupportedRange(0, 255)
        var red: Int = 128

        @IntSupportedRange(0, 255)
        var green: Int = 128

        @IntSupportedRange(0, 255)
        var blue: Int = 128
    }
}
