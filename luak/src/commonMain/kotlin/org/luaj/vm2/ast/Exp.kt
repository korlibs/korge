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

import org.luaj.vm2.*

abstract class Exp : SyntaxElement() {
    abstract fun accept(visitor: Visitor)

    open fun isvarexp(): Boolean {
        return false
    }

    open fun isfunccall(): Boolean {
        return false
    }

    open fun isvarargexp(): Boolean {
        return false
    }

    abstract class PrimaryExp : Exp() {
        override fun isvarexp(): Boolean {
            return false
        }

        override fun isfunccall(): Boolean {
            return false
        }
    }

    abstract class VarExp : PrimaryExp() {
        override fun isvarexp(): Boolean {
            return true
        }

        open fun markHasAssignment() {}
    }

    class NameExp(name: String) : VarExp() {
        val name: Name

        init {
            this.name = Name(name)
        }

        override fun markHasAssignment() {
            name.variable!!.hasassignments = true
        }

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class ParensExp(val exp: Exp) : PrimaryExp() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class FieldExp(val lhs: PrimaryExp, name: String) : VarExp() {
        val name: Name

        init {
            this.name = Name(name)
        }

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class IndexExp(val lhs: PrimaryExp, val exp: Exp) : VarExp() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    open class FuncCall(val lhs: PrimaryExp, val args: FuncArgs) : PrimaryExp() {

        override fun isfunccall(): Boolean {
            return true
        }

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }

        override fun isvarargexp(): Boolean {
            return true
        }
    }

    class MethodCall(lhs: PrimaryExp, name: String, args: FuncArgs) : FuncCall(lhs, args) {
        val name: String = name

        override fun isfunccall(): Boolean {
            return true
        }

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class Constant(val value: LuaValue) : Exp() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class VarargsExp : Exp() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }

        override fun isvarargexp(): Boolean {
            return true
        }
    }

    class UnopExp(val op: Int, val rhs: Exp) : Exp() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class BinopExp(val lhs: Exp, val op: Int, val rhs: Exp) : Exp() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    class AnonFuncDef(val body: FuncBody) : Exp() {

        override fun accept(visitor: Visitor) {
            visitor.visit(this)
        }
    }

    companion object {


        fun constant(value: LuaValue): Exp {
            return Constant(value)
        }


        fun numberconstant(token: String): Exp {
            return Constant(LuaValue.valueOf(token).tonumber())
        }


        fun varargs(): Exp {
            return VarargsExp()
        }


        fun tableconstructor(tc: TableConstructor): Exp {
            return tc
        }


        fun unaryexp(op: Int, rhs: Exp): Exp {
            if (rhs is BinopExp) {
                if (precedence(op) > precedence(rhs.op))
                    return binaryexp(unaryexp(op, rhs.lhs), rhs.op, rhs.rhs)
            }
            return UnopExp(op, rhs)
        }


        fun binaryexp(lhs: Exp, op: Int, rhs: Exp): Exp {
            if (lhs is UnopExp) {
                if (precedence(op) > precedence(lhs.op))
                    return unaryexp(lhs.op, binaryexp(lhs.rhs, op, rhs))
            }
            // TODO: cumulate string concatenations together
            // TODO: constant folding
            if (lhs is BinopExp) {
                if (precedence(op) > precedence(lhs.op) || precedence(op) == precedence(lhs.op) && isrightassoc(op))
                    return binaryexp(lhs.lhs, lhs.op, binaryexp(lhs.rhs, op, rhs))
            }
            if (rhs is BinopExp) {
                if (precedence(op) > precedence(rhs.op) || precedence(op) == precedence(rhs.op) && !isrightassoc(op))
                    return binaryexp(binaryexp(lhs, op, rhs.lhs), rhs.op, rhs.rhs)
            }
            return BinopExp(lhs, op, rhs)
        }


        internal fun isrightassoc(op: Int): Boolean {
            when (op) {
                Lua.OP_CONCAT, Lua.OP_POW -> return true
                else -> return false
            }
        }


        internal fun precedence(op: Int): Int {
            when (op) {
                Lua.OP_OR -> return 0
                Lua.OP_AND -> return 1
                Lua.OP_LT, Lua.OP_GT, Lua.OP_LE, Lua.OP_GE, Lua.OP_NEQ, Lua.OP_EQ -> return 2
                Lua.OP_CONCAT -> return 3
                Lua.OP_ADD, Lua.OP_SUB -> return 4
                Lua.OP_MUL, Lua.OP_DIV, Lua.OP_MOD -> return 5
                Lua.OP_NOT, Lua.OP_UNM, Lua.OP_LEN -> return 6
                Lua.OP_POW -> return 7
                else -> throw IllegalStateException("precedence of bad op $op")
            }
        }


        fun anonymousfunction(funcbody: FuncBody): Exp {
            return AnonFuncDef(funcbody)
        }

        /** foo  */

        fun nameprefix(name: String): NameExp {
            return NameExp(name)
        }

        /** ( foo.bar )  */

        fun parensprefix(exp: Exp): ParensExp {
            return ParensExp(exp)
        }

        /** foo[exp]  */

        fun indexop(lhs: PrimaryExp, exp: Exp): IndexExp {
            return IndexExp(lhs, exp)
        }

        /** foo.bar  */

        fun fieldop(lhs: PrimaryExp, name: String): FieldExp {
            return FieldExp(lhs, name)
        }

        /** foo(2,3)  */

        fun functionop(lhs: PrimaryExp, args: FuncArgs): FuncCall {
            return FuncCall(lhs, args)
        }

        /** foo:bar(4,5)  */

        fun methodop(lhs: PrimaryExp, name: String, args: FuncArgs): MethodCall {
            return MethodCall(lhs, name, args)
        }
    }

}
