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

abstract class Visitor {
    open fun visit(chunk: Chunk) {
        chunk.block.accept(this)
    }

    open fun visit(block: Block) {
        visit(block.scope)
        if (block.stats != null) {
            var i = 0
            val n = block.stats.size
            while (i < n) {
                block.stats[i].accept(this)
                i++
            }
        }
    }

    open fun visit(stat: Stat.Assign) {
        visitVars(stat.vars)
        visitExps(stat.exps)
    }

    open fun visit(breakstat: Stat.Break) {}
    open fun visit(stat: Stat.FuncCallStat) {
        stat.funccall.accept(this)
    }

    open fun visit(stat: Stat.FuncDef) {
        stat.body.accept(this)
    }

    open fun visit(stat: Stat.GenericFor) {
        visit(stat.scope)
        visitNames(stat.names)
        visitExps(stat.exps)
        stat.block.accept(this)
    }

    open fun visit(stat: Stat.IfThenElse) {
        stat.ifexp.accept(this)
        stat.ifblock.accept(this)
        if (stat.elseifblocks != null) {
            var i = 0
            val n = stat.elseifblocks.size
            while (i < n) {
                (stat.elseifexps!![i] as Exp).accept(this)
                (stat.elseifblocks[i] as Block).accept(this)
                i++
            }
        }
        if (stat.elseblock != null)
            visit(stat.elseblock)
    }

    open fun visit(stat: Stat.LocalAssign) {
        visitNames(stat.names)
        visitExps(stat.values)
    }

    open fun visit(stat: Stat.LocalFuncDef) {
        visit(stat.name)
        stat.body.accept(this)
    }

    open fun visit(stat: Stat.NumericFor) {
        visit(stat.scope)
        visit(stat.name)
        stat.initial.accept(this)
        stat.limit.accept(this)
        if (stat.step != null)
            stat.step.accept(this)
        stat.block.accept(this)
    }

    open fun visit(stat: Stat.RepeatUntil) {
        stat.block.accept(this)
        stat.exp.accept(this)
    }

    open fun visit(stat: Stat.Return) {
        visitExps(stat.values)
    }

    open fun visit(stat: Stat.WhileDo) {
        stat.exp.accept(this)
        stat.block.accept(this)
    }

    open fun visit(body: FuncBody) {
        visit(body.scope)
        body.parlist.accept(this)
        body.block.accept(this)
    }

    open fun visit(args: FuncArgs) {
        visitExps(args.exps)
    }

    open fun visit(field: TableField) {
        if (field.name != null);
        visit(field.name)
        if (field.index != null)
            field.index.accept(this)
        field.rhs!!.accept(this)
    }

    open fun visit(exp: Exp.AnonFuncDef) {
        exp.body.accept(this)
    }

    open fun visit(exp: Exp.BinopExp) {
        exp.lhs.accept(this)
        exp.rhs.accept(this)
    }

    open fun visit(exp: Exp.Constant) {}
    open fun visit(exp: Exp.FieldExp) {
        exp.lhs.accept(this)
        visit(exp.name)
    }

    open fun visit(exp: Exp.FuncCall) {
        exp.lhs.accept(this)
        exp.args.accept(this)
    }

    open fun visit(exp: Exp.IndexExp) {
        exp.lhs.accept(this)
        exp.exp.accept(this)
    }

    open fun visit(exp: Exp.MethodCall) {
        exp.lhs.accept(this)
        visit(exp.name)
        exp.args.accept(this)
    }

    open fun visit(exp: Exp.NameExp) {
        visit(exp.name)
    }

    open fun visit(exp: Exp.ParensExp) {
        exp.exp.accept(this)
    }

    open fun visit(exp: Exp.UnopExp) {
        exp.rhs.accept(this)
    }

    open fun visit(exp: Exp.VarargsExp) {}
    open fun visit(pars: ParList) {
        visitNames(pars.names)
    }

    open fun visit(table: TableConstructor) {
        if (table.fields != null) {
            var i = 0
            val n = table.fields!!.size
            while (i < n) {
                (table.fields!![i] as TableField).accept(this)
                i++
            }
        }
    }

    open fun visitVars(vars: List<VarExp>?) {
        if (vars != null) {
            var i = 0
            val n = vars.size
            while (i < n) {
                vars[i].accept(this)
                i++
            }
        }
    }

    open fun visitExps(exps: List<Exp>?) {
        if (exps != null) {
            var i = 0
            val n = exps.size
            while (i < n) {
                exps[i].accept(this)
                i++
            }
        }
    }

    open fun visitNames(names: List<Name>?) {
        if (names != null) {
            var i = 0
            val n = names.size
            while (i < n) {
                visit(names[i])
                i++
            }
        }
    }

    open fun visit(name: Name) {}
    open fun visit(name: String?) {}
    open fun visit(scope: NameScope?) {}
    open fun visit(gotostat: Stat.Goto) {}
    open fun visit(label: Stat.Label) {}
}
