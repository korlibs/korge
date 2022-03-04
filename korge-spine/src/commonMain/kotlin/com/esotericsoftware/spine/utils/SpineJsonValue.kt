package com.esotericsoftware.spine.utils

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*

internal class SpineJsonValue {
    enum class ValueType { OBJECT, ARRAY, STRING, DOUBLE, BOOLEAN, NULL }

    private var type: ValueType? = null
    private var stringValue: String? = null
    private var doubleValue: Double = 0.toDouble()

    var name: String? = null
    var children: List<SpineJsonValue>? = null
    var map: Map<String, SpineJsonValue>? = null
    val size: Int get() = children?.size ?: 0
    val isString: Boolean get() = type == ValueType.STRING
    val isNull: Boolean get() = type == ValueType.NULL
    val isValue: Boolean get() = type == ValueType.STRING || type == ValueType.DOUBLE || type == ValueType.BOOLEAN || type == ValueType.NULL

    constructor(type: ValueType) { this.type = type }

    constructor(type: ValueType, children: List<SpineJsonValue>) {
        this.type = type
        this.children = children
        if (type == ValueType.OBJECT) {
            this.map = children.associateBy { it.name!! }.toCaseInsensitiveMap()
        }
    }

    constructor(value: String) { set(value) }
    constructor(value: Double, stringValue: String) { set(value, stringValue) }
    constructor(value: Boolean) { set(value) }

    operator fun get(index: Int): SpineJsonValue? = children?.getOrNull(index)
    operator fun get(name: String): SpineJsonValue? = map?.get(name)

    fun getSure(name: String): SpineJsonValue = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
    fun has(name: String): Boolean = get(name) != null
    inline fun fastForEach(block: (value: SpineJsonValue) -> Unit) { children?.fastForEach(block) }
    fun require(name: String): SpineJsonValue {
        fastForEach { current -> if (current.name?.equals(name, ignoreCase = true) == true) return current }
        error("Child not found with name: $name")
    }

    fun asString(): String? = when (type) {
        ValueType.STRING -> stringValue
        ValueType.DOUBLE -> if (stringValue != null) stringValue else (doubleValue).toString()
        ValueType.BOOLEAN -> if (doubleValue != 0.0) "true" else "false"
        ValueType.NULL -> null
        else -> throw IllegalStateException("Value cannot be converted to string: ${type!!}")
    }

    fun asDouble(): Double = when (type) {
        ValueType.STRING -> (stringValue!!).toDouble()
        ValueType.DOUBLE, ValueType.BOOLEAN -> doubleValue
        else -> throw IllegalStateException("Value cannot be converted to float: ${type!!}")
    }

    fun asFloat(): Float = asDouble().toFloat()
    fun asInt(): Int = asDouble().toInt()
    fun asBoolean(): Boolean = asDouble() != 0.0
    fun checkArray() = check(type == ValueType.ARRAY) { "Value is not an array: " + type!! }

    private inline fun <T> asAnyArray(new: (size: Int) -> T, set: (array: T, index: Int, value: SpineJsonValue) -> Unit): T =
        checkArray().let { new(size).also { array -> children?.fastForEachWithIndex { index, value -> set(array, index, value) } } }

    fun asFloatArray(): FloatArray = asAnyArray({ FloatArray(it) }, { array, index, value -> array[index] = value.asFloat() })
    fun asShortArray(): ShortArray = asAnyArray({ ShortArray(it) }, { array, index, value -> array[index] = value.asInt().toShort() })

    private inline fun <T> getAny(name: String, defaultValue: T, convert: (value: SpineJsonValue) -> T): T {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else convert(child)
    }

    fun getString(name: String, defaultValue: String?): String? = getAny(name, defaultValue) { it.asString() }
    fun getStringNotNull(name: String, defaultValue: String): String = getAny(name, defaultValue) { it?.asString()?: defaultValue }
    fun getFloat(name: String, defaultValue: Float): Float = getAny(name, defaultValue) { it.asFloat() }
    fun getInt(name: String, defaultValue: Int): Int = getAny(name, defaultValue) { it.asInt() }
    fun getBoolean(name: String, defaultValue: Boolean): Boolean = getAny(name, defaultValue) { it.asBoolean() }
    fun getString(name: String): String? = getSure(name).asString()
    fun getFloat(name: String): Float = getSure(name).asFloat()
    fun getInt(name: String): Int = getSure(name).asInt()

    fun set(value: String?) {
        stringValue = value
        type = if (value == null) ValueType.NULL else ValueType.STRING
    }

    operator fun set(value: Double, stringValue: String?) {
        doubleValue = value
        this.stringValue = stringValue
        type = ValueType.DOUBLE
    }

    fun set(value: Boolean) {
        doubleValue = if (value) 1.0 else 0.0
        type = ValueType.BOOLEAN
    }

    override fun toString(): String = "JsonValue"

    companion object {
        fun fromPrimitiveTree(value: Any?, name: String? = null): SpineJsonValue = when (value) {
            null -> SpineJsonValue(ValueType.NULL)
            is String -> SpineJsonValue(value)
            is Boolean -> SpineJsonValue(value)
            is Number -> SpineJsonValue(value.toDouble(), value.toString())
            is List<*> -> SpineJsonValue(ValueType.ARRAY, value.map { fromPrimitiveTree(it) })
            is Map<*, *> -> SpineJsonValue(ValueType.OBJECT, value.map { fromPrimitiveTree(it.value, it.key as String) })
            is DoubleArrayList -> SpineJsonValue(ValueType.ARRAY, value.map { fromPrimitiveTree(it) })
            else -> TODO()
        }.also {
            it.name = name
        }
    }
}
