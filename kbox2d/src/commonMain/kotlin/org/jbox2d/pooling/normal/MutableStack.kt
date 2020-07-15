/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.jbox2d.pooling.normal

import org.jbox2d.internal.*
import org.jbox2d.pooling.IDynamicStack

abstract class MutableStack<E>(private val argInitSize: Int) : IDynamicStack<E> {
    private var stack: Array<E>? = null
    private var index: Int = 0
    private var size: Int = 0

    private fun ensureInit() {
        if (stack == null) {
            extendStack(argInitSize)
        }
    }

    private fun extendStack(argSize: Int) {
        val newStack = newArray(argSize)
        if (stack != null) {
            arraycopy(stack!!, 0, newStack, 0, size)
        }
        for (i in newStack.indices) {
            newStack[i] = newInstance()
        }
        stack = newStack
        size = newStack.size
    }

    override fun pop(): E {
        ensureInit()
        if (index >= size) {
            extendStack(size * 2)
        }
        return stack!![index++]
    }

    override fun push(argObject: E) {
        ensureInit()
        assert(index > 0)
        stack!![--index] = argObject
    }

    /** Creates a new instance of the object contained by this stack.  */
    protected abstract fun newInstance(): E

    protected abstract fun newArray(size: Int): Array<E>
}
