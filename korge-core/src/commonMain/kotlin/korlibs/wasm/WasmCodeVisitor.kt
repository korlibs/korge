package korlibs.wasm

import korlibs.datastructure.*

interface WasmCodeVisitor {
    class Context(val func: WasmFunc, val module: WasmModule) {
        val stack = arrayListOf<WasmSType>()
        val controlStructures = arrayListOf<ControlStructure>()
        var lastLabel = 0
        val retType = func.type.retType

        val lastStackType get() = stack.lastOrNull()

        var lastInstructionInputStack = -1
        var lastInstructionOutputType = WasmSType.VOID
        var lastInstructionType = WasmSType.VOID

        fun pop(n: Int = 1) {
            repeat(n) { stack.removeLast() }
        }
        fun push(type: WasmType) {
            if (type != WasmSType.VOID) stack.add(type.toWasmSType())
        }
        fun stackSave(): List<WasmSType> = this.stack.toList()
        fun stackRestore(stack: List<WasmSType>) {
            this.stack.clear()
            this.stack.addAll(stack)
        }

        fun createLabel() = Label(lastLabel++)

        private var currentIndex = 0
        val funcLocals = func.rlocals
        val localsMemOffset = IntArray(funcLocals.size + 1) { index ->
            currentIndex.also {
                if (index < funcLocals.size) {
                    currentIndex += funcLocals[index].stype.nbytes
                }
            }
        }
        val paramsOffset = localsMemOffset[func.type.args.size]
        val localsOffset = localsMemOffset.last()

        fun getLabelByControlStructureIndex(index: Int): Label = controlStructures[controlStructures.size - 1 - index].label

        inline fun <T> addControlStructure(label: Label, block: () -> T): T {
            controlStructures.add(ControlStructure(label))
            try {
                return block()
            } finally {
                controlStructures.removeLast()
            }
        }
    }

    class ControlStructure(val label: Label)
    class Label(val id: Int) {
        var target: Int = -1
    }

    fun visitFuncStart(context: Context) {
    }

    fun visitFuncEnd(context: Context) {
    }

    fun visit(i: WasmInstruction, context: Context) {
    }

    fun visitGoto(label: Label, cond: Boolean?, context: Context) {
    }

    fun visitGotoTable(labels: List<Label>, default: Label, context: Context) {
    }

    fun visitLabel(label: Label, context: Context) {
    }
}

fun WasmInstruction.accept(visitor: WasmCodeVisitor, context: WasmCodeVisitor.Context): Boolean {
    val i = this
    when (i) {
        is WasmInstruction.IF -> {
            val bend = context.createLabel()
            val belse = context.createLabel()
            context.pop()
            val stack = context.stackSave()
            context.addControlStructure(bend) {
                //visitor.visit(i, context)
                visitor.visitGoto(if (i.bfalse != null) belse else bend, false, context)
                i.btrue.accept(visitor, context)
                if (i.bfalse != null) {
                    visitor.visitGoto(bend, null, context)
                    visitor.visitLabel(belse, context)
                    context.stackRestore(stack)
                    i.bfalse.accept(visitor, context)
                }
            }
            context.stackRestore(stack)
            context.push(i.b)
            visitor.visitLabel(bend, context)
        }
        is WasmInstruction.BlockOrLoop -> {
            val end = context.createLabel()
            val stack = context.stackSave()
            context.addControlStructure(end) {
                if (i is WasmInstruction.loop) visitor.visitLabel(end, context)
                //visitor.visit(i, context)
                i.expr.accept(visitor, context)
            }
            context.stackRestore(stack)
            context.push(i.b)
            if (i is WasmInstruction.block) visitor.visitLabel(end, context)
        }
        is WasmInstruction.nop -> {
        }
        is WasmInstruction.End -> {
        }
        is WasmInstruction.br -> {
            visitor.visitGoto(context.getLabelByControlStructureIndex(i.label), null, context)
            return true
        }
        is WasmInstruction.br_if -> {
            visitor.visitGoto(context.getLabelByControlStructureIndex(i.label), true, context)
        }
        is WasmInstruction.br_table -> {
            val labels = i.labels.map { context.getLabelByControlStructureIndex(it) }
            visitor.visitGotoTable(labels, context.getLabelByControlStructureIndex(i.default), context)
            return true
        }
        else -> {
            var inputStack = i.op.istack
            var outType: WasmSType = i.op.outType
            var instructionType: WasmSType = WasmSType.VOID

            when (i.op) {
                WasmOp.Op_call -> {
                    i as WasmInstruction.CALL
                    val func = context.module.functions[i.funcIdx]
                    inputStack = func.func.type.args.size
                    instructionType = func.func.type.retType.toWasmSType()
                    outType = instructionType
                }
                WasmOp.Op_call_indirect -> {
                    i as WasmInstruction.CALL_INDIRECT
                    val func = context.module.types[i.typeIdx].type as WasmType.Function
                    inputStack = func.args.size + 1 // + 1 because being indirect the methodId will be in the stack
                    instructionType = func.retType.toWasmSType()
                    outType = instructionType
                }
                WasmOp.Op_return -> {
                    instructionType = WasmSType.VOID
                    outType = instructionType
                }
                WasmOp.Op_select -> {
                    instructionType = context.stack.getCyclic(-2)
                    outType = instructionType
                }
                WasmOp.Op_drop -> {
                    instructionType = context.stack.last()
                }
                WasmOp.Op_local_get, WasmOp.Op_local_tee, WasmOp.Op_local_set -> {
                    instructionType = context.func.rlocals[(i as WasmInstruction.InsInt).param].stype
                    if (i.op != WasmOp.Op_global_set) outType = instructionType
                }
                WasmOp.Op_global_get, WasmOp.Op_global_set -> {
                    instructionType = context.module.globalsByIndex[(i as WasmInstruction.InsInt).param]!!.globalType.toWasmSType()
                    if (i.op != WasmOp.Op_global_set) outType = instructionType
                }
                else -> {
                    if (inputStack == -1) TODO("$i : ${inputStack}")
                    //if (i.op.rstack == -1) TODO("$i : ${i.op.rstack}")
                }
            }

            context.lastInstructionInputStack = inputStack
            context.lastInstructionOutputType = outType
            context.lastInstructionType = instructionType

            visitor.visit(i, context)

            context.pop(inputStack)
            context.push(outType)
            if (i.op == WasmOp.Op_return) {
                return true
            }
        }
    }
    return false
}

fun WasmExpr.accept(visitor: WasmCodeVisitor, context: WasmCodeVisitor.Context) {
    for (i in this.instructions) {
        if (i.accept(visitor, context)) {
            // end instructiom
            break
        }
    }
}

fun WasmFunc.accept(module: WasmModule, implicitReturn: Boolean, visitor: WasmCodeVisitor) {
    val context = WasmCodeVisitor.Context(this, module)
    visitor.visitFuncStart(context)
    this.code?.body?.accept(visitor, context)
    if (implicitReturn && this.code?.body?.instructions?.lastOrNull()?.op != WasmOp.Op_return) {
        WasmInstruction.RETURN.accept(visitor, context)
    }
    visitor.visitFuncEnd(context)
}
