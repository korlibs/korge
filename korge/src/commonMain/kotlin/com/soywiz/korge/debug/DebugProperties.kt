package com.soywiz.korge.debug

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import kotlin.jvm.*
import kotlin.reflect.*

sealed class EditableNode {
    open val allBaseEditableProperty: List<BaseEditableProperty<*>> get() = listOf()
    open fun synchronizeProperties() {
        allBaseEditableProperty.fastForEach {
            if (it !== this@EditableNode) {
                it.synchronizeProperties()
            }
        }
    }
}

class EditableNodeList(val list: List<EditableNode>) : EditableNode() {
    constructor(vararg list: EditableNode) : this(list.toList())
    constructor(block: MutableList<EditableNode>.() -> Unit) : this(ArrayList<EditableNode>().apply(block))
    override val allBaseEditableProperty: List<BaseEditableProperty<*>> = this.list.flatMap { it.allBaseEditableProperty }
}

class EditableSection(val title: String, val list: List<EditableNode>) : EditableNode() {
    constructor(title: String, vararg list: EditableNode) : this(title, list.toList())
    constructor(title: String, block: MutableList<EditableNode>.() -> Unit) : this(title, ArrayList<EditableNode>().apply(block))
    override val allBaseEditableProperty: List<BaseEditableProperty<*>> = this.list.flatMap { it.allBaseEditableProperty }
}

abstract class BaseEditableProperty<T : Any>(
    val getValue: () -> T,
    val setValue: (T) -> Unit,
) : EditableNode() {
    abstract val name: String
    abstract val clazz: KClass<T>
    val onChange = Signal<T>()
    //var prev: T = initialValue
    val initialValue = getValue()

    fun updateValue(newValue: T) {
        setValue(newValue)
        onChange(newValue)
    }

    override fun synchronizeProperties() {
        onChange(value)
    }

    var value: T
        get() = getValue()
        set(newValue) {
            //this.prev = this.value
            if (value != newValue) {
                updateValue(newValue)
            }
        }

    override val allBaseEditableProperty: List<BaseEditableProperty<*>> = listOf(this)
}

data class InformativeProperty<T : Any>(
    val name: String,
    val value: T
) : EditableNode() {
    override val allBaseEditableProperty: List<BaseEditableProperty<*>> = listOf()
}

data class EditableEnumerableProperty<T : Any>(
    override val name: String,
    override val clazz: KClass<T>,
    val get: () -> T,
    val set: (T) -> Unit,
    var supportedValues: Set<T>
) : BaseEditableProperty<T>(get, set) {
    val onUpdateSupportedValues = Signal<Set<T>>()

    fun updateSupportedValues(set: Set<T>) {
        supportedValues = set
        onUpdateSupportedValues(set)
    }
}

data class EditableNumericProperty<T : Number>(
    override val name: String,
    override val clazz: KClass<T>,
    val get: () -> T,
    val set: (value: T) -> Unit,
    val minimumValue: T? = null,
    val maximumValue: T? = null,
    val step: T? = null,
    val supportOutOfRange: Boolean = false
) : BaseEditableProperty<T>(get, set) {
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
        get = { prop.get().convertRange(realMin, realMax, editMin, editMax) },
        set = { prop.set(it.convertRange(editMin, editMax, realMin, realMax)) },
        minimumValue = editMin,
        maximumValue = editMax,
        supportOutOfRange = supportOutOfRange
    )
}

@OptIn(ExperimentalStdlibApi::class)
@JvmName("toEditablePropertyFloat")
fun KMutableProperty0<Float>.toEditableProperty(
    min: Float? = null, max: Float? = null,
    transformedMin: Float? = null, transformedMax: Float? = null,
    name: String? = null,
    supportOutOfRange: Boolean = false
): EditableNumericProperty<Float> {
    val prop = this

    val editMin = min ?: 0f
    val editMax = max ?: 1000f

    val realMin = transformedMin ?: editMin
    val realMax = transformedMax ?: editMax

    return EditableNumericProperty(
        name = name ?: this.name,
        clazz = Float::class,
        get = { prop.get().convertRange(realMin, realMax, editMin, editMax) },
        set = { prop.set(it.convertRange(editMin, editMax, realMin, realMax)) },
        minimumValue = editMin,
        maximumValue = editMax,
        supportOutOfRange = supportOutOfRange
    )
}

@JvmName("toEditablePropertyInt")
fun KMutableProperty0<Int>.toEditableProperty(min: Int? = null, max: Int? = null): EditableNumericProperty<Int> {
    val prop = this
    return EditableNumericProperty(
        name = this.name,
        clazz = Int::class,
        get = { this.get() },
        set = { prop.set(it) },
        minimumValue = min,
        maximumValue = max,
    )
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
        get = { prop.get() },
        set = { prop.set(it) },
        supportedValues = enumConstants.toSet()
    )
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
