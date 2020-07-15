package org.luaj.vm2.ast

import org.luaj.vm2.LuaValue
import org.luaj.vm2.ast.Exp.Constant
import org.luaj.vm2.ast.Exp.NameExp
import org.luaj.vm2.ast.Exp.VarExp
import org.luaj.vm2.ast.Stat.Assign
import org.luaj.vm2.ast.Stat.FuncDef
import org.luaj.vm2.ast.Stat.GenericFor
import org.luaj.vm2.ast.Stat.LocalAssign
import org.luaj.vm2.ast.Stat.LocalFuncDef
import org.luaj.vm2.ast.Stat.NumericFor

/**
 * Visitor that resolves names to scopes.
 * Each Name is resolved to a NamedVarible, possibly in a NameScope
 * if it is a local, or in no named scope if it is a global.
 */
class NameResolver : Visitor() {

    private var scope: NameScope? = null

    private fun pushScope() {
        scope = NameScope(scope)
    }

    private fun popScope() {
        scope = scope!!.outerScope
    }

    override fun visit(scope: NameScope?) {}

    override fun visit(block: Block) {
        pushScope()
        block.scope = scope
        super.visit(block)
        popScope()
    }

    override fun visit(body: FuncBody) {
        pushScope()
        scope!!.functionNestingCount++
        body.scope = scope
        super.visit(body)
        popScope()
    }

    override fun visit(stat: LocalFuncDef) {
        defineLocalVar(stat.name)
        super.visit(stat)
    }

    override fun visit(stat: NumericFor) {
        pushScope()
        stat.scope = scope
        defineLocalVar(stat.name)
        super.visit(stat)
        popScope()
    }

    override fun visit(stat: GenericFor) {
        pushScope()
        stat.scope = scope
        defineLocalVars(stat.names)
        super.visit(stat)
        popScope()
    }

    override fun visit(exp: NameExp) {
        exp.name.variable = resolveNameReference(exp.name)
        super.visit(exp)
    }

    override fun visit(stat: FuncDef) {
        stat.name.name.variable = resolveNameReference(stat.name.name)
        stat.name.name.variable!!.hasassignments = true
        super.visit(stat)
    }

    override fun visit(stat: Assign) {
        super.visit(stat)
        var i = 0
        val n = stat.vars.size
        while (i < n) {
            val v = stat.vars[i] as VarExp
            v.markHasAssignment()
            i++
        }
    }

    override fun visit(stat: LocalAssign) {
        visitExps(stat.values)
        defineLocalVars(stat.names)
        val n = stat.names.size
        val m = if (stat.values != null) stat.values.size else 0
        val isvarlist = m > 0 && m < n && (stat.values!![m - 1] as Exp).isvarargexp()
        run {
            var i = 0
            while (i < n && i < if (isvarlist) m - 1 else m) {
                if (stat.values!![i] is Constant)
                    (stat.names[i] as Name).variable!!.initialValue = (stat.values!![i] as Constant).value
                i++
            }
        }
        if (!isvarlist)
            for (i in m until n)
                (stat.names[i] as Name).variable!!.initialValue = LuaValue.NIL
    }

    override fun visit(pars: ParList) {
        if (pars.names != null)
            defineLocalVars(pars.names)
        if (pars.isvararg)
            scope!!.define("arg")
        super.visit(pars)
    }

    protected fun defineLocalVars(names: List<Name>) {
        var i = 0
        val n = names.size
        while (i < n) {
            defineLocalVar(names[i])
            i++
        }
    }

    protected fun defineLocalVar(name: Name) {
        name.variable = scope!!.define(name.name)
    }

    protected fun resolveNameReference(name: Name): Variable {
        val v = scope!!.find(name.name)
        if (v.isLocal && scope!!.functionNestingCount != v.definingScope!!.functionNestingCount)
            v.isupvalue = true
        return v
    }
}
