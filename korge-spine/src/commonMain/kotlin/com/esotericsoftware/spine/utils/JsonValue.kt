/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.esotericsoftware.spine.utils

import com.soywiz.kds.iterators.*
import kotlin.jvm.*

/** Container for a JSON object, array, string, double, long, boolean, or null.
 *
 *
 * JsonValue children are a linked list. Iteration of arrays or objects is easily done using a for loop, either with the enhanced
 * for loop syntactic sugar or like the example below. This is much more efficient than accessing children by index when there are
 * many children.<br></br>
 *
 * <pre>
 * JsonValue map = ...;
 * for (JsonValue entry = map.child; entry != null; entry = entry.next)
 * System.out.println(entry.name + " = " + entry.asString());
</pre> *
 *
 * @author Nathan Sweet
 */
class JsonValue {
    private var type: ValueType? = null
    private var stringValue: String? = null
    private var doubleValue: Double = 0.toDouble()
    var name: String? = null
    var children: List<JsonValue>? = null
    var child: JsonValue? = null
    var parent: JsonValue? = null
    var next: JsonValue? = null
    var prev: JsonValue? = null
    val size: Int get() = children?.size ?: 0
    val isString: Boolean get() = type == ValueType.stringValue
    val isNull: Boolean get() = type == ValueType.nullValue
    val isValue: Boolean get() = type == ValueType.stringValue || type == ValueType.doubleValue || type == ValueType.booleanValue || type == ValueType.nullValue

    constructor(type: ValueType) {
        this.type = type
    }

    constructor(value: String) {
        set(value)
    }

    constructor(value: Double, stringValue: String) {
        set(value, stringValue)
    }

    constructor(value: Boolean) {
        set(value)
    }

    operator fun get(index: Int): JsonValue? {
        var index = index
        var current = child
        while (current != null && index > 0) {
            index--
            current = current.next
        }
        return current
    }

    operator fun get(name: String): JsonValue? {
        var current = child
        while (current != null && (current.name == null || !current.name!!.equals(name, ignoreCase = true))) current = current.next
        return current
    }

    fun getSure(name: String): JsonValue = get(name) ?: throw IllegalArgumentException("Named value not found: $name")

    fun has(name: String): Boolean = get(name) != null

    inline fun fastForEach(block: (value: JsonValue) -> Unit) {
        children?.fastForEach(block)
        //var current = child
        //while (current != null) {
        //    block(current)
        //    current = current.next
        //}
    }

    fun require(name: String): JsonValue {
        fastForEach { current ->
            if (current.name?.equals(name, ignoreCase = true) == true) return current
        }
        error("Child not found with name: $name")
    }

    fun asString(): String? = when (type) {
        ValueType.stringValue -> stringValue
        ValueType.doubleValue -> if (stringValue != null) stringValue else (doubleValue).toString()
        ValueType.booleanValue -> if (doubleValue != 0.0) "true" else "false"
        ValueType.nullValue -> null
        else -> throw IllegalStateException("Value cannot be converted to string: " + type!!)
    }

    fun asDouble(): Double = when (type) {
        ValueType.stringValue -> (stringValue!!).toDouble()
        ValueType.doubleValue, ValueType.booleanValue -> doubleValue
        else -> throw IllegalStateException("Value cannot be converted to float: " + type!!)
    }

    fun asFloat(): Float = asDouble().toFloat()
    fun asInt(): Int = asDouble().toInt()
    fun asBoolean(): Boolean = asDouble() != 0.0

    fun checkArray() {
        check(type == ValueType.array) { "Value is not an array: " + type!! }
    }

    fun asFloatArray(): FloatArray = FloatArray(size).also { array ->
        checkArray()
        var i = 0
        fastForEach { array[i++] = it.asFloat() }
    }

    fun asShortArray(): ShortArray = ShortArray(size).also { array ->
        checkArray()
        var i = 0
        fastForEach { array[i++] = it.asInt().toShort() }
    }

    fun getChild(name: String): JsonValue? = get(name)?.child

    private inline fun <T> getAny(name: String, defaultValue: T, convert: (value: JsonValue) -> T): T {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else convert(child)
    }

    fun getString(name: String, defaultValue: String?): String? {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asString()
    }

    @JvmName("getStringNotNull")
    fun getString(name: String, defaultValue: String): String {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asString() ?: defaultValue
    }

    /** Finds the child with the specified name and returns it as a float. Returns defaultValue if not found.  */
    fun getFloat(name: String, defaultValue: Float): Float {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asFloat()
    }

    /** Finds the child with the specified name and returns it as an int. Returns defaultValue if not found.  */
    fun getInt(name: String, defaultValue: Int): Int {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asInt()
    }

    /** Finds the child with the specified name and returns it as a boolean. Returns defaultValue if not found.  */
    fun getBoolean(name: String, defaultValue: Boolean): Boolean {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asBoolean()
    }
    fun getString(name: String): String? = getSure(name).asString()
    fun getFloat(name: String): Float = getSure(name).asFloat()
    fun getInt(name: String): Int = getSure(name).asInt()

    /** @param value May be null.
     */
    fun set(value: String?) {
        stringValue = value
        type = if (value == null) ValueType.nullValue else ValueType.stringValue
    }

    operator fun set(value: Double, stringValue: String?) {
        doubleValue = value
        this.stringValue = stringValue
        type = ValueType.doubleValue
    }

    fun set(value: Boolean) {
        doubleValue = if (value) 1.0 else 0.0
        type = ValueType.booleanValue
    }

    override fun toString(): String = "JsonValue"

    enum class ValueType {
        `object`, array, stringValue, doubleValue, booleanValue, nullValue
    }

    companion object {
        private fun List<JsonValue>.populateSiblingsReferences(parent: JsonValue): JsonValue {
            parent.child = this.getOrNull(0)
            //parent.size = this.size
            for (n in 0 until size) {
                val child = this[n]
                child.parent = parent
                child.prev = this.getOrNull(n - 1)
                child.next = this.getOrNull(n + 1)
            }
            parent.children = this
            return parent
        }

        fun fromPrimitiveTree(value: Any?, parent: JsonValue? = null, name: String? = null): JsonValue {
            return when (value) {
                null -> JsonValue(ValueType.nullValue)
                is String -> JsonValue(value)
                is Boolean -> JsonValue(value)
                is Number -> JsonValue(value.toDouble(), value.toString())
                is List<*> -> {
                    JsonValue(ValueType.array).also { array ->
                        value.map { fromPrimitiveTree(it, array) }.populateSiblingsReferences(array)
                    }
                }
                is Map<*, *> -> {
                    JsonValue(ValueType.`object`).also { mapJsonValue ->
                        value.map {
                            val key = it.key as String
                            fromPrimitiveTree(it.value, mapJsonValue, key)
                        }.populateSiblingsReferences(mapJsonValue)
                    }
                }
                else -> {
                    TODO()
                }
            }.also {
                it.parent = parent
                it.name = name
            }
        }
    }
}
