import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.input.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.awt.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import java.awt.*
import java.awt.Graphics
import javax.swing.*
import java.awt.event.*
import java.text.*
import javax.swing.border.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.system.*

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

fun main() {
    val f = JFrame("hello")
    f.contentPane.layout = BoxLayout(f.contentPane, BoxLayout.Y_AXIS)

    val obj = MyObject()

    val rootSection = EditableNodeList {
        add(EditableSection("Position", obj::x.toEditableProperty(), obj::y.toEditableProperty()))
        add(EditableSection("Color", obj::red.toEditableProperty(), obj::blue.toEditableProperty(), obj::green.toEditableProperty()))
    }

    f.add(rootSection.toComponent())

    /*

    for (n in 0 until 5) {
        f.add(EditableNumberValue(obj::x.toEditableProperty()))
    }
    f.add(EditableNumberValue(obj::red.toEditableProperty()))
    */
    f.add(Box.Filler(Dimension(0, 0), Dimension(0, 1024), Dimension(0, 1024)))
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

@JvmName("toEditablePropertyDouble")
fun KMutableProperty0<Double>.toEditableProperty(): EditableNumericProperty<Double> {
    val range = this.annotations.filterIsInstance<DoubleSupportedRange>().firstOrNull()
    return EditableNumericProperty(
        name = this.name,
        clazz = this.returnType.jvmErasure as KClass<Double>,
        initialValue = this.get(),
        minimumValue = range?.min,
        maximumValue = range?.max,
    )
}

@JvmName("toEditablePropertyInt")
fun KMutableProperty0<Int>.toEditableProperty(): EditableNumericProperty<Int> {
    val range = this.annotations.filterIsInstance<IntSupportedRange>().firstOrNull()
    return EditableNumericProperty(
        name = this.name,
        clazz = this.returnType.jvmErasure as KClass<Int>,
        initialValue = this.get(),
        minimumValue = range?.min,
        maximumValue = range?.max,
    )
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
    }

    var opened: Boolean = true
        set(value) {
            field = value
            val labelSymbol = if (value) "⯆" else "⯈"
            label.text = "$labelSymbol $title"
        }

    init {
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
                    val dy = e.point.y - origin.y
                    //println("($dx, $dy)")
                    val delta = dx.toDouble().convertRange(-FULL_CHANGE_WIDTH.toDouble(), +FULL_CHANGE_WIDTH.toDouble(), minimum, maximum)
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
        this.border = CompoundBorder(this.border, EmptyBorder(8, 8, 8, 8))
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

fun main2() {
    val f = object : Frame("hello") {
        override fun update(g: Graphics) {
            paint(g)
        }

        override fun paintAll(p0: Graphics?) {
        }

        override fun paint(p0: Graphics) {
        }
    }

    var color = 0f
    val canvas = object : GLCanvas() {
        //override fun render(gl: KmlGl, g: Graphics) {
        //    gl.clearColor(0f, color, 0f, 1f)
        //    color += 0.01f
        //    gl.clear(X11KmlGl.COLOR_BUFFER_BIT)
        //}
    }
    f.add(canvas)

    f.size = Dimension(512, 512)
    f.isVisible = true
    f.setLocationRelativeTo(null)
    f.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(event: WindowEvent) {
            exitProcess(0)
        }
    })

    //launchImmediately(Dispatchers.Unconfined) {
    runBlocking {
        println("[1]")
        val korge = GLCanvasKorge(canvas)
        println("[2]")
        launchImmediately {
            korge.executeInContext {
                val rect = solidRect(50, 50, Colors.BLUE)
                rect.mouse {
                    click {
                        println("CLICKED!")
                    }
                }
                animate {  }
                tween(rect::x[400])
                //views.gameWindow.exit()
            }
        }
        println("[3]")
        delay(0.5.seconds)
        korge.close()
    }

    //Timer(100) { canvas.repaint() }.start()
}
