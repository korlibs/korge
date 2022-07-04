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

import org.luaj.vm2.LuaValue

/** Variable is created lua name scopes, and is a named, lua variable that
 * either refers to a lua local, global, or upvalue storage location.
 */
class Variable {

    /** The name as it appears in lua source code  */
    @kotlin.jvm.JvmField
    val name: String

    /** The lua scope in which this variable is defined.  */
    @kotlin.jvm.JvmField
    val definingScope: NameScope?

    /** true if this variable is an upvalue  */
    @kotlin.jvm.JvmField
    var isupvalue: Boolean = false

    /** true if there are assignments made to this variable  */
    @kotlin.jvm.JvmField
    var hasassignments: Boolean = false

    /** When hasassignments == false, and the initial value is a constant, this is the initial value  */
    @kotlin.jvm.JvmField
    var initialValue: LuaValue? = null
    val isLocal: Boolean
        get() = this.definingScope != null
    val isConstant: Boolean
        get() = !hasassignments && initialValue != null

    /** Global is named variable not associated with a defining scope  */
    constructor(name: String) {
        this.name = name
        this.definingScope = null
    }

    constructor(name: String, definingScope: NameScope) {
        /** Local variable is defined in a particular scope.   */
        this.name = name
        this.definingScope = definingScope
    }
}
