/*******************************************************************************
 * Copyright (c) 2009 Luaj.org. All rights reserved.
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
package org.luaj.vm2.compiler

import org.luaj.vm2.LocVars
import org.luaj.vm2.Lua
import org.luaj.vm2.LuaDouble
import org.luaj.vm2.LuaInteger
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Prototype
import org.luaj.vm2.Upvaldesc
import org.luaj.vm2.compiler.LexState.ConsControl
import org.luaj.vm2.compiler.LexState.expdesc


class FuncState constructor() : Constants() {

    @kotlin.jvm.JvmField var f: Prototype? = null  /* current function header */
    @kotlin.jvm.JvmField var h: HashMap<LuaValue, Int>? = null  /* table to find (and reuse) elements in `k' */
    @kotlin.jvm.JvmField var prev: FuncState? = null  /* enclosing function */
    @kotlin.jvm.JvmField var ls: LexState? = null  /* lexical state */
    @kotlin.jvm.JvmField var L: LuaC.CompileState? = null  /* compiler being invoked */
    @kotlin.jvm.JvmField var bl: BlockCnt? = null  /* chain of current blocks */
    @kotlin.jvm.JvmField var pc: Int = 0  /* next position to code (equivalent to `ncode') */
    @kotlin.jvm.JvmField var lasttarget: Int = 0   /* `pc' of last `jump target' */
    @kotlin.jvm.JvmField var jpc: IntPtr? = null  /* list of pending jumps to `pc' */
    @kotlin.jvm.JvmField var nk: Int = 0  /* number of elements in `k' */
    @kotlin.jvm.JvmField var np: Int = 0  /* number of elements in `p' */
    @kotlin.jvm.JvmField var firstlocal: Int = 0  /* index of first local var (in Dyndata array) */
    @kotlin.jvm.JvmField var nlocvars: Short = 0  /* number of elements in `locvars' */
    @kotlin.jvm.JvmField var nactvar: Short = 0  /* number of active local variables */
    @kotlin.jvm.JvmField var nups: Short = 0  /* number of upvalues */
    @kotlin.jvm.JvmField var freereg: Short = 0  /* first free register */

    class BlockCnt {
        @kotlin.jvm.JvmField var previous: BlockCnt? = null /* chain */
        @kotlin.jvm.JvmField var firstlabel: Short = 0 /* index of first label in this block */
        @kotlin.jvm.JvmField var firstgoto: Short = 0 /* index of first pending goto in this block */
        @kotlin.jvm.JvmField var nactvar: Short = 0 /* # active locals outside the breakable structure */
        @kotlin.jvm.JvmField var upval: Boolean = false /* true if some variable in the block is an upvalue */
        @kotlin.jvm.JvmField var isloop: Boolean = false /* true if `block' is a loop */
    }


    // =============================================================
    // from lcode.h
    // =============================================================

    fun getcodePtr(e: expdesc): InstructionPtr {
        return InstructionPtr(f!!.code, e.u.info)
    }

    fun getcode(e: expdesc): Int {
        return f!!.code[e.u.info]
    }

    fun codeAsBx(o: Int, A: Int, sBx: Int): Int {
        return codeABx(o, A, sBx + Lua.MAXARG_sBx)
    }

    fun setmultret(e: expdesc) {
        setreturns(e, Lua.LUA_MULTRET)
    }


    // =============================================================
    // from lparser.c
    // =============================================================

    /* check for repeated labels on the same block */
    fun checkrepeated(ll: Array<LexState.Labeldesc>?, ll_n: Int, label: LuaString) {
        var i: Int
        i = bl!!.firstlabel.toInt()
        while (i < ll_n) {
            if (label.eq_b(ll!![i].name!!)) {
                val msg = ls!!.L.pushfstring(
                    "label '" + label + " already defined on line " + ll[i].line
                )
                ls!!.semerror(msg)
            }
            i++
        }
    }


    fun checklimit(v: Int, l: Int, msg: String) {
        if (v > l)
            errorlimit(l, msg)
    }

    fun errorlimit(limit: Int, what: String) {
        // TODO: report message logic.
        val msg = if (f!!.linedefined == 0)
            L!!.pushfstring("main function has more than $limit $what")
        else
            L!!.pushfstring("function at line " + f!!.linedefined + " has more than " + limit + " " + what)
        ls!!.lexerror(msg, 0)
    }

    fun getlocvar(i: Int): LocVars {
        val idx = ls!!.dyd!!.actvar!![firstlocal + i].idx.toInt()
        _assert(idx < nlocvars)
        return f!!.locvars[idx]
    }

    fun removevars(tolevel: Int) {
        ls!!.dyd.n_actvar -= nactvar - tolevel
        while (nactvar > tolevel)
            getlocvar((--nactvar).toInt()).endpc = pc
    }


    fun searchupvalue(name: LuaString): Int {
        var i: Int
        val up = f!!.upvalues
        i = 0
        while (i < nups) {
            if (up[i].name!!.eq_b(name))
                return i
            i++
        }
        return -1  /* not found */
    }

    fun newupvalue(name: LuaString, v: expdesc): Int {
        checklimit(nups + 1, Constants.LUAI_MAXUPVAL, "upvalues")
        if (f!!.upvalues == null || nups + 1 > f!!.upvalues.size)
            f!!.upvalues = Constants.realloc(f!!.upvalues, if (nups > 0) nups * 2 else 1)
        f!!.upvalues[nups.toInt()] = Upvaldesc(name, v.k == LexState.VLOCAL, v.u.info)
        return nups++.toInt()
    }

    fun searchvar(n: LuaString): Int {
        var i: Int
        i = nactvar - 1
        while (i >= 0) {
            if (n.eq_b(getlocvar(i).varname))
                return i
            i--
        }
        return -1 /* not found */
    }

    fun markupval(level: Int) {
        var bl = this.bl
        while (bl!!.nactvar > level)
            bl = bl.previous
        bl.upval = true
    }

    /*
	** "export" pending gotos to outer level, to check them against
	** outer labels; if the block being exited has upvalues, and
	** the goto exits the scope of any variable (which can be the
	** upvalue), close those variables being exited.
	*/
    fun movegotosout(bl: BlockCnt) {
        var i = bl.firstgoto.toInt()
        val gl = ls!!.dyd.gt
        /* correct pending gotos to current block and try to close it
		   with visible labels */
        while (i < ls!!.dyd.n_gt) {
            val gt = gl!![i]!!
            if (gt.nactvar > bl.nactvar) {
                if (bl.upval)
                    patchclose(gt.pc, bl.nactvar.toInt())
                gt.nactvar = bl.nactvar
            }
            if (!ls!!.findlabel(i))
                i++ /* move to next one */
        }
    }

    fun enterblock(bl: BlockCnt, isloop: Boolean) {
        bl.isloop = isloop
        bl.nactvar = nactvar
        bl.firstlabel = ls!!.dyd.n_label.toShort()
        bl.firstgoto = ls!!.dyd.n_gt.toShort()
        bl.upval = false
        bl.previous = this.bl
        this.bl = bl
        _assert(this.freereg == this.nactvar)
    }

    fun leaveblock() {
        val bl = this.bl
        if (bl!!.previous != null && bl.upval) {
            /* create a 'jump to here' to close upvalues */
            val j = this.jump()
            this.patchclose(j, bl.nactvar.toInt())
            this.patchtohere(j)
        }
        if (bl.isloop)
            ls!!.breaklabel()  /* close pending breaks */
        this.bl = bl.previous
        this.removevars(bl.nactvar.toInt())
        _assert(bl.nactvar == this.nactvar)
        this.freereg = this.nactvar  /* free registers */
        ls!!.dyd.n_label = bl.firstlabel.toInt()  /* remove local labels */
        if (bl.previous != null)
        /* inner block? */
            this.movegotosout(bl)  /* update pending gotos to outer block */
        else if (bl.firstgoto < ls!!.dyd.n_gt)
        /* pending gotos in outer block? */
            ls!!.undefgoto(ls!!.dyd!!.gt!![bl.firstgoto.toInt()]!!)  /* error */
    }

    fun closelistfield(cc: ConsControl) {
        if (cc.v.k == LexState.VVOID)
            return  /* there is no list item */
        this.exp2nextreg(cc.v)
        cc.v.k = LexState.VVOID
        if (cc.tostore == Lua.LFIELDS_PER_FLUSH) {
            this.setlist(cc.t!!.u.info, cc.na, cc.tostore) /* flush */
            cc.tostore = 0 /* no more items pending */
        }
    }

    fun hasmultret(k: Int): Boolean {
        return k == LexState.VCALL || k == LexState.VVARARG
    }

    fun lastlistfield(cc: ConsControl) {
        if (cc.tostore == 0) return
        if (hasmultret(cc.v.k)) {
            this.setmultret(cc.v)
            this.setlist(cc.t!!.u.info, cc.na, Lua.LUA_MULTRET)
            cc.na--
            /** do not count last expression (unknown number of elements)  */
        } else {
            if (cc.v.k != LexState.VVOID)
                this.exp2nextreg(cc.v)
            this.setlist(cc.t!!.u.info, cc.na, cc.tostore)
        }
    }


    // =============================================================
    // from lcode.c
    // =============================================================

    fun nil(from: Int, n: Int) {
        var from = from
        var l = from + n - 1  /* last register to set nil */
        if (this.pc > this.lasttarget && pc > 0) {  /* no jumps to current position? */
            val previous_code = f!!.code[pc - 1]
            if (Lua.GET_OPCODE(previous_code) == Lua.OP_LOADNIL) {
                val pfrom = Lua.GETARG_A(previous_code)
                val pl = pfrom + Lua.GETARG_B(previous_code)
                if (pfrom <= from && from <= pl + 1 || from <= pfrom && pfrom <= l + 1) { /* can connect both? */
                    if (pfrom < from)
                        from = pfrom /* from = min(from, pfrom) */
                    if (pl > l)
                        l = pl /* l = max(l, pl) */
                    val previous = InstructionPtr(this.f!!.code, this.pc - 1)
                    Constants.SETARG_A(previous, from)
                    Constants.SETARG_B(previous, l - from)
                    return
                }
            }  /* else go through */
        }
        this.codeABC(Lua.OP_LOADNIL, from, n - 1, 0)
    }


    fun jump(): Int {
        val jpc = this.jpc!!.i /* save list of jumps to here */
        this.jpc!!.i = LexState.NO_JUMP
        val j = IntPtr(this.codeAsBx(Lua.OP_JMP, 0, LexState.NO_JUMP))
        this.concat(j, jpc) /* keep them on hold */
        return j.i
    }

    fun ret(first: Int, nret: Int) {
        this.codeABC(Lua.OP_RETURN, first, nret + 1, 0)
    }

    fun condjump(/* OpCode */op: Int, A: Int, B: Int, C: Int): Int {
        this.codeABC(op, A, B, C)
        return this.jump()
    }

    fun fixjump(pc: Int, dest: Int) {
        val jmp = InstructionPtr(this.f!!.code, pc)
        val offset = dest - (pc + 1)
        _assert(dest != LexState.NO_JUMP)
        if (kotlin.math.abs(offset) > Lua.MAXARG_sBx)
            ls!!.syntaxerror("control structure too long")
        SETARG_sBx(jmp, offset)
    }


    /*
	 * * returns current `pc' and marks it as a jump target (to avoid wrong *
	 * optimizations with consecutive instructions not in the same basic block).
	 */
    fun getlabel(): Int {
        this.lasttarget = this.pc
        return this.pc
    }


    fun getjump(pc: Int): Int {
        val offset = Lua.GETARG_sBx(this.f!!.code[pc])
        /* point to itself represents end of list */
        return if (offset == LexState.NO_JUMP)
        /* end of list */
            LexState.NO_JUMP
        else
        /* turn offset into absolute position */
            pc + 1 + offset
    }


    fun getjumpcontrol(pc: Int): InstructionPtr {
        val pi = InstructionPtr(this.f!!.code, pc)
        return if (pc >= 1 && Lua.testTMode(Lua.GET_OPCODE(pi.code[pi.idx - 1])))
            InstructionPtr(pi.code, pi.idx - 1)
        else
            pi
    }


    /*
	 * * check whether list has any jump that do not produce a value * (or
	 * produce an inverted value)
	 */
    fun need_value(list: Int): Boolean {
        var list = list
        while (list != LexState.NO_JUMP) {
            val i = this.getjumpcontrol(list).get()
            if (Lua.GET_OPCODE(i) != Lua.OP_TESTSET)
                return true
            list = this.getjump(list)
        }
        return false /* not found */
    }


    fun patchtestreg(node: Int, reg: Int): Boolean {
        val i = this.getjumpcontrol(node)
        if (Lua.GET_OPCODE(i.get()) != Lua.OP_TESTSET)
        /* cannot patch other instructions */
            return false
        if (reg != Constants.NO_REG && reg != Lua.GETARG_B(i.get()))
            Constants.SETARG_A(i, reg)
        else
        /* no register to put value or register already has the value */
            i.set(Constants.CREATE_ABC(Lua.OP_TEST, Lua.GETARG_B(i.get()), 0, Lua.GETARG_C(i.get())))

        return true
    }


    fun removevalues(list: Int) {
        var list = list
        while (list != LexState.NO_JUMP) {
            this.patchtestreg(list, Constants.NO_REG)
            list = this.getjump(list)
        }
    }

    fun patchlistaux(list: Int, vtarget: Int, reg: Int, dtarget: Int) {
        var list = list
        while (list != LexState.NO_JUMP) {
            val next = this.getjump(list)
            if (this.patchtestreg(list, reg))
                this.fixjump(list, vtarget)
            else
                this.fixjump(list, dtarget) /* jump to default target */
            list = next
        }
    }

    fun dischargejpc() {
        this.patchlistaux(this.jpc!!.i, this.pc, Constants.NO_REG, this.pc)
        this.jpc!!.i = LexState.NO_JUMP
    }

    fun patchlist(list: Int, target: Int) {
        if (target == this.pc)
            this.patchtohere(list)
        else {
            _assert(target < this.pc)
            this.patchlistaux(list, target, Constants.NO_REG, target)
        }
    }

    fun patchclose(list: Int, level: Int) {
        var list = list
        var level = level
        level++ /* argument is +1 to reserve 0 as non-op */
        while (list != LexState.NO_JUMP) {
            val next = getjump(list)
            _assert(
                Lua.GET_OPCODE(f!!.code[list]) == Lua.OP_JMP && (Lua.GETARG_A(f!!.code[list]) == 0 || Lua.GETARG_A(
                    f!!.code[list]
                ) >= level)
            )
            Constants.SETARG_A(f!!.code, list, level)
            list = next
        }
    }

    fun patchtohere(list: Int) {
        this.getlabel()
        this.concat(this.jpc, list)
    }

    fun concat(l1: IntPtr?, l2: Int) {
        if (l2 == LexState.NO_JUMP)
            return
        if (l1!!.i == LexState.NO_JUMP)
            l1.i = l2
        else {
            var list = l1.i
            var next: Int
            while ((run {
                    next = this.getjump(list)
                    next
                }) != LexState.NO_JUMP)
            /* find last element */
                list = next
            this.fixjump(list, l2)
        }
    }

    fun checkstack(n: Int) {
        val newstack = this.freereg + n
        if (newstack > this.f!!.maxstacksize) {
            if (newstack >= Constants.MAXSTACK)
                ls!!.syntaxerror("function or expression too complex")
            this.f!!.maxstacksize = newstack
        }
    }

    fun reserveregs(n: Int) {
        this.checkstack(n)
        this.freereg = (this.freereg + n).toShort()
    }

    fun freereg(reg: Int) {
        if (!Lua.ISK(reg) && reg >= this.nactvar) {
            this.freereg--
            _assert(reg == this.freereg.toInt())
        }
    }

    fun freeexp(e: expdesc) {
        if (e.k == LexState.VNONRELOC)
            this.freereg(e.u.info)
    }

    fun addk(v: LuaValue?): Int {
        if (this.h == null) {
            this.h = HashMap<LuaValue, Int>()
        } else if (this.h!!.containsKey(v)) {
            return (h!![v] as Int).toInt()
        }
        val idx = this.nk
        this.h!![v!!] = idx
        val f = this.f
        if (f!!.k == null || nk + 1 >= f.k.size)
            f.k = Constants.realloc(f.k, nk * 2 + 1)
        f.k[this.nk++] = v ?: LuaValue.NIL
        return idx
    }

    fun stringK(s: LuaString): Int {
        return this.addk(s)
    }

    fun numberK(r: LuaValue?): Int {
        var r = r
        if (r is LuaDouble) {
            val d = r.todouble()
            val i = d.toInt()
            if (d == i.toDouble())
                r = LuaInteger.valueOf(i)
        }
        return this.addk(r)
    }

    fun boolK(b: Boolean): Int {
        return this.addk(if (b) LuaValue.BTRUE else LuaValue.BFALSE)
    }

    fun nilK(): Int {
        return this.addk(LuaValue.NIL)
    }

    fun setreturns(e: expdesc, nresults: Int) {
        if (e.k == LexState.VCALL) { /* expression is an open function call? */
            Constants.SETARG_C(this.getcodePtr(e), nresults + 1)
        } else if (e.k == LexState.VVARARG) {
            Constants.SETARG_B(this.getcodePtr(e), nresults + 1)
            Constants.SETARG_A(this.getcodePtr(e), this.freereg.toInt())
            this.reserveregs(1)
        }
    }

    fun setoneret(e: expdesc) {
        if (e.k == LexState.VCALL) { /* expression is an open function call? */
            e.k = LexState.VNONRELOC
            e.u.info = Lua.GETARG_A(this.getcode(e))
        } else if (e.k == LexState.VVARARG) {
            Constants.SETARG_B(this.getcodePtr(e), 2)
            e.k = LexState.VRELOCABLE /* can relocate its simple result */
        }
    }

    fun dischargevars(e: expdesc) {
        when (e.k) {
            LexState.VLOCAL -> {
                e.k = LexState.VNONRELOC
            }
            LexState.VUPVAL -> {
                e.u.info = this.codeABC(Lua.OP_GETUPVAL, 0, e.u.info, 0)
                e.k = LexState.VRELOCABLE
            }
            LexState.VINDEXED -> {
                var op = Lua.OP_GETTABUP  /* assume 't' is in an upvalue */
                this.freereg(e.u.ind_idx.toInt())
                if (e.u.ind_vt.toInt() == LexState.VLOCAL) {  /* 't' is in a register? */
                    this.freereg(e.u.ind_t.toInt())
                    op = Lua.OP_GETTABLE
                }
                e.u.info = this.codeABC(op, 0, e.u.ind_t.toInt(), e.u.ind_idx.toInt())
                e.k = LexState.VRELOCABLE
            }
            LexState.VVARARG, LexState.VCALL -> {
                this.setoneret(e)
            }
            else -> {
            }
        }/* there is one value available (somewhere) */
    }

    fun code_label(A: Int, b: Int, jump: Int): Int {
        this.getlabel() /* those instructions may be jump targets */
        return this.codeABC(Lua.OP_LOADBOOL, A, b, jump)
    }

    fun discharge2reg(e: expdesc, reg: Int) {
        this.dischargevars(e)
        when (e.k) {
            LexState.VNIL -> {
                this.nil(reg, 1)
            }
            LexState.VFALSE, LexState.VTRUE -> {
                this.codeABC(
                    Lua.OP_LOADBOOL, reg, if (e.k == LexState.VTRUE) 1 else 0,
                    0
                )
            }
            LexState.VK -> {
                this.codeABx(Lua.OP_LOADK, reg, e.u.info)
            }
            LexState.VKNUM -> {
                this.codeABx(Lua.OP_LOADK, reg, this.numberK(e.u.nval()))
            }
            LexState.VRELOCABLE -> {
                val pc = this.getcodePtr(e)
                Constants.SETARG_A(pc, reg)
            }
            LexState.VNONRELOC -> {
                if (reg != e.u.info)
                    this.codeABC(Lua.OP_MOVE, reg, e.u.info, 0)
            }
            else -> {
                _assert(e.k == LexState.VVOID || e.k == LexState.VJMP)
                return  /* nothing to do... */
            }
        }
        e.u.info = reg
        e.k = LexState.VNONRELOC
    }

    fun discharge2anyreg(e: expdesc) {
        if (e.k != LexState.VNONRELOC) {
            this.reserveregs(1)
            this.discharge2reg(e, this.freereg - 1)
        }
    }

    fun exp2reg(e: expdesc, reg: Int) {
        this.discharge2reg(e, reg)
        if (e.k == LexState.VJMP)
            this.concat(e.t, e.u.info) /* put this jump in `t' list */
        if (e.hasjumps()) {
            val _final: Int /* position after whole expression */
            var p_f = LexState.NO_JUMP /* position of an eventual LOAD false */
            var p_t = LexState.NO_JUMP /* position of an eventual LOAD true */
            if (this.need_value(e.t.i) || this.need_value(e.f.i)) {
                val fj = if (e.k == LexState.VJMP)
                    LexState.NO_JUMP
                else
                    this
                        .jump()
                p_f = this.code_label(reg, 0, 1)
                p_t = this.code_label(reg, 1, 0)
                this.patchtohere(fj)
            }
            _final = this.getlabel()
            this.patchlistaux(e.f.i, _final, reg, p_f)
            this.patchlistaux(e.t.i, _final, reg, p_t)
        }
        e.t.i = LexState.NO_JUMP
        e.f.i = e.t.i
        e.u.info = reg
        e.k = LexState.VNONRELOC
    }

    fun exp2nextreg(e: expdesc) {
        this.dischargevars(e)
        this.freeexp(e)
        this.reserveregs(1)
        this.exp2reg(e, this.freereg - 1)
    }

    fun exp2anyreg(e: expdesc): Int {
        this.dischargevars(e)
        if (e.k == LexState.VNONRELOC) {
            if (!e.hasjumps())
                return e.u.info /* exp is already in a register */
            if (e.u.info >= this.nactvar) { /* reg. is not a local? */
                this.exp2reg(e, e.u.info) /* put value on it */
                return e.u.info
            }
        }
        this.exp2nextreg(e) /* default */
        return e.u.info
    }

    fun exp2anyregup(e: expdesc) {
        if (e.k != LexState.VUPVAL || e.hasjumps())
            exp2anyreg(e)
    }

    fun exp2val(e: expdesc) {
        if (e.hasjumps())
            this.exp2anyreg(e)
        else
            this.dischargevars(e)
    }

    fun exp2RK(e: expdesc): Int {
        this.exp2val(e)
        when (e.k) {
            LexState.VTRUE, LexState.VFALSE, LexState.VNIL -> {
                if (this.nk <= Lua.MAXINDEXRK) { /* constant fit in RK operand? */
                    e.u.info = if (e.k == LexState.VNIL)
                        this.nilK()
                    else
                        this.boolK(e.k == LexState.VTRUE)
                    e.k = LexState.VK
                    return Lua.RKASK(e.u.info)
                }
            }
            LexState.VKNUM -> {
                run {
                    e.u.info = this.numberK(e.u.nval())
                    e.k = LexState.VK
                    /* go through */
                }
                run {
                    if (e.u.info <= Lua.MAXINDEXRK)
                    /* constant fit in argC? */
                        return Lua.RKASK(e.u.info)
                }
            }
            LexState.VK -> {
                if (e.u.info <= Lua.MAXINDEXRK)
                    return Lua.RKASK(e.u.info)
            }
            else -> {
            }
        }
        /* not a constant in the right range: put it in a register */
        return this.exp2anyreg(e)
    }

    fun storevar(`var`: expdesc, ex: expdesc) {
        when (`var`.k) {
            LexState.VLOCAL -> {
                this.freeexp(ex)
                this.exp2reg(ex, `var`.u.info)
                return
            }
            LexState.VUPVAL -> {
                val e = this.exp2anyreg(ex)
                this.codeABC(Lua.OP_SETUPVAL, e, `var`.u.info, 0)
            }
            LexState.VINDEXED -> {
                val op = if (`var`.u.ind_vt.toInt() == LexState.VLOCAL) Lua.OP_SETTABLE else Lua.OP_SETTABUP
                val e = this.exp2RK(ex)
                this.codeABC(op, `var`.u.ind_t.toInt(), `var`.u.ind_idx.toInt(), e)
            }
            else -> {
                _assert(false) /* invalid var kind to store */
            }
        }
        this.freeexp(ex)
    }

    fun self(e: expdesc, key: expdesc) {
        val func: Int
        this.exp2anyreg(e)
        this.freeexp(e)
        func = this.freereg.toInt()
        this.reserveregs(2)
        this.codeABC(Lua.OP_SELF, func, e.u.info, this.exp2RK(key))
        this.freeexp(key)
        e.u.info = func
        e.k = LexState.VNONRELOC
    }

    fun invertjump(e: expdesc) {
        val pc = this.getjumpcontrol(e.u.info)
        _assert(
            Lua.testTMode(Lua.GET_OPCODE(pc.get()))
                    && Lua.GET_OPCODE(pc.get()) != Lua.OP_TESTSET && Lua
                .GET_OPCODE(pc.get()) != Lua.OP_TEST
        )
        // SETARG_A(pc, !(GETARG_A(pc.get())));
        val a = Lua.GETARG_A(pc.get())
        val nota = if (a != 0) 0 else 1
        Constants.SETARG_A(pc, nota)
    }

    fun jumponcond(e: expdesc, cond: Int): Int {
        if (e.k == LexState.VRELOCABLE) {
            val ie = this.getcode(e)
            if (Lua.GET_OPCODE(ie) == Lua.OP_NOT) {
                this.pc-- /* remove previous OP_NOT */
                return this.condjump(Lua.OP_TEST, Lua.GETARG_B(ie), 0, if (cond != 0) 0 else 1)
            }
            /* else go through */
        }
        this.discharge2anyreg(e)
        this.freeexp(e)
        return this.condjump(Lua.OP_TESTSET, Constants.NO_REG, e.u.info, cond)
    }

    fun goiftrue(e: expdesc) {
        val pc: Int /* pc of last jump */
        this.dischargevars(e)
        when (e.k) {
            LexState.VJMP -> {
                this.invertjump(e)
                pc = e.u.info
            }
            LexState.VK, LexState.VKNUM, LexState.VTRUE -> {
                pc = LexState.NO_JUMP /* always true; do nothing */
            }
            else -> {
                pc = this.jumponcond(e, 0)
            }
        }
        this.concat(e.f, pc) /* insert last jump in `f' list */
        this.patchtohere(e.t.i)
        e.t.i = LexState.NO_JUMP
    }

    fun goiffalse(e: expdesc) {
        val pc: Int /* pc of last jump */
        this.dischargevars(e)
        when (e.k) {
            LexState.VJMP -> {
                pc = e.u.info
            }
            LexState.VNIL, LexState.VFALSE -> {
                pc = LexState.NO_JUMP /* always false; do nothing */
            }
            else -> {
                pc = this.jumponcond(e, 1)
            }
        }
        this.concat(e.t, pc) /* insert last jump in `t' list */
        this.patchtohere(e.f.i)
        e.f.i = LexState.NO_JUMP
    }

    fun codenot(e: expdesc) {
        this.dischargevars(e)
        when (e.k) {
            LexState.VNIL, LexState.VFALSE -> {
                e.k = LexState.VTRUE
            }
            LexState.VK, LexState.VKNUM, LexState.VTRUE -> {
                e.k = LexState.VFALSE
            }
            LexState.VJMP -> {
                this.invertjump(e)
            }
            LexState.VRELOCABLE, LexState.VNONRELOC -> {
                this.discharge2anyreg(e)
                this.freeexp(e)
                e.u.info = this.codeABC(Lua.OP_NOT, 0, e.u.info, 0)
                e.k = LexState.VRELOCABLE
            }
            else -> {
                _assert(false) /* cannot happen */
            }
        }
        /* interchange true and false lists */
        run {
            val temp = e.f.i
            e.f.i = e.t.i
            e.t.i = temp
        }
        this.removevalues(e.f.i)
        this.removevalues(e.t.i)
    }

    fun indexed(t: expdesc, k: expdesc) {
        t.u.ind_t = t.u.info.toShort()
        t.u.ind_idx = this.exp2RK(k).toShort()
        _assert(t.k == LexState.VUPVAL || vkisinreg(t.k))
        t.u.ind_vt = (if (t.k == LexState.VUPVAL) LexState.VUPVAL else LexState.VLOCAL).toShort()
        t.k = LexState.VINDEXED
    }

    fun constfolding(op: Int, e1: expdesc, e2: expdesc): Boolean {
        val v1: LuaValue
        val v2: LuaValue
        val r: LuaValue?
        if (!e1.isnumeral() || !e2.isnumeral())
            return false
        if ((op == Lua.OP_DIV || op == Lua.OP_MOD) && e2.u.nval().eq_b(LuaValue.ZERO))
            return false  /* do not attempt to divide by 0 */
        v1 = e1.u.nval()
        v2 = e2.u.nval()
        when (op) {
            Lua.OP_ADD -> r = v1.add(v2)
            Lua.OP_SUB -> r = v1.sub(v2)
            Lua.OP_MUL -> r = v1.mul(v2)
            Lua.OP_DIV -> r = v1.div(v2)
            Lua.OP_MOD -> r = v1.mod(v2)
            Lua.OP_POW -> r = v1.pow(v2)
            Lua.OP_UNM -> r = v1.neg()
            Lua.OP_LEN ->
                // r = v1.len();
                // break;
                return false /* no constant folding for 'len' */
            else -> {
                _assert(false)
                r = null
            }
        }
        if (r!!.todouble().isNaN())
            return false /* do not attempt to produce NaN */
        e1.u.setNval(r)
        return true
    }

    fun codearith(op: Int, e1: expdesc, e2: expdesc, line: Int) {
        if (constfolding(op, e1, e2))
            return
        else {
            val o2 = if (op != Lua.OP_UNM && op != Lua.OP_LEN)
                this.exp2RK(e2)
            else
                0
            val o1 = this.exp2RK(e1)
            if (o1 > o2) {
                this.freeexp(e1)
                this.freeexp(e2)
            } else {
                this.freeexp(e2)
                this.freeexp(e1)
            }
            e1.u.info = this.codeABC(op, 0, o1, o2)
            e1.k = LexState.VRELOCABLE
            fixline(line)
        }
    }

    fun codecomp(/* OpCode */op: Int, cond: Int, e1: expdesc, e2: expdesc) {
        var cond = cond
        var o1 = this.exp2RK(e1)
        var o2 = this.exp2RK(e2)
        this.freeexp(e2)
        this.freeexp(e1)
        if (cond == 0 && op != Lua.OP_EQ) {
            val temp: Int /* exchange args to replace by `<' or `<=' */
            temp = o1
            o1 = o2
            o2 = temp /* o1 <==> o2 */
            cond = 1
        }
        e1.u.info = this.condjump(op, cond, o1, o2)
        e1.k = LexState.VJMP
    }

    fun prefix(/* UnOpr */op: Int, e: expdesc, line: Int) {
        val e2 = expdesc()
        e2.init(LexState.VKNUM, 0)
        when (op) {
            LexState.OPR_MINUS -> {
                if (e.isnumeral())
                /* minus constant? */
                    e.u.setNval(e.u.nval().neg())  /* fold it */
                else {
                    this.exp2anyreg(e)
                    this.codearith(Lua.OP_UNM, e, e2, line)
                }
            }
            LexState.OPR_NOT -> this.codenot(e)
            LexState.OPR_LEN -> {
                this.exp2anyreg(e) /* cannot operate on constants */
                this.codearith(Lua.OP_LEN, e, e2, line)
            }
            else -> _assert(false)
        }
    }

    fun infix(/* BinOpr */op: Int, v: expdesc) {
        when (op) {
            LexState.OPR_AND -> {
                this.goiftrue(v)
            }
            LexState.OPR_OR -> {
                this.goiffalse(v)
            }
            LexState.OPR_CONCAT -> {
                this.exp2nextreg(v) /* operand must be on the `stack' */
            }
            LexState.OPR_ADD, LexState.OPR_SUB, LexState.OPR_MUL, LexState.OPR_DIV, LexState.OPR_MOD, LexState.OPR_POW -> {
                if (!v.isnumeral())
                    this.exp2RK(v)
            }
            else -> {
                this.exp2RK(v)
            }
        }
    }


    fun posfix(op: Int, e1: expdesc, e2: expdesc, line: Int) {
        when (op) {
            LexState.OPR_AND -> {
                _assert(e1.t.i == LexState.NO_JUMP) /* list must be closed */
                this.dischargevars(e2)
                this.concat(e2.f, e1.f.i)
                // *e1 = *e2;
                e1.setvalue(e2)
            }
            LexState.OPR_OR -> {
                _assert(e1.f.i == LexState.NO_JUMP) /* list must be closed */
                this.dischargevars(e2)
                this.concat(e2.t, e1.t.i)
                // *e1 = *e2;
                e1.setvalue(e2)
            }
            LexState.OPR_CONCAT -> {
                this.exp2val(e2)
                if (e2.k == LexState.VRELOCABLE && Lua.GET_OPCODE(this.getcode(e2)) == Lua.OP_CONCAT) {
                    _assert(e1.u.info == Lua.GETARG_B(this.getcode(e2)) - 1)
                    this.freeexp(e1)
                    Constants.SETARG_B(this.getcodePtr(e2), e1.u.info)
                    e1.k = LexState.VRELOCABLE
                    e1.u.info = e2.u.info
                } else {
                    this.exp2nextreg(e2) /* operand must be on the 'stack' */
                    this.codearith(Lua.OP_CONCAT, e1, e2, line)
                }
            }
            LexState.OPR_ADD -> this.codearith(Lua.OP_ADD, e1, e2, line)
            LexState.OPR_SUB -> this.codearith(Lua.OP_SUB, e1, e2, line)
            LexState.OPR_MUL -> this.codearith(Lua.OP_MUL, e1, e2, line)
            LexState.OPR_DIV -> this.codearith(Lua.OP_DIV, e1, e2, line)
            LexState.OPR_MOD -> this.codearith(Lua.OP_MOD, e1, e2, line)
            LexState.OPR_POW -> this.codearith(Lua.OP_POW, e1, e2, line)
            LexState.OPR_EQ -> this.codecomp(Lua.OP_EQ, 1, e1, e2)
            LexState.OPR_NE -> this.codecomp(Lua.OP_EQ, 0, e1, e2)
            LexState.OPR_LT -> this.codecomp(Lua.OP_LT, 1, e1, e2)
            LexState.OPR_LE -> this.codecomp(Lua.OP_LE, 1, e1, e2)
            LexState.OPR_GT -> this.codecomp(Lua.OP_LT, 0, e1, e2)
            LexState.OPR_GE -> this.codecomp(Lua.OP_LE, 0, e1, e2)
            else -> _assert(false)
        }
    }


    fun fixline(line: Int) {
        this.f!!.lineinfo[this.pc - 1] = line
    }


    fun code(instruction: Int, line: Int): Int {
        val f = this.f
        this.dischargejpc() /* `pc' will change */
        /* put new instruction in code array */
        if (f!!.code == null || this.pc + 1 > f.code.size)
            f.code = Constants.realloc(f.code, this.pc * 2 + 1)
        f.code[this.pc] = instruction
        /* save corresponding line information */
        if (f.lineinfo == null || this.pc + 1 > f.lineinfo.size)
            f.lineinfo = Constants.realloc(
                f.lineinfo,
                this.pc * 2 + 1
            )
        f.lineinfo[this.pc] = line
        return this.pc++
    }


    fun codeABC(o: Int, a: Int, b: Int, c: Int): Int {
        _assert(Lua.getOpMode(o) == Constants.iABC)
        _assert(Lua.getBMode(o) != Constants.OpArgN || b == 0)
        _assert(Lua.getCMode(o) != Constants.OpArgN || c == 0)
        return this.code(Constants.CREATE_ABC(o, a, b, c), this.ls!!.lastline)
    }


    fun codeABx(o: Int, a: Int, bc: Int): Int {
        _assert(Lua.getOpMode(o) == Constants.iABx || Lua.getOpMode(o) == Constants.iAsBx)
        _assert(Lua.getCMode(o) == Constants.OpArgN)
        _assert(bc >= 0 && bc <= Lua.MAXARG_Bx)
        return this.code(Constants.CREATE_ABx(o, a, bc), this.ls!!.lastline)
    }


    fun setlist(base: Int, nelems: Int, tostore: Int) {
        val c = (nelems - 1) / Lua.LFIELDS_PER_FLUSH + 1
        val b = if (tostore == Lua.LUA_MULTRET) 0 else tostore
        _assert(tostore != 0)
        if (c <= Lua.MAXARG_C)
            this.codeABC(Lua.OP_SETLIST, base, b, c)
        else {
            this.codeABC(Lua.OP_SETLIST, base, b, 0)
            this.code(c, this.ls!!.lastline)
        }
        this.freereg = (base + 1).toShort() /* free registers with list values */
    }

    companion object {


        fun singlevaraux(fs: FuncState?, n: LuaString, `var`: expdesc, base: Int): Int {
            if (fs == null)
            /* no more levels? */
                return LexState.VVOID  /* default is global */
            val v = fs.searchvar(n) /* look up at current level */
            if (v >= 0) {
                `var`.init(LexState.VLOCAL, v)
                if (base == 0)
                    fs.markupval(v) /* local will be used as an upval */
                return LexState.VLOCAL
            } else { /* not found at current level; try upvalues */
                var idx = fs.searchupvalue(n)  /* try existing upvalues */
                if (idx < 0) {  /* not found? */
                    if (singlevaraux(fs.prev, n, `var`, 0) == LexState.VVOID)
                    /* try upper levels */
                        return LexState.VVOID  /* not found; is a global */
                    /* else was LOCAL or UPVAL */
                    idx = fs.newupvalue(n, `var`)  /* will be a new upvalue */
                }
                `var`.init(LexState.VUPVAL, idx)
                return LexState.VUPVAL
            }
        }


        fun vkisinreg(k: Int): Boolean {
            return k == LexState.VNONRELOC || k == LexState.VLOCAL
        }
    }

}
