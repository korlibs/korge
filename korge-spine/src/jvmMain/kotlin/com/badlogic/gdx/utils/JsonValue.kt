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

package com.badlogic.gdx.utils

import java.io.IOException
import java.io.Writer
import java.util.NoSuchElementException

import com.badlogic.gdx.utils.JsonWriter.OutputType

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
class JsonValue : Iterable<JsonValue> {
    private var type: ValueType? = null

    /** May be null.  */
    private var stringValue: String? = null
    private var doubleValue: Double = 0.toDouble()
    private var longValue: Long = 0

    /** @param name May be null.
     */
    @JvmField
    var name: String? = null

    /** May be null.  */
    @JvmField
    var child: JsonValue? = null
    @JvmField
    var parent: JsonValue? = null

    /** May be null. When changing this field the parent [.size] may need to be changed.  */
    /** Sets the next sibling of this value. Does not change the parent [.size].
     * @param next May be null.
     */
    @JvmField
    var next: JsonValue? = null
    /** Sets the next sibling of this value. Does not change the parent [.size].
     * @param prev May be null.
     */
    @JvmField
    var prev: JsonValue? = null
    @JvmField
    var size: Int = 0

    /** Returns true if there are not children in the array or object.  */
    val isEmpty: Boolean
        get() = size == 0

    val isArray: Boolean
        get() = type == ValueType.array

    val isObject: Boolean
        get() = type == ValueType.`object`

    val isString: Boolean
        get() = type == ValueType.stringValue

    /** Returns true if this is a double or long value.  */
    val isNumber: Boolean
        get() = type == ValueType.doubleValue || type == ValueType.longValue

    val isDouble: Boolean
        get() = type == ValueType.doubleValue

    val isLong: Boolean
        get() = type == ValueType.longValue

    val isBoolean: Boolean
        get() = type == ValueType.booleanValue

    val isNull: Boolean
        get() = type == ValueType.nullValue

    /** Returns true if this is not an array or object.  */
    val isValue: Boolean
        get() {
            when (type) {
                JsonValue.ValueType.stringValue, JsonValue.ValueType.doubleValue, JsonValue.ValueType.longValue, JsonValue.ValueType.booleanValue, JsonValue.ValueType.nullValue -> return true
            }
            return false
        }

    constructor(type: ValueType) {
        this.type = type
    }

    /** @param value May be null.
     */
    constructor(value: String) {
        set(value)
    }

    constructor(value: Double) {
        set(value, null)
    }

    constructor(value: Long) {
        set(value, null)
    }

    constructor(value: Double, stringValue: String) {
        set(value, stringValue)
    }

    constructor(value: Long, stringValue: String) {
        set(value, stringValue)
    }

    constructor(value: Boolean) {
        set(value)
    }

    /** Returns the child at the specified index. This requires walking the linked list to the specified entry, see
     * [JsonValue] for how to iterate efficiently.
     * @return May be null.
     */
    operator fun get(index: Int): JsonValue? {
        var index = index
        var current = child
        while (current != null && index > 0) {
            index--
            current = current.next
        }
        return current
    }

    /** Returns the child with the specified name.
     * @return May be null.
     */
    operator fun get(name: String): JsonValue? {
        var current = child
        while (current != null && (current.name == null || !current.name!!.equals(name, ignoreCase = true)))
            current = current.next
        return current
    }

    /** Returns true if a child with the specified name exists.  */
    fun has(name: String): Boolean {
        return get(name) != null
    }

    /** Returns the child at the specified index. This requires walking the linked list to the specified entry, see
     * [JsonValue] for how to iterate efficiently.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun require(index: Int): JsonValue {
        var index = index
        var current = child
        while (current != null && index > 0) {
            index--
            current = current.next
        }
        requireNotNull(current) { "Child not found with index: $index" }
        return current
    }

    /** Returns the child with the specified name.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun require(name: String): JsonValue {
        var current = child
        while (current != null && (current.name == null || !current.name!!.equals(name, ignoreCase = true)))
            current = current.next
        requireNotNull(current) { "Child not found with name: $name" }
        return current
    }

    /** Removes the child with the specified index. This requires walking the linked list to the specified entry, see
     * [JsonValue] for how to iterate efficiently.
     * @return May be null.
     */
    fun remove(index: Int): JsonValue? {
        val child = get(index) ?: return null
        if (child.prev == null) {
            this.child = child.next
            if (this.child != null) this.child!!.prev = null
        } else {
            child.prev!!.next = child.next
            if (child.next != null) child.next!!.prev = child.prev
        }
        size--
        return child
    }

    /** Removes the child with the specified name.
     * @return May be null.
     */
    fun remove(name: String): JsonValue? {
        val child = get(name) ?: return null
        if (child.prev == null) {
            this.child = child.next
            if (this.child != null) this.child!!.prev = null
        } else {
            child.prev!!.next = child.next
            if (child.next != null) child.next!!.prev = child.prev
        }
        size--
        return child
    }

    /** Returns true if there are one or more children in the array or object.  */
    fun notEmpty(): Boolean {
        return size > 0
    }


    @Deprecated("Use {@link #size} instead. Returns this number of children in the array or object. ")
    fun size(): Int {
        return size
    }

    /** Returns this value as a string.
     * @return May be null if this value is null.
     * @throws IllegalStateException if this an array or object.
     */
    fun asString(): String? {
        when (type) {
            JsonValue.ValueType.stringValue -> return stringValue
            JsonValue.ValueType.doubleValue -> return if (stringValue != null) stringValue else java.lang.Double.toString(doubleValue)
            JsonValue.ValueType.longValue -> return if (stringValue != null) stringValue else java.lang.Long.toString(longValue)
            JsonValue.ValueType.booleanValue -> return if (longValue != 0L) "true" else "false"
            JsonValue.ValueType.nullValue -> return null
        }
        throw IllegalStateException("Value cannot be converted to string: " + type!!)
    }

    /** Returns this value as a float.
     * @throws IllegalStateException if this an array or object.
     */
    fun asFloat(): Float {
        when (type) {
            JsonValue.ValueType.stringValue -> return java.lang.Float.parseFloat(stringValue!!)
            JsonValue.ValueType.doubleValue -> return doubleValue.toFloat()
            JsonValue.ValueType.longValue -> return longValue.toFloat()
            JsonValue.ValueType.booleanValue -> return (if (longValue != 0L) 1 else 0).toFloat()
        }
        throw IllegalStateException("Value cannot be converted to float: " + type!!)
    }

    /** Returns this value as a double.
     * @throws IllegalStateException if this an array or object.
     */
    fun asDouble(): Double {
        when (type) {
            JsonValue.ValueType.stringValue -> return java.lang.Double.parseDouble(stringValue!!)
            JsonValue.ValueType.doubleValue -> return doubleValue
            JsonValue.ValueType.longValue -> return longValue.toDouble()
            JsonValue.ValueType.booleanValue -> return (if (longValue != 0L) 1 else 0).toDouble()
        }
        throw IllegalStateException("Value cannot be converted to double: " + type!!)
    }

    /** Returns this value as a long.
     * @throws IllegalStateException if this an array or object.
     */
    fun asLong(): Long {
        when (type) {
            JsonValue.ValueType.stringValue -> return java.lang.Long.parseLong(stringValue!!)
            JsonValue.ValueType.doubleValue -> return doubleValue.toLong()
            JsonValue.ValueType.longValue -> return longValue
            JsonValue.ValueType.booleanValue -> return (if (longValue != 0L) 1 else 0).toLong()
        }
        throw IllegalStateException("Value cannot be converted to long: " + type!!)
    }

    /** Returns this value as an int.
     * @throws IllegalStateException if this an array or object.
     */
    fun asInt(): Int {
        when (type) {
            JsonValue.ValueType.stringValue -> return Integer.parseInt(stringValue!!)
            JsonValue.ValueType.doubleValue -> return doubleValue.toInt()
            JsonValue.ValueType.longValue -> return longValue.toInt()
            JsonValue.ValueType.booleanValue -> return if (longValue != 0L) 1 else 0
        }
        throw IllegalStateException("Value cannot be converted to int: " + type!!)
    }

    /** Returns this value as a boolean.
     * @throws IllegalStateException if this an array or object.
     */
    fun asBoolean(): Boolean {
        when (type) {
            JsonValue.ValueType.stringValue -> return stringValue!!.equals("true", ignoreCase = true)
            JsonValue.ValueType.doubleValue -> return doubleValue != 0.0
            JsonValue.ValueType.longValue -> return longValue != 0L
            JsonValue.ValueType.booleanValue -> return longValue != 0L
        }
        throw IllegalStateException("Value cannot be converted to boolean: " + type!!)
    }

    /** Returns this value as a byte.
     * @throws IllegalStateException if this an array or object.
     */
    fun asByte(): Byte {
        when (type) {
            JsonValue.ValueType.stringValue -> return java.lang.Byte.parseByte(stringValue!!)
            JsonValue.ValueType.doubleValue -> return doubleValue.toByte()
            JsonValue.ValueType.longValue -> return longValue.toByte()
            JsonValue.ValueType.booleanValue -> return if (longValue != 0L) 1.toByte() else 0
        }
        throw IllegalStateException("Value cannot be converted to byte: " + type!!)
    }

    /** Returns this value as a short.
     * @throws IllegalStateException if this an array or object.
     */
    fun asShort(): Short {
        when (type) {
            JsonValue.ValueType.stringValue -> return java.lang.Short.parseShort(stringValue!!)
            JsonValue.ValueType.doubleValue -> return doubleValue.toShort()
            JsonValue.ValueType.longValue -> return longValue.toShort()
            JsonValue.ValueType.booleanValue -> return if (longValue != 0L) 1.toShort() else 0
        }
        throw IllegalStateException("Value cannot be converted to short: " + type!!)
    }

    /** Returns this value as a char.
     * @throws IllegalStateException if this an array or object.
     */
    fun asChar(): Char {
        when (type) {
            JsonValue.ValueType.stringValue -> return if (stringValue!!.length == 0) 0.toChar() else stringValue!![0]
            JsonValue.ValueType.doubleValue -> return doubleValue.toChar()
            JsonValue.ValueType.longValue -> return longValue.toChar()
            JsonValue.ValueType.booleanValue -> return if (longValue != 0L) 1.toChar() else 0.toChar()
        }
        throw IllegalStateException("Value cannot be converted to char: " + type!!)
    }

    /** Returns the children of this value as a newly allocated String array.
     * @throws IllegalStateException if this is not an array.
     */
    fun asStringArray(): Array<String> {
        check(type == ValueType.array) { "Value is not an array: " + type!! }
        val array = arrayOfNulls<String>(size)
        var i = 0
        var value = child
        while (value != null) {
            val v: String?
            when (value.type) {
                JsonValue.ValueType.stringValue -> v = value.stringValue
                JsonValue.ValueType.doubleValue -> v = if (stringValue != null) stringValue else java.lang.Double.toString(value.doubleValue)
                JsonValue.ValueType.longValue -> v = if (stringValue != null) stringValue else java.lang.Long.toString(value.longValue)
                JsonValue.ValueType.booleanValue -> v = if (value.longValue != 0L) "true" else "false"
                JsonValue.ValueType.nullValue -> v = null
                else -> throw IllegalStateException("Value cannot be converted to string: " + value.type!!)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array as Array<String>
    }

    /** Returns the children of this value as a newly allocated float array.
     * @throws IllegalStateException if this is not an array.
     */
    fun asFloatArray(): FloatArray {
        check(type == ValueType.array) { "Value is not an array: " + type!! }
        val array = FloatArray(size)
        var i = 0
        var value = child
        while (value != null) {
            val v: Float
            when (value.type) {
                JsonValue.ValueType.stringValue -> v = java.lang.Float.parseFloat(value.stringValue!!)
                JsonValue.ValueType.doubleValue -> v = value.doubleValue.toFloat()
                JsonValue.ValueType.longValue -> v = value.longValue.toFloat()
                JsonValue.ValueType.booleanValue -> v = (if (value.longValue != 0L) 1 else 0).toFloat()
                else -> throw IllegalStateException("Value cannot be converted to float: " + value.type!!)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /** Returns the children of this value as a newly allocated double array.
     * @throws IllegalStateException if this is not an array.
     */
    fun asDoubleArray(): DoubleArray {
        check(type == ValueType.array) { "Value is not an array: " + type!! }
        val array = DoubleArray(size)
        var i = 0
        var value = child
        while (value != null) {
            val v: Double
            when (value.type) {
                JsonValue.ValueType.stringValue -> v = java.lang.Double.parseDouble(value.stringValue!!)
                JsonValue.ValueType.doubleValue -> v = value.doubleValue
                JsonValue.ValueType.longValue -> v = value.longValue.toDouble()
                JsonValue.ValueType.booleanValue -> v = (if (value.longValue != 0L) 1 else 0).toDouble()
                else -> throw IllegalStateException("Value cannot be converted to double: " + value.type!!)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /** Returns the children of this value as a newly allocated long array.
     * @throws IllegalStateException if this is not an array.
     */
    fun asLongArray(): LongArray {
        check(type == ValueType.array) { "Value is not an array: " + type!! }
        val array = LongArray(size)
        var i = 0
        var value = child
        while (value != null) {
            val v: Long
            when (value.type) {
                JsonValue.ValueType.stringValue -> v = java.lang.Long.parseLong(value.stringValue!!)
                JsonValue.ValueType.doubleValue -> v = value.doubleValue.toLong()
                JsonValue.ValueType.longValue -> v = value.longValue
                JsonValue.ValueType.booleanValue -> v = (if (value.longValue != 0L) 1 else 0).toLong()
                else -> throw IllegalStateException("Value cannot be converted to long: " + value.type!!)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /** Returns the children of this value as a newly allocated int array.
     * @throws IllegalStateException if this is not an array.
     */
    fun asIntArray(): IntArray {
        check(type == ValueType.array) { "Value is not an array: " + type!! }
        val array = IntArray(size)
        var i = 0
        var value = child
        while (value != null) {
            val v: Int
            when (value.type) {
                JsonValue.ValueType.stringValue -> v = Integer.parseInt(value.stringValue!!)
                JsonValue.ValueType.doubleValue -> v = value.doubleValue.toInt()
                JsonValue.ValueType.longValue -> v = value.longValue.toInt()
                JsonValue.ValueType.booleanValue -> v = if (value.longValue != 0L) 1 else 0
                else -> throw IllegalStateException("Value cannot be converted to int: " + value.type!!)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /** Returns the children of this value as a newly allocated boolean array.
     * @throws IllegalStateException if this is not an array.
     */
    fun asBooleanArray(): BooleanArray {
        check(type == ValueType.array) { "Value is not an array: " + type!! }
        val array = BooleanArray(size)
        var i = 0
        var value = child
        while (value != null) {
            val v: Boolean
            when (value.type) {
                JsonValue.ValueType.stringValue -> v = java.lang.Boolean.parseBoolean(value.stringValue)
                JsonValue.ValueType.doubleValue -> v = value.doubleValue == 0.0
                JsonValue.ValueType.longValue -> v = value.longValue == 0L
                JsonValue.ValueType.booleanValue -> v = value.longValue != 0L
                else -> throw IllegalStateException("Value cannot be converted to boolean: " + value.type!!)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /** Returns the children of this value as a newly allocated byte array.
     * @throws IllegalStateException if this is not an array.
     */
    fun asByteArray(): ByteArray {
        check(type == ValueType.array) { "Value is not an array: " + type!! }
        val array = ByteArray(size)
        var i = 0
        var value = child
        while (value != null) {
            val v: Byte
            when (value.type) {
                JsonValue.ValueType.stringValue -> v = java.lang.Byte.parseByte(value.stringValue!!)
                JsonValue.ValueType.doubleValue -> v = value.doubleValue.toByte()
                JsonValue.ValueType.longValue -> v = value.longValue.toByte()
                JsonValue.ValueType.booleanValue -> v = if (value.longValue != 0L) 1.toByte() else 0
                else -> throw IllegalStateException("Value cannot be converted to byte: " + value.type!!)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /** Returns the children of this value as a newly allocated short array.
     * @throws IllegalStateException if this is not an array.
     */
    fun asShortArray(): ShortArray {
        check(type == ValueType.array) { "Value is not an array: " + type!! }
        val array = ShortArray(size)
        var i = 0
        var value = child
        while (value != null) {
            val v: Short
            when (value.type) {
                JsonValue.ValueType.stringValue -> v = java.lang.Short.parseShort(value.stringValue!!)
                JsonValue.ValueType.doubleValue -> v = value.doubleValue.toShort()
                JsonValue.ValueType.longValue -> v = value.longValue.toShort()
                JsonValue.ValueType.booleanValue -> v = if (value.longValue != 0L) 1.toShort() else 0
                else -> throw IllegalStateException("Value cannot be converted to short: " + value.type!!)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /** Returns the children of this value as a newly allocated char array.
     * @throws IllegalStateException if this is not an array.
     */
    fun asCharArray(): CharArray {
        check(type == ValueType.array) { "Value is not an array: " + type!! }
        val array = CharArray(size)
        var i = 0
        var value = child
        while (value != null) {
            val v: Char
            when (value.type) {
                JsonValue.ValueType.stringValue -> v = if (value.stringValue!!.length == 0) 0.toChar() else value.stringValue!![0]
                JsonValue.ValueType.doubleValue -> v = value.doubleValue.toChar()
                JsonValue.ValueType.longValue -> v = value.longValue.toChar()
                JsonValue.ValueType.booleanValue -> v = if (value.longValue != 0L) 1.toChar() else 0.toChar()
                else -> throw IllegalStateException("Value cannot be converted to char: " + value.type!!)
            }
            array[i] = v
            value = value.next
            i++
        }
        return array
    }

    /** Returns true if a child with the specified name exists and has a child.  */
    fun hasChild(name: String): Boolean {
        return getChild(name) != null
    }

    /** Finds the child with the specified name and returns its first child.
     * @return May be null.
     */
    fun getChild(name: String): JsonValue? {
        val child = get(name)
        return child?.child
    }

    /** Finds the child with the specified name and returns it as a string. Returns defaultValue if not found.
     * @param defaultValue May be null.
     */
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

    /** Finds the child with the specified name and returns it as a double. Returns defaultValue if not found.  */
    fun getDouble(name: String, defaultValue: Double): Double {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asDouble()
    }

    /** Finds the child with the specified name and returns it as a long. Returns defaultValue if not found.  */
    fun getLong(name: String, defaultValue: Long): Long {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asLong()
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

    /** Finds the child with the specified name and returns it as a byte. Returns defaultValue if not found.  */
    fun getByte(name: String, defaultValue: Byte): Byte {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asByte()
    }

    /** Finds the child with the specified name and returns it as a short. Returns defaultValue if not found.  */
    fun getShort(name: String, defaultValue: Short): Short {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asShort()
    }

    /** Finds the child with the specified name and returns it as a char. Returns defaultValue if not found.  */
    fun getChar(name: String, defaultValue: Char): Char {
        val child = get(name)
        return if (child == null || !child.isValue || child.isNull) defaultValue else child.asChar()
    }

    /** Finds the child with the specified name and returns it as a string.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getString(name: String): String? {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asString()
    }

    /** Finds the child with the specified name and returns it as a float.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getFloat(name: String): Float {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asFloat()
    }

    /** Finds the child with the specified name and returns it as a double.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getDouble(name: String): Double {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asDouble()
    }

    /** Finds the child with the specified name and returns it as a long.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getLong(name: String): Long {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asLong()
    }

    /** Finds the child with the specified name and returns it as an int.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getInt(name: String): Int {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asInt()
    }

    /** Finds the child with the specified name and returns it as a boolean.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getBoolean(name: String): Boolean {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asBoolean()
    }

    /** Finds the child with the specified name and returns it as a byte.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getByte(name: String): Byte {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asByte()
    }

    /** Finds the child with the specified name and returns it as a short.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getShort(name: String): Short {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asShort()
    }

    /** Finds the child with the specified name and returns it as a char.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getChar(name: String): Char {
        val child = get(name) ?: throw IllegalArgumentException("Named value not found: $name")
        return child.asChar()
    }

    /** Finds the child with the specified index and returns it as a string.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getString(index: Int): String? {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: " + name!!)
        return child.asString()
    }

    /** Finds the child with the specified index and returns it as a float.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getFloat(index: Int): Float {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: " + name!!)
        return child.asFloat()
    }

    /** Finds the child with the specified index and returns it as a double.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getDouble(index: Int): Double {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: " + name!!)
        return child.asDouble()
    }

    /** Finds the child with the specified index and returns it as a long.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getLong(index: Int): Long {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: " + name!!)
        return child.asLong()
    }

    /** Finds the child with the specified index and returns it as an int.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getInt(index: Int): Int {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: " + name!!)
        return child.asInt()
    }

    /** Finds the child with the specified index and returns it as a boolean.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getBoolean(index: Int): Boolean {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: " + name!!)
        return child.asBoolean()
    }

    /** Finds the child with the specified index and returns it as a byte.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getByte(index: Int): Byte {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: " + name!!)
        return child.asByte()
    }

    /** Finds the child with the specified index and returns it as a short.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getShort(index: Int): Short {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: " + name!!)
        return child.asShort()
    }

    /** Finds the child with the specified index and returns it as a char.
     * @throws IllegalArgumentException if the child was not found.
     */
    fun getChar(index: Int): Char {
        val child = get(index) ?: throw IllegalArgumentException("Indexed value not found: " + name!!)
        return child.asChar()
    }

    fun type(): ValueType? {
        return type
    }

    fun setType(type: ValueType?) {
        requireNotNull(type) { "type cannot be null." }
        this.type = type
    }

    /** Returns the name for this object value.
     * @return May be null.
     */
    fun name(): String? {
        return name
    }

    /** Returns the parent for this value.
     * @return May be null.
     */
    fun parent(): JsonValue? {
        return parent
    }

    /** Returns the first child for this object or array.
     * @return May be null.
     */
    fun child(): JsonValue? {
        return child
    }

    /** Sets the name of the specified value and adds it after the last child.  */
    fun addChild(name: String?, value: JsonValue) {
        requireNotNull(name) { "name cannot be null." }
        value.name = name
        addChild(value)
    }

    /** Adds the specified value after the last child.  */
    fun addChild(value: JsonValue) {
        value.parent = this
        size++
        var current = child
        if (current == null)
            child = value
        else {
            while (true) {
                if (current!!.next == null) {
                    current.next = value
                    value.prev = current
                    return
                }
                current = current.next
            }
        }
    }

    /** Returns the next sibling of this value.
     * @return May be null.
     */
    operator fun next(): JsonValue? {
        return next
    }

    /** Returns the previous sibling of this value.
     * @return May be null.
     */
    fun prev(): JsonValue? {
        return prev
    }

    /** @param value May be null.
     */
    fun set(value: String?) {
        stringValue = value
        type = if (value == null) ValueType.nullValue else ValueType.stringValue
    }

    /** @param stringValue May be null if the string representation is the string value of the double (eg, no leading zeros).
     */
    operator fun set(value: Double, stringValue: String?) {
        doubleValue = value
        longValue = value.toLong()
        this.stringValue = stringValue
        type = ValueType.doubleValue
    }

    /** @param stringValue May be null if the string representation is the string value of the long (eg, no leading zeros).
     */
    operator fun set(value: Long, stringValue: String?) {
        longValue = value
        doubleValue = value.toDouble()
        this.stringValue = stringValue
        type = ValueType.longValue
    }

    fun set(value: Boolean) {
        longValue = (if (value) 1 else 0).toLong()
        type = ValueType.booleanValue
    }

    fun toJson(outputType: OutputType): String? {
        if (isValue) return asString()
        val buffer = StringBuilder(512)
        json(this, buffer, outputType)
        return buffer.toString()
    }

    private fun json(`object`: JsonValue, buffer: StringBuilder, outputType: OutputType) {
        if (`object`.isObject) {
            if (`object`.child == null)
                buffer.append("{}")
            else {
                val start = buffer.length
                while (true) {
                    buffer.append('{')
                    val i = 0
                    run {
                        var child = `object`.child
                        while (child != null) {
                            buffer.append(outputType.quoteName(child.name!!))
                            buffer.append(':')
                            json(child, buffer, outputType)
                            if (child.next != null) buffer.append(',')
                            child = child.next
                        }
                    }
                    break
                }
                buffer.append('}')
            }
        } else if (`object`.isArray) {
            if (`object`.child == null)
                buffer.append("[]")
            else {
                val start = buffer.length
                while (true) {
                    buffer.append('[')
                    run {
                        var child = `object`.child
                        while (child != null) {
                            json(child!!, buffer, outputType)
                            if (child!!.next != null) buffer.append(',')
                            child = child!!.next
                        }
                    }
                    break
                }
                buffer.append(']')
            }
        } else if (`object`.isString) {
            buffer.append(outputType.quoteValue(`object`.asString()))
        } else if (`object`.isDouble) {
            val doubleValue = `object`.asDouble()
            val longValue = `object`.asLong()
            buffer.append(if (doubleValue == longValue.toDouble()) longValue else doubleValue)
        } else if (`object`.isLong) {
            buffer.append(`object`.asLong())
        } else if (`object`.isBoolean) {
            buffer.append(`object`.asBoolean())
        } else if (`object`.isNull) {
            buffer.append("null")
        } else
            throw SerializationException("Unknown object type: $`object`")
    }

    override fun iterator(): JsonIterator {
        return JsonIterator()
    }

    override fun toString(): String {
        return if (isValue) if (name == null) asString() ?: "" else name + ": " + asString() else (if (name == null) "" else name!! + ": ") + prettyPrint(OutputType.minimal, 0)
    }

    /** Returns a human readable string representing the path from the root of the JSON object graph to this value.  */
    fun trace(): String {
        if (parent == null) {
            if (type == ValueType.array) return "[]"
            return if (type == ValueType.`object`) "{}" else ""
        }
        var trace: String
        if (parent!!.type == ValueType.array) {
            trace = "[]"
            var i = 0
            run {
                var child = parent!!.child
                while (child != null) {
                    if (child === this) {
                        trace = "[$i]"
                        break
                    }
                    child = child.next
                    i++
                }
            }
        } else if (name!!.indexOf('.') != -1)
            trace = ".\"" + name!!.replace("\"", "\\\"") + "\""
        else
            trace = '.' + name!!
        return parent!!.trace() + trace
    }

    fun prettyPrint(outputType: OutputType, singleLineColumns: Int): String {
        val settings = PrettyPrintSettings()
        settings.outputType = outputType
        settings.singleLineColumns = singleLineColumns
        return prettyPrint(settings)
    }

    fun prettyPrint(settings: PrettyPrintSettings): String {
        val buffer = StringBuilder(512)
        prettyPrint(this, buffer, 0, settings)
        return buffer.toString()
    }

    private fun prettyPrint(`object`: JsonValue, buffer: StringBuilder, indent: Int, settings: PrettyPrintSettings) {
        val outputType = settings.outputType
        if (`object`.isObject) {
            if (`object`.child == null)
                buffer.append("{}")
            else {
                var newLines = !isFlat(`object`)
                val start = buffer.length
                outer@ while (true) {
                    buffer.append(if (newLines) "{\n" else "{ ")
                    val i = 0
                    var child = `object`.child
                    while (child != null) {
                        if (newLines) indent(indent, buffer)
                        buffer.append(outputType!!.quoteName(child.name!!))
                        buffer.append(": ")
                        prettyPrint(child, buffer, indent + 1, settings)
                        if ((!newLines || outputType != OutputType.minimal) && child.next != null) buffer.append(',')
                        buffer.append(if (newLines) '\n' else ' ')
                        if (!newLines && buffer.length - start > settings.singleLineColumns) {
                            buffer.setLength(start)
                            newLines = true
                            continue@outer
                        }
                        child = child.next
                    }
                    break
                }
                if (newLines) indent(indent - 1, buffer)
                buffer.append('}')
            }
        } else if (`object`.isArray) {
            if (`object`.child == null)
                buffer.append("[]")
            else {
                var newLines = !isFlat(`object`)
                val wrap = settings.wrapNumericArrays || !isNumeric(`object`)
                val start = buffer.length
                outer@ while (true) {
                    buffer.append(if (newLines) "[\n" else "[ ")
                    var child = `object`.child
                    while (child != null) {
                        if (newLines) indent(indent, buffer)
                        prettyPrint(child!!, buffer, indent + 1, settings)
                        if ((!newLines || outputType != OutputType.minimal) && child!!.next != null) buffer.append(',')
                        buffer.append(if (newLines) '\n' else ' ')
                        if (wrap && !newLines && buffer.length - start > settings.singleLineColumns) {
                            buffer.setLength(start)
                            newLines = true
                            continue@outer
                        }
                        child = child!!.next
                    }
                    break
                }
                if (newLines) indent(indent - 1, buffer)
                buffer.append(']')
            }
        } else if (`object`.isString) {
            buffer.append(outputType!!.quoteValue(`object`.asString()))
        } else if (`object`.isDouble) {
            val doubleValue = `object`.asDouble()
            val longValue = `object`.asLong()
            buffer.append(if (doubleValue == longValue.toDouble()) longValue else doubleValue)
        } else if (`object`.isLong) {
            buffer.append(`object`.asLong())
        } else if (`object`.isBoolean) {
            buffer.append(`object`.asBoolean())
        } else if (`object`.isNull) {
            buffer.append("null")
        } else
            throw SerializationException("Unknown object type: $`object`")
    }

    /** More efficient than [.prettyPrint] but [PrettyPrintSettings.singleLineColumns] and
     * [PrettyPrintSettings.wrapNumericArrays] are not supported.  */
    fun prettyPrint(outputType: OutputType, writer: Writer) {
        val settings = PrettyPrintSettings()
        settings.outputType = outputType
        prettyPrint(this, writer, 0, settings)
    }

    private fun prettyPrint(`object`: JsonValue, writer: Writer, indent: Int, settings: PrettyPrintSettings) {
        val outputType = settings.outputType
        if (`object`.isObject) {
            if (`object`.child == null)
                writer.append("{}")
            else {
                val newLines = !isFlat(`object`) || `object`.size > 6
                writer.append(if (newLines) "{\n" else "{ ")
                val i = 0
                run {
                    var child = `object`.child
                    while (child != null) {
                        if (newLines) indent(indent, writer)
                        writer.append(outputType!!.quoteName(child.name!!))
                        writer.append(": ")
                        prettyPrint(child, writer, indent + 1, settings)
                        if ((!newLines || outputType != OutputType.minimal) && child.next != null) writer.append(',')
                        writer.append(if (newLines) '\n' else ' ')
                        child = child.next
                    }
                }
                if (newLines) indent(indent - 1, writer)
                writer.append('}')
            }
        } else if (`object`.isArray) {
            if (`object`.child == null)
                writer.append("[]")
            else {
                val newLines = !isFlat(`object`)
                writer.append(if (newLines) "[\n" else "[ ")
                val i = 0
                run {
                    var child = `object`.child
                    while (child != null) {
                        if (newLines) indent(indent, writer)
                        prettyPrint(child!!, writer, indent + 1, settings)
                        if ((!newLines || outputType != OutputType.minimal) && child!!.next != null) writer.append(',')
                        writer.append(if (newLines) '\n' else ' ')
                        child = child!!.next
                    }
                }
                if (newLines) indent(indent - 1, writer)
                writer.append(']')
            }
        } else if (`object`.isString) {
            writer.append(outputType!!.quoteValue(`object`.asString()))
        } else if (`object`.isDouble) {
            val doubleValue = `object`.asDouble()
            val longValue = `object`.asLong()
            writer.append(java.lang.Double.toString(if (doubleValue == longValue.toDouble()) longValue.toDouble() else doubleValue))
        } else if (`object`.isLong) {
            writer.append(java.lang.Long.toString(`object`.asLong()))
        } else if (`object`.isBoolean) {
            writer.append(java.lang.Boolean.toString(`object`.asBoolean()))
        } else if (`object`.isNull) {
            writer.append("null")
        } else
            throw SerializationException("Unknown object type: $`object`")
    }

    private fun isFlat(`object`: JsonValue): Boolean {
        run {
            var child = `object`.child
            while (child != null) {
                if (child.isObject || child.isArray) return false
                child = child.next
            }
        }
        return true
    }

    private fun isNumeric(`object`: JsonValue): Boolean {
        run {
            var child = `object`.child
            while (child != null) {
                if (!child.isNumber) return false
                child = child.next
            }
        }
        return true
    }

    private fun indent(count: Int, buffer: StringBuilder) {
        for (i in 0 until count)
            buffer.append('\t')
    }

    private fun indent(count: Int, buffer: Writer) {
        for (i in 0 until count)
            buffer.append('\t')
    }

    inner class JsonIterator : MutableIterator<JsonValue>, Iterable<JsonValue> {
        internal var entry = child
        internal var current: JsonValue? = null

        override fun hasNext(): Boolean {
            return entry != null
        }

        override fun next(): JsonValue {
            current = entry
            if (current == null) throw NoSuchElementException()
            entry = current!!.next
            return current!!
        }

        override fun remove() {
            if (current!!.prev == null) {
                child = current!!.next
                if (child != null) child!!.prev = null
            } else {
                current!!.prev!!.next = current!!.next
                if (current!!.next != null) current!!.next!!.prev = current!!.prev
            }
            size--
        }

        override fun iterator(): Iterator<JsonValue> {
            return this
        }
    }

    enum class ValueType {
        `object`, array, stringValue, doubleValue, longValue, booleanValue, nullValue
    }

    class PrettyPrintSettings {
        var outputType: OutputType? = null

        /** If an object on a single line fits this many columns, it won't wrap.  */
        var singleLineColumns: Int = 0

        /** Arrays of floats won't wrap.  */
        var wrapNumericArrays: Boolean = false
    }
}
