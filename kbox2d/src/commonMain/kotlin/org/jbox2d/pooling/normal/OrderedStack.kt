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
/**
 * Created at 12:52:04 AM Jan 20, 2011
 */
package org.jbox2d.pooling.normal

import org.jbox2d.internal.*

/**
 * @author Daniel Murphy
 */
abstract class OrderedStack<E>(private val size: Int, argContainerSize: Int) {
    private val pool: Array<Any> by lazy { Array(size) { newInstance() as Any } }
    private var index: Int = 0
    private val container: Array<Any> = Array(argContainerSize) { Unit }

    fun pop(): E {
        assert(index < size) { "End of stack reached, there is probably a leak somewhere" }
        return pool[index++] as E
    }

    fun pop(argNum: Int): Array<E> {
        assert(index + argNum < size) { "End of stack reached, there is probably a leak somewhere" }
        assert(argNum <= container.size) { "Container array is too small" }
        arraycopy(pool, index, container, 0, argNum)
        index += argNum
        return container as Array<E>
    }

    fun push(argNum: Int) {
        index -= argNum
        assert(index >= 0) { "Beginning of stack reached, push/pops are unmatched" }
    }

    /** Creates a new instance of the object contained by this stack.  */
    protected abstract fun newInstance(): E
}
