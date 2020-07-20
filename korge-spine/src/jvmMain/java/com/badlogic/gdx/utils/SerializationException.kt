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

/** Indicates an error during serialization due to misconfiguration or during deserialization due to invalid input data.
 * @author Nathan Sweet
 */
class SerializationException : RuntimeException {
    private var trace: StringBuilder? = null

    constructor() : super() {}

    constructor(message: String, cause: Throwable?) : super(message, cause) {}

    constructor(message: String) : super(message) {}

    constructor(cause: Throwable?) : super("", cause) {}

    /** Returns true if any of the exceptions that caused this exception are of the specified type.  */
    fun causedBy(type: Class<*>): Boolean {
        return causedBy(this, type)
    }

    private fun causedBy(ex: Throwable, type: Class<*>): Boolean {
        val cause = ex.cause
        if (cause == null || cause === ex) return false
        return if (type.isAssignableFrom(cause.javaClass)) true else causedBy(cause, type)
    }

    override val message: String?
        get() {
            if (trace == null) return super.message
            val sb = StringBuilder(512)
            sb.append(super.message)
            if (sb.length > 0) sb.append('\n')
            sb.append("Serialization trace:")
            sb.append(trace)
            return sb.toString()
        }

    /** Adds information to the exception message about where in the the object graph serialization failure occurred. Serializers
     * can catch [SerializationException], add trace information, and rethrow the exception.  */
    fun addTrace(info: String?) {
        requireNotNull(info) { "info cannot be null." }
        if (trace == null) trace = StringBuilder(512)
        trace!!.append('\n')
        trace!!.append(info)
    }
}
