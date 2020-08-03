package com.soywiz.korge.awt

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korge.debug.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import java.awt.*
import java.awt.event.*
import java.util.*
import javax.swing.*
import javax.swing.border.*
import kotlin.collections.ArrayList
import kotlin.coroutines.*
import kotlin.math.*
import kotlin.reflect.*
import kotlin.system.*

//typealias MyJScrollPane = JScrollPane
//typealias MyJComboBox<T> = JComboBox<T>
//typealias MyJScrollPane = com.intellij.ui.components.JBScrollPane
//typealias MyJComboBox<T> = com.intellij.openapi.ui.ComboBox<T>

var myComponentFactory = MyComponentFactory()

fun Styled<out Container>.createPropertyPanelWithEditor(
    editor: Component,
    rootNode: EditableNode,
    coroutineContext: CoroutineContext = EmptyCoroutineContext
) {
    verticalStack {
        fill()
        horizontalStack {
            fill()
            verticalStack {
                //minWidth = 32.pt
                minWidth = 360.pt
                fill()
                add(editor.styled {
                    fill()
                })
            }
            verticalStack {
                minWidth = 360.pt
                width = minWidth
                fillHeight()
                add(PropertyPanel(rootNode, coroutineContext).styled {
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

class PropertyPanel(val rootNode: EditableNode, var coroutineContext: CoroutineContext) : JPanel() {
    val contentPane = JPanel()
    init {
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        add(myComponentFactory.scrollPane(contentPane))
    }
    init {
        //contentPane.preferredSize = Dimension(500, 400)

        contentPane.add(rootNode.toComponent(coroutineContext))
        //contentPane.add(Box.Filler(Dimension(0, 0), Dimension(0, 1024), Dimension(0, 1024)))
        contentPane.add(Box.createVerticalGlue())
    }
}

@OptIn(ExperimentalStdlibApi::class)
@JvmName("toEditablePropertyDouble")
fun KMutableProperty0<Double>.toEditableProperty(
    min: Double? = null, max: Double? = null,
    transformedMin: Double? = null, transformedMax: Double? = null,
    name: String? = null,
    supportOutOfRange: Boolean = false
): EditableNumericProperty<Double> {
    val prop = this
    val range = this.annotations.filterIsInstance<DoubleSupportedRange>().firstOrNull()

    val editMin = min ?: range?.min ?: 0.0
    val editMax = max ?: range?.max ?: 1000.0

    val realMin = transformedMin ?: editMin
    val realMax = transformedMax ?: editMax

    return EditableNumericProperty(
        name = name ?: this.name,
        clazz = Double::class,
        initialValue = this.get().convertRange(realMin, realMax, editMin, editMax),
        minimumValue = editMin,
        maximumValue = editMax,
        supportOutOfRange = supportOutOfRange
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
        clazz = Int::class,
        initialValue = this.get(),
        minimumValue = min ?: range?.min,
        maximumValue = max ?: range?.max,
    ).also {
        it.onChange { prop.set(it) }
    }
}

inline fun <reified T : Enum<*>> KMutableProperty0<T>.toEditableProperty(name: String? = null): EditableEnumerableProperty<T> =
    toEditableProperty(name, T::class)

fun <T : Enum<*>> KMutableProperty0<T>.toEditableProperty(
    name: String? = null,
    clazz: KClass<T>
): EditableEnumerableProperty<T> {
    val prop = this
    return EditableEnumerableProperty<T>(
        name = name ?: this.name,
        clazz = clazz,
        initialValue = prop.get(),
        supportedValues = clazz.java.enumConstants.toSet() as Set<T>
    ).also {
        it.onChange { prop.set(it) }
    }
}

annotation class IntSupportedRange(val min: Int, val max: Int)
annotation class DoubleSupportedRange(val min: Double, val max: Double)

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
        add(JLabel(" "))

        //val obj = MyObject()

        //for (n in 0 until 5) {
        //    add(EditableNumberValue(obj::x.toEditableProperty()))
        //}
        //add(EditableNumberValue(obj::red.toEditableProperty()))
    }
}

class EditableListValue<T : Any>(val context: CoroutineContext, val editProp: EditableEnumerableProperty<T>, val indentation: Int) : JPanel(GridLayout(1, 2)) {
    val FULL_CHANGE_WIDTH = 200
    lateinit var valueTextField: EditableLabel

    var stringToType: Map<String, T> = mapOf()

    fun updateStringToType() {
        stringToType = editProp.supportedValues.associateBy { it.toString() }.toCaseInsensitiveMap()
    }

    fun updateValue(value: T?) {
        if (value != null) {
            launchImmediately(context) {
                editProp.value = value
            }
            valueTextField.text = value.toString()
        }
    }

    init {
        updateStringToType()
        //preferredSize = Dimension(1024, 32)
        maximumSize = Dimension(1024, 32)
        add(JLabel(editProp.name).apply {
            this.border = CompoundBorder(this.border, EmptyBorder(10, 10 + INDENTATION_SIZE * indentation, 10, 10))
            size = Dimension(128, 32)
            maximumSize = Dimension(128, 32)
        })
        add(EditableLabel("", stringToType.keys) {
            updateValue(stringToType[it] ?: editProp.value)
        }.also { valueTextField = it })
        editProp.onUpdateSupportedValues {
            updateStringToType()
            valueTextField.supportedValues = it.map { it.toString() }.toSet()
        }
        updateValue(editProp.value)
    }
}
class EditableNumberValue(val context: CoroutineContext, val editProp: EditableNumericProperty<out Number>, val indentation: Int) : JPanel(GridLayout(1, 2)) {
    val FULL_CHANGE_WIDTH = 200
    var value = editProp.value?.toDouble() ?: 0.0
    val minimum = editProp.minimumValue?.toDouble() ?: -10000.0
    val maximum = editProp.maximumValue?.toDouble() ?: +10000.0
    val length = maximum - minimum
    lateinit var valueTextField: EditableLabel

    fun updateValue(value: Double) {
        this.value = if (editProp.supportOutOfRange) value else value.coerceIn(minimum, maximum)
        if (editProp.clazz == Int::class) {
            valueTextField.text = this.value.toInt().toString()
            launchImmediately(context) {
                (editProp as EditableNumericProperty<Int>).value = this.value.toInt()
            }
        } else {
            valueTextField.text = this.value.toStringDecimal(2)
            launchImmediately(context) {
                (editProp as EditableNumericProperty<Double>).value = this.value
            }
        }
    }

    init {
        //preferredSize = Dimension(1024, 32)
        maximumSize = Dimension(1024, 32)
        add(JLabel(editProp.name).apply {
            this.border = CompoundBorder(this.border, EmptyBorder(10, 10 + INDENTATION_SIZE * indentation, 10, 10))
            size = Dimension(128, 32)
            maximumSize = Dimension(128, 32)
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
                    //println("(startValue=$startValue, dx=$dx, delta=$delta, minimum=$minimum, maximum=$maximum, FULL_CHANGE_WIDTH=$FULL_CHANGE_WIDTH)")
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

class EditableLabel(initialText: String, supportedValues: Set<String>? = null, val textChanged: (String) -> Unit) : JPanel(BorderLayout()) {
    var supportedValues: Set<String>? = supportedValues
        set(value) {
            field = value
            if (value != null) {
                comboBox.model = DefaultComboBoxModel(Vector(value.toList()))
            }
        }

    val panel = this
    val label: JLabel = JLabel(initialText).apply {
        this.border = CompoundBorder(this.border, EmptyBorder(10, 10, 10, 10))
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = startEditing()
        })
    }

    val textField: JTextField = JTextField(initialText).apply {
        this.border = CompoundBorder(this.border, EmptyBorder(0, 2, 0, 2))
        //this.border = EmptyBorder(8, 8, 8, 8)
        val textField = this
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_ENTER -> stopEditing()
                    else -> super.keyPressed(e)
                }
            }
        })
        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) = selectAll()
            override fun focusLost(e: FocusEvent) = stopEditing()
        })
    }

    val comboBox: JComboBox<String> = myComponentFactory.comboBox((supportedValues ?: setOf()).toTypedArray()).apply {
        this.border = CompoundBorder(this.border, EmptyBorder(0, 2, 0, 2))
        //this.border = EmptyBorder(8, 8, 8, 8)
        addItemListener {
            //stopEditing()
            synchronizeEditText()
        }
        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) = this@apply.showPopup()
            override fun focusLost(e: FocusEvent) = stopEditing()
        })
    }

    val editComponent: JComponent = if (supportedValues != null) comboBox else textField

    fun synchronizeEditText() {
        val editText = when (editComponent) {
            is JTextField -> editComponent.text
            is JComboBox<*> -> editComponent.selectedItem?.toString() ?: ""
            else -> TODO("editComponent: $editComponent")
        }
        if (label.text != editText) {
            label.text = editText
            textChanged(editText)
        }
    }

    fun startEditing() {
        panel.removeAll()
        panel.add(editComponent, BorderLayout.CENTER)
        panel.revalidate()
        panel.repaint()
        editComponent.requestFocus()
    }

    fun stopEditing() {
        panel.removeAll()
        panel.add(label, BorderLayout.CENTER)
        panel.revalidate()
        panel.repaint()
        synchronizeEditText()
    }

    var text: String
        get() = label.text
        set(value) {
            if (this.components.firstOrNull() != label) {
                //useLabelNoChangeText()
                stopEditing()
            }
            label.text = value
            when (editComponent) {
                is JTextField -> editComponent.text = value
                is JComboBox<*> -> editComponent.selectedItem = value
                else -> TODO("editComponent: $editComponent")
            }
        }

    init {
        this.isFocusable = true
        add(label, BorderLayout.CENTER)
        //add(textField, BorderLayout.CENTER)
        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                startEditing()
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
            add(EditableSection("Enum", obj::z.toEditableProperty()))
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

    enum class MyEnum { A, B, C }

    class MyObject {
        var z: MyEnum = MyEnum.A

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
