package com.soywiz.korge.debug

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
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

data class DebugChangeEvent<T>(
    val newValue: T,
    val triggeredByUser: Boolean
)

abstract class BaseEditableProperty<T : Any>(
    val getValue: () -> T,
    val setValue: (T) -> Unit,
) : EditableNode() {
    abstract val name: String
    abstract val clazz: KClass<T>
    val onChange = Signal<DebugChangeEvent<T>>()
    //var prev: T = initialValue
    val initialValue = getValue()

    fun updateValue(newValue: T, triggeredByUser: Boolean = true) {
        setValue(newValue)
        onChange(DebugChangeEvent(newValue, triggeredByUser))
    }

    override fun synchronizeProperties() {
        onChange(DebugChangeEvent(value, false))
    }

    var value: T
        get() = getValue()
        set(newValue) {
            //this.prev = this.value
            //println("TEXT CHANGED: '$value' != '$newValue'")
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

data class EditableStringProperty(
    override val name: String,
    val kind: Kind,
    val views: Views?,
    val get: () -> String,
    val set: (String) -> Unit
) : BaseEditableProperty<String>(get, set) {
    enum class Kind { STRING, FILE }

    override val clazz = String::class
}

data class EditableColorProperty(
    override val name: String,
    val views: Views?,
    val get: () -> RGBA,
    val set: (RGBA) -> Unit
) : BaseEditableProperty<RGBA>(get, set) {
    override val clazz = RGBA::class
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
@JvmName("toEditablePropertyStringNullable")
fun KMutableProperty0<String?>.toEditableProperty(
    name: String? = null,
    kind: EditableStringProperty.Kind = EditableStringProperty.Kind.STRING,
    views: Views? = null
): EditableStringProperty {
    val prop = this

    return EditableStringProperty(
        name = name ?: this.name,
        kind = kind,
        views = views,
        get = { prop.get() ?: "" },
        set = {
            //println("TEXT CHANGED")
            prop.set(it.takeIf { it.isNotEmpty() })
        }
    )
}

@OptIn(ExperimentalStdlibApi::class)
@JvmName("toEditablePropertyString")
fun KMutableProperty0<String>.toEditableProperty(
    name: String? = null,
    kind: EditableStringProperty.Kind = EditableStringProperty.Kind.STRING,
    views: Views? = null,
): EditableStringProperty {
    val prop = this

    return EditableStringProperty(
        name = name ?: this.name,
        kind = kind,
        views = views,
        get = { prop.get() },
        set = { prop.set(it) },
    )
}

@OptIn(ExperimentalStdlibApi::class)
@JvmName("toEditablePropertyColor")
fun KMutableProperty0<RGBA>.toEditableProperty(
    name: String? = null,
    views: Views? = null,
): EditableColorProperty {
    val prop = this

    return EditableColorProperty(
        name = name ?: this.name,
        views = views,
        get = { prop.get() },
        set = { prop.set(it) }
    )
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
