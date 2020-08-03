package com.soywiz.korge.debug

import com.soywiz.kmem.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import kotlin.jvm.*
import kotlin.reflect.*

sealed class EditableNode {
    open fun getAllBaseEditableProperty(): List<BaseEditableProperty<*>> = listOf()
}

class EditableNodeList(val list: List<EditableNode>) : EditableNode() {
    constructor(vararg list: EditableNode) : this(list.toList())
    constructor(block: MutableList<EditableNode>.() -> Unit) : this(ArrayList<EditableNode>().apply(block))
    override fun getAllBaseEditableProperty(): List<BaseEditableProperty<*>> = this.list.flatMap { it.getAllBaseEditableProperty() }
}

class EditableSection(val title: String, val list: List<EditableNode>) : EditableNode() {
    constructor(title: String, vararg list: EditableNode) : this(title, list.toList())
    constructor(title: String, block: MutableList<EditableNode>.() -> Unit) : this(title, ArrayList<EditableNode>().apply(block))
    override fun getAllBaseEditableProperty(): List<BaseEditableProperty<*>> = this.list.flatMap { it.getAllBaseEditableProperty() }
}

abstract class BaseEditableProperty<T : Any>(initialValue: T) : EditableNode() {
    abstract val name: String
    abstract val clazz: KClass<T>
    val onChange = Signal<T>()
    //var prev: T = initialValue
    var value: T = initialValue
        set(newValue) {
            //this.prev = this.value
            if (field != newValue) {
                onChange(newValue)
                field = newValue
            }
        }
    override fun getAllBaseEditableProperty(): List<BaseEditableProperty<*>> = listOf(this)
}

data class InformativeProperty<T : Any>(
    val name: String,
    val value: T
) : EditableNode() {
    override fun getAllBaseEditableProperty(): List<BaseEditableProperty<*>> = listOf()
}

data class EditableEnumerableProperty<T : Any>(
    override val name: String,
    override val clazz: KClass<T>,
    val initialValue: T,
    var supportedValues: Set<T>
) : BaseEditableProperty<T>(initialValue) {
    val onUpdateSupportedValues = Signal<Set<T>>()

    fun updateSupportedValues(set: Set<T>) {
        supportedValues = set
        onUpdateSupportedValues(set)
    }
}

data class EditableNumericProperty<T : Number>(
    override val name: String,
    override val clazz: KClass<T>,
    val initialValue: T,
    val minimumValue: T? = null,
    val maximumValue: T? = null,
    val step: T? = null,
    val supportOutOfRange: Boolean = false
) : BaseEditableProperty<T>(initialValue) {
}

data class EditableButtonProperty(
    val title: String,
    val callback: () -> Unit
) : EditableNode() {
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

    val editMin = min ?: 0.0
    val editMax = max ?: 1000.0

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
    return EditableNumericProperty(
        name = this.name,
        clazz = Int::class,
        initialValue = this.get(),
        minimumValue = min,
        maximumValue = max,
    ).also {
        it.onChange { prop.set(it) }
    }
}

inline fun <reified T : Enum<*>> KMutableProperty0<T>.toEditableProperty(enumConstants: Array<T>, name: String? = null): EditableEnumerableProperty<T> {
    return toEditableProperty(name, T::class, enumConstants)
}

fun <T : Enum<*>> KMutableProperty0<T>.toEditableProperty(
    name: String? = null,
    clazz: KClass<T>,
    enumConstants: Array<T>
): EditableEnumerableProperty<T> {
    val prop = this
    return EditableEnumerableProperty<T>(
        name = name ?: this.name,
        clazz = clazz,
        initialValue = prop.get(),
        supportedValues = enumConstants.toSet()
    ).also {
        it.onChange { prop.set(it) }
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
