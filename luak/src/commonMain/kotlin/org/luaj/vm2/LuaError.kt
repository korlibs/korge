/*******************************************************************************
 * Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.luaj.vm2


/**
 * RuntimeException that is thrown and caught in response to a lua error.
 *
 *
 * [LuaError] is used wherever a lua call to `error()`
 * would be used within a script.
 *
 *
 * Since it is an unchecked exception inheriting from [RuntimeException],
 * Java method signatures do notdeclare this exception, althoug it can
 * be thrown on almost any luaj Java operation.
 * This is analagous to the fact that any lua script can throw a lua error at any time.
 *
 *
 * The LuaError may be constructed with a message object, in which case the message
 * is the string representation of that object.  getMessageObject will get the object
 * supplied at construct time, or a LuaString containing the message of an object
 * was not supplied.
 */
class LuaError : RuntimeException {

    @kotlin.jvm.JvmField
    var level: Int = 0

    @kotlin.jvm.JvmField
    var fileline: String? = null

    @kotlin.jvm.JvmField
    var traceback: String? = null

    @kotlin.jvm.JvmField
    var luaCause: Throwable? = null

    private var `object`: LuaValue? = null

    /** Get the LuaValue that was provided in the constructor, or
     * a LuaString containing the message if it was a string error argument.
     * @return LuaValue which was used in the constructor, or a LuaString
     * containing the message.
     */
    val messageObject: LuaValue?
        get() {
            if (`object` != null) return `object`
            val m = message
            return if (m != null) LuaValue.valueOf(m) else null
        }

    /** Get the string message if it was supplied, or a string
     * representation of the message object if that was supplied.
     */
    fun getLuaMessage(): String? {
        if (traceback != null)
            return traceback
        val m = super.message ?: return null
        return if (fileline != null) "$fileline $m" else m
    }

    /** Construct LuaError when a program exception occurs.
     *
     *
     * All errors generated from lua code should throw LuaError(String) instead.
     * @param cause the Throwable that caused the error, if known.
     */
    constructor(cause: Throwable) : super("vm error: $cause") {
        this.luaCause = cause
        this.level = 1
    }

    /**
     * Construct a LuaError with a specific message.
     *
     * @param message message to supply
     */
    constructor(message: String) : super(message) {
        this.level = 1
    }

    /**
     * Construct a LuaError with a message, and level to draw line number information from.
     * @param message message to supply
     * @param level where to supply line info from in call stack
     */
    constructor(message: String, level: Int) : super(message) {
        this.level = level
    }

    /**
     * Construct a LuaError with a LuaValue as the message object,
     * and level to draw line number information from.
     * @param message_object message string or object to supply
     */
    constructor(message_object: LuaValue) : super(message_object.tojstring()) {
        this.`object` = message_object
        this.level = 1
    }


    /**
     * Get the cause, if any.
     */
    fun getLuaCause(): Throwable? {
        return luaCause
    }

}
