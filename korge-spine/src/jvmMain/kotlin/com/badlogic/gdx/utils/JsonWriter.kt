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
import java.math.BigDecimal
import java.math.BigInteger
import java.util.regex.Pattern

/** Builder style API for emitting JSON.
 * @author Nathan Sweet
 */
class JsonWriter(val writer: Writer) : Writer() {
    private val stack = JArray<JsonObject>()
    private var current: JsonObject? = null
    private var named: Boolean = false
    private var outputType = OutputType.json
    private var quoteLongValues = false

    /** Sets the type of JSON output. Default is [OutputType.minimal].  */
    fun setOutputType(outputType: OutputType) {
        this.outputType = outputType
    }

    /** When true, quotes long, double, BigInteger, BigDecimal types to prevent truncation in languages like JavaScript and PHP.
     * This is not necessary when using libgdx, which handles these types without truncation. Default is false.  */
    fun setQuoteLongValues(quoteLongValues: Boolean) {
        this.quoteLongValues = quoteLongValues
    }

    @Throws(IOException::class)
    fun name(name: String): JsonWriter {
        check(!(current == null || current!!.array)) { "Current item must be an object." }
        if (!current!!.needsComma)
            current!!.needsComma = true
        else
            writer.write(','.toInt())
        writer.write(outputType.quoteName(name))
        writer.write(':'.toInt())
        named = true
        return this
    }

    @Throws(IOException::class)
    fun `object`(): JsonWriter {
        requireCommaOrName()
        current = JsonObject(false)
        stack.add(current!!)
        return this
    }

    @Throws(IOException::class)
    fun array(): JsonWriter {
        requireCommaOrName()
        current = JsonObject(true)
        stack.add(current!!)
        return this
    }

    @Throws(IOException::class)
    fun value(value: Any): JsonWriter {
        var value = value
        if (quoteLongValues && (value is Long || value is Double || value is BigDecimal || value is BigInteger)) {
            value = value.toString()
        } else if (value is Number) {
            val number = value
            val longValue = number.toLong()
            if (number.toDouble() == longValue.toDouble()) value = longValue
        }
        requireCommaOrName()
        writer.write(outputType.quoteValue(value))
        return this
    }

    /** Writes the specified JSON value, without quoting or escaping.  */
    @Throws(IOException::class)
    fun json(json: String): JsonWriter {
        requireCommaOrName()
        writer.write(json)
        return this
    }

    @Throws(IOException::class)
    private fun requireCommaOrName() {
        if (current == null) return
        if (current!!.array) {
            if (!current!!.needsComma)
                current!!.needsComma = true
            else
                writer.write(','.toInt())
        } else {
            check(named) { "Name must be set." }
            named = false
        }
    }

    @Throws(IOException::class)
    fun `object`(name: String): JsonWriter {
        return name(name).`object`()
    }

    @Throws(IOException::class)
    fun array(name: String): JsonWriter {
        return name(name).array()
    }

    @Throws(IOException::class)
    operator fun set(name: String, value: Any): JsonWriter {
        return name(name).value(value)
    }

    /** Writes the specified JSON value, without quoting or escaping.  */
    @Throws(IOException::class)
    fun json(name: String, json: String): JsonWriter {
        return name(name).json(json)
    }

    @Throws(IOException::class)
    fun pop(): JsonWriter {
        check(!named) { "Expected an object, array, or value since a name was set." }
        stack.pop().close()
        current = if (stack.size == 0) null else stack.peek()
        return this
    }

    @Throws(IOException::class)
    override fun write(cbuf: CharArray, off: Int, len: Int) {
        writer.write(cbuf, off, len)
    }

    @Throws(IOException::class)
    override fun flush() {
        writer.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        while (stack.size > 0)
            pop()
        writer.close()
    }

    private inner class JsonObject @Throws(IOException::class)
    internal constructor(internal val array: Boolean) {
        internal var needsComma: Boolean = false

        init {
            writer.write((if (array) '[' else '{').toInt())
        }

        @Throws(IOException::class)
        internal fun close() {
            writer.write((if (array) ']' else '}').toInt())
        }
    }

    enum class OutputType {
        json, javascript, minimal;


        fun quoteValue(value: Any?): String {
            if (value == null) return "null"
            val string = value.toString()
            if (value is Number || value is Boolean) return string
            var buffer = string
            buffer = buffer.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t")
            if (this == OutputType.minimal && string != "true" && string != "false" && string != "null"
                    && !string.contains("//") && !string.contains("/*")) {
                val length = buffer.length
                if (length > 0 && buffer[length - 1] != ' ' && minimalValuePattern.matcher(buffer).matches())
                    return buffer
            }
            return '"'.toString() + buffer.replace("\"", "\\\"") + '"'.toString()
        }

        fun quoteName(value: String): String {
            var buffer = value
            buffer = buffer.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t")
            when (this) {
                minimal -> {
                    if (!value.contains("//") && !value.contains("/*") && minimalNamePattern.matcher(buffer).matches())
                        return buffer
                    if (javascriptPattern.matcher(buffer).matches()) return buffer
                }
                javascript -> if (javascriptPattern.matcher(buffer).matches()) return buffer
            }
            return '"'.toString() + buffer.replace("\"", "\\\"") + '"'.toString()
        }

        companion object {

            private val javascriptPattern = Pattern.compile("^[a-zA-Z_$][a-zA-Z_$0-9]*$")
            private val minimalNamePattern = Pattern.compile("^[^\":,}/ ][^:]*$")
            private val minimalValuePattern = Pattern.compile("^[^\":,{\\[\\]/ ][^}\\],]*$")
        }
    }
}
