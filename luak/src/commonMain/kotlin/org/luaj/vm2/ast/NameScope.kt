/*******************************************************************************
 * Copyright (c) 2010 Luaj.org. All rights reserved.
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
package org.luaj.vm2.ast

class NameScope {

    @kotlin.jvm.JvmField
    val namedVariables: MutableMap<String, Variable> = HashMap()

    @kotlin.jvm.JvmField
    val outerScope: NameScope?

    @kotlin.jvm.JvmField
    var functionNestingCount: Int = 0

    /** Construct default names scope  */
    constructor() {
        this.outerScope = null
        this.functionNestingCount = 0
    }

    /** Construct name scope within another scope */
    constructor(outerScope: NameScope?) {
        this.outerScope = outerScope
        this.functionNestingCount = outerScope?.functionNestingCount ?: 0
    }

    /** Look up a name.  If it is a global name, then throw IllegalArgumentException.  */

    fun find(name: String): Variable {
        validateIsNotKeyword(name)
        var n: NameScope? = this
        while (n != null) {
            if (n.namedVariables.containsKey(name))
                return n.namedVariables[name] as Variable
            n = n.outerScope
        }
        val value = Variable(name)
        this.namedVariables[name] = value
        return value
    }

    /** Define a name in this scope.  If it is a global name, then throw IllegalArgumentException.  */

    fun define(name: String): Variable {
        validateIsNotKeyword(name)
        val value = Variable(name, this)
        this.namedVariables[name] = value
        return value
    }

    private fun validateIsNotKeyword(name: String) {
        if (LUA_KEYWORDS.contains(name))
            throw IllegalArgumentException("name is a keyword: '$name'")
    }

    companion object {

        private val LUA_KEYWORDS = setOf(
            "and",
            "break",
            "do",
            "else",
            "elseif",
            "end",
            "false",
            "for",
            "function",
            "if",
            "in",
            "local",
            "nil",
            "not",
            "or",
            "repeat",
            "return",
            "then",
            "true",
            "until",
            "while"
        )

    }
}
