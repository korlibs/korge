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

import org.luaj.vm2.ast.Exp.VarExp

abstract class Stat : SyntaxElement() {
    abstract fun accept(visitor: Visitor)

    class Goto(val name: String) : Stat() {
        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class Label(val name: String) : Stat() {
        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class Assign(val vars: List<VarExp>, val exps: List<Exp>) : Stat() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }

    }

    class WhileDo(val exp: Exp, val block: Block) : Stat() {
        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class RepeatUntil(val block: Block, val exp: Exp) : Stat() {
        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class Break : Stat() {
        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class Return(val values: List<Exp>?) : Stat() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }

        fun nreturns(): Int {
            var n = values?.size ?: 0
            if (n > 0 && values!![n - 1].isvarargexp())
                n = -1
            return n
        }
    }

    class FuncCallStat(val funccall: Exp.FuncCall) : Stat() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class LocalFuncDef(name: String, val body: FuncBody) : Stat() {
        val name: Name

        init {
            this.name = Name(name)
        }

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class FuncDef(val name: FuncName, val body: FuncBody) : Stat() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class GenericFor(var names: List<Name>, var exps: List<Exp>, var block: Block) : Stat() {
        var scope: NameScope? = null

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class NumericFor(name: String, val initial: Exp, val limit: Exp, val step: Exp?, val block: Block) : Stat() {
        val name: Name = Name(name)
        var scope: NameScope? = null

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class LocalAssign(val names: List<Name>, val values: List<Exp>?) : Stat() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class IfThenElse(
        val ifexp: Exp, val ifblock: Block, val elseifexps: List<Exp>?,
        val elseifblocks: List<Block>?, val elseblock: Block?
    ) : Stat() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    companion object {


        fun block(block: Block): Stat {
            return block
        }


        fun whiledo(exp: Exp, block: Block): Stat {
            return WhileDo(exp, block)
        }


        fun repeatuntil(block: Block, exp: Exp): Stat {
            return RepeatUntil(block, exp)
        }


        fun breakstat(): Stat {
            return Break()
        }


        fun returnstat(exps: List<Exp>?): Stat {
            return Return(exps)
        }


        fun assignment(vars: List<VarExp>, exps: List<Exp>): Stat {
            return Assign(vars, exps)
        }


        fun functioncall(funccall: Exp.FuncCall): Stat {
            return FuncCallStat(funccall)
        }


        fun localfunctiondef(name: String, funcbody: FuncBody): Stat {
            return LocalFuncDef(name, funcbody)
        }


        fun fornumeric(name: String, initial: Exp, limit: Exp, step: Exp?, block: Block): Stat {
            return NumericFor(name, initial, limit, step, block)
        }


        fun functiondef(funcname: FuncName, funcbody: FuncBody): Stat {
            return FuncDef(funcname, funcbody)
        }


        fun forgeneric(names: List<Name>, exps: List<Exp>, block: Block): Stat {
            return GenericFor(names, exps, block)
        }


        fun localassignment(names: List<Name>, values: List<Exp>?): Stat {
            return LocalAssign(names, values)
        }


        fun ifthenelse(
            ifexp: Exp,
            ifblock: Block,
            elseifexps: List<Exp>?,
            elseifblocks: List<Block>?,
            elseblock: Block?
        ): Stat {
            return IfThenElse(ifexp, ifblock, elseifexps, elseifblocks, elseblock)
        }


        fun gotostat(name: String): Stat {
            return Goto(name)
        }


        fun labelstat(name: String): Stat {
            return Label(name)
        }
    }
}
