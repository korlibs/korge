package korlibs.wasm

import korlibs.math.*
import korlibs.memory.*
import korlibs.wasm.*
import korlibs.wasm.WasmRunJVMOutput.Companion.isStatic
import korlibs.wasm.WasmSType.*
import org.objectweb.asm.*
import org.objectweb.asm.Type
import java.io.*
import java.lang.invoke.*
import java.lang.reflect.*
import java.security.*
import kotlin.jvm.functions.*
import kotlin.reflect.*

open class WasmRunJVMJIT(module: WasmModule, memSize: Int, memMax: Int) : WasmRuntime(module, memSize, memMax) {

    // TODO make this an inline class wrapping an integer referencing to some address in a Buffer/Memory?
    // Alternatively allocate two I64 in the stack/locals
    class V128(val data: ByteArray) {
        init {
            check(data.size == 16)
        }

        fun i(index: Int): Int = data.getS32LE(index * 4)
        fun l(index: Int): Long = data.getS64LE(index * 8)
        fun f(index: Int): Float = data.getF32LE(index * 4)
        fun d(index: Int): Double = data.getF64LE(index * 8)
    }

    /*
    data class V128(val i0: Int, val i1: Int, val i2: Int, val i3: Int) {
        constructor(l0: Long, l1: Long) : this(l0.low, l0.high, l1.low, l1.high)
        constructor(f0: Float, f1: Float, f2: Float, f3: Float) : this(f0.toRawBits(), f1.toRawBits(), f2.toRawBits(), f3.toRawBits())
        constructor(d0: Double, d1: Double) : this(d0.toRawBits(), d1.toRawBits())

        val l0: Long get() = Long.fromLowHigh(i0, i1)
        val l1: Long get() = Long.fromLowHigh(i2, i3)

        val f0: Float get() = Float.fromBits(i0)
        val f1: Float get() = Float.fromBits(i1)
        val f2: Float get() = Float.fromBits(i2)
        val f3: Float get() = Float.fromBits(i3)

        val d0: Double get() = Double.fromBits(l0)
        val d1: Double get() = Double.fromBits(l1)
    }
     */

    val declaredMethodsByName: Map<String, Method> by lazy {
        //(this::class.java.declaredMethods + WasmRuntime::class.java.declaredMethods).associateBy { it.name }
        this::class.java.declaredMethods.filter {
            //println("${it.name} : ${it.declaringClass}")
            it.isStatic()
        }.associateBy { it.name }
    }

    override val exported: Set<String> get() = declaredMethodsByName.keys

    override operator fun invoke(name: String, vararg params: Any?) : Any? {
        //return declaredMethodsByName[name]!!.invoke(this, *params)
        val method = declaredMethodsByName[name] ?: error("Can't find method '$name' in ${declaredMethodsByName.keys}")
        if (!method.modifiers.hasBits(Modifier.STATIC)) error("Can't call non-static method '$name'")
        //if (params == null) error("Params is null")
        return method.invoke(null, *params, this)
    }

    val imports = LinkedHashMap<String, LinkedHashMap<String, WasmRuntime.(params: Array<Any?>) -> Any?>>()

    // Register import
    override fun register(module: String, funcName: String, function: WasmRuntime.(params: Array<Any?>) -> Any?) {
        imports.getOrPut(module) { LinkedHashMap() }[funcName] = function
    }

    fun invokeImport(module: String, funcName: String, params: Array<Any?>): Any? {
        val func = imports[module]?.get(funcName) ?: error("Can't find method $module\$$funcName")
        return func.invoke(this, params)
    }

    class MethodWasmFuncCall(val obj: Any?, val method: Method?, val methodName: String) : WasmFuncCall {
        override operator fun invoke(runtime: WasmRuntime, args: Array<Any?>): Any? {
            if (method == null) error("Can't find method '$methodName'")
            //println("method=$method, args=${args.toList()}")
            return method.invoke(obj, *args, runtime)
        }
    }

    fun setElements(elements: java.util.ArrayList<WasmRunJVMOutput.ElementInfo>) {
        for (element in elements) {
            val out = tables[element.tableIndex].elements
            for (n in 0 until element.elements.size) {
                val elem = element.elements[n]
                out[element.index + n] = MethodWasmFuncCall(null, this.declaredMethodsByName[elem.funcName] ?: error("Can't find $elem"), elem.funcName)
            }
        }
    }

    companion object {
        operator fun get(func: KFunction<*>): Method {
            return declaredMethodsByName[func.name] ?: error("Can't find method ${func.name}")
        }

        val declaredMethodsByName by lazy {
            //WasmRunJVMJIT::class.java.declaredMethods.associateBy { it.name }
            (WasmRunJVMJIT::class.java.declaredMethods + WasmRuntime::class.java.declaredMethods).associateBy { it.name }
        }

        fun build(module: WasmModule, outputGen: () -> WasmRunJVMOutput = { WasmRunJVMOutput() }, codeTrace: Boolean = false): WasmRunJVMJIT =
            outputGen().also { it.trace = codeTrace }.generate(module)
    }
}

open class WasmRunJVMOutput(
    val OUTPUT_CLASS_NAME: String = "WasmProgram",
) {

    fun MethodVisitor.generateFunc(func: WasmFunc, module: WasmModule) {
        val context = GenMethodContext(func.name, this, func, module)

        if (trace) {
            println("- func: topLevelIns=${func.code?.body?.instructions?.size}")
            println("- localsIndices:")
            for ((index, local) in context.locals.withIndex()) {
                println("  - [$index] $local")
            }
        }
        //context.runtimeLocalIndex = func.rlocals.size
        //context.runtimeLocalIndex = 0

        // Copy runtime argument to a non-usable place in the stack
        if (func.type.args.size != context.runtimeLocalIndex) {
            context.load(func.type.args.size, VOID)
            context.store(context.runtimeLocalIndex, VOID)
        }

        // Init to zero all items
        for (localIndex in func.type.args.size until func.rlocals.size) {
            when (func.rlocals[localIndex].stype) {
                VOID -> TODO()
                I32 -> constant(0).also { context.istore(localIndex) }
                I64 -> constant(0L).also { context.lstore(localIndex) }
                F32 -> constant(0f).also { context.fstore(localIndex) }
                F64 -> constant(0.0).also { context.dstore(localIndex) }
                V128 -> TODO("v128")
                ANYREF -> TODO()
                FUNCREF -> TODO()
            }
        }

        generateExpr(func.code?.body!!, context, 0)
        if (context.typeStack.size > 0) {
            if (trace) println("context.typeStack.size=${context.typeStack.size}")
            when (func.type.retType.toWasmSType()) {
                VOID -> ret()
                I32 -> iret()
                I64 -> lret()
                F32 -> fret()
                F64 -> dret()
                V128 -> TODO("v128")
                ANYREF -> TODO()
                FUNCREF -> TODO()
            }
        }
    }

    data class ControlStructure(val kind: String, val startStack: List<WasmSType>, val result: WasmType, val label: Label = Label())

    class GenMethodContext(val name: String, val methodVisitor: MethodVisitor, val func: WasmFunc, val module: WasmModule) {
        val controlStack = arrayListOf<ControlStructure>()
        var typeStack = arrayListOf<WasmSType>()

        private var currentIndex = 0

        data class LocalInfo(val index: Int, val type: WasmSType?, val kind: LocalKind)

        val locals = arrayListOf<LocalInfo>()

        fun getControlByRelativeIndex(offset: Int): ControlStructure {
            return controlStack[controlStack.size - 1 - offset]
        }

        fun addLocal(type: WasmSType?, kind: LocalKind): Int {
            val rindex = locals.size
            val index = currentIndex
            locals += LocalInfo(index, type, kind)
            when (type?.toWasmSType()) {
                null -> currentIndex += 1
                VOID -> currentIndex += 1
                I32 -> currentIndex += 1
                I64 -> currentIndex += 2
                F32 -> currentIndex += 1
                F64 -> currentIndex += 2
                V128 -> TODO("v128")
                ANYREF -> TODO()
                FUNCREF -> TODO()
            }
            return rindex
        }

        enum class LocalKind { PARAM, LOCAL, RUNTIME, TEMP }

        init {
            for (local in func.type.args) {
                addLocal(local.stype, LocalKind.PARAM)
            }
            //if (trace) println("LOCALS: " + func.code?.locals)
            for (local in (func.code?.flatLocals ?: emptyList())) {
                addLocal(local.stype, LocalKind.LOCAL)
            }
        }
        val runtimeLocalIndex: Int = addLocal(null, LocalKind.RUNTIME)
        val longTempIndex: Int = addLocal(I64, LocalKind.TEMP)
        val longTemp: LocalInfo = locals[longTempIndex]

        fun getLocalIndex(index: Int, managed: Boolean = true): Int = if (managed) locals[index].index else index

        fun astore(index: Int, managed: Boolean = true) = methodVisitor._astore(getLocalIndex(index, managed))
        fun istore(index: Int, managed: Boolean = true) = methodVisitor._istore(getLocalIndex(index, managed))
        fun lstore(index: Int, managed: Boolean = true) = methodVisitor._lstore(getLocalIndex(index, managed))
        fun fstore(index: Int, managed: Boolean = true) = methodVisitor._fstore(getLocalIndex(index, managed))
        fun dstore(index: Int, managed: Boolean = true) = methodVisitor._dstore(getLocalIndex(index, managed))

        fun aload(index: Int, managed: Boolean = true) = methodVisitor._aload(getLocalIndex(index, managed))
        fun iload(index: Int, managed: Boolean = true) = methodVisitor._iload(getLocalIndex(index, managed))
        fun lload(index: Int, managed: Boolean = true) = methodVisitor._lload(getLocalIndex(index, managed))
        fun fload(index: Int, managed: Boolean = true) = methodVisitor._fload(getLocalIndex(index, managed))
        fun dload(index: Int, managed: Boolean = true) = methodVisitor._dload(getLocalIndex(index, managed))

        fun store(index: Int, type: WasmSType?, managed: Boolean = true) {
            when (type) {
                null, VOID -> astore(index, managed)
                I32 -> istore(index, managed)
                I64 -> lstore(index, managed)
                F32 -> fstore(index, managed)
                F64 -> dstore(index, managed)
                V128 -> TODO("v128")
                ANYREF -> TODO()
                FUNCREF -> TODO()
            }
        }

        fun ret(type: WasmSType?) {
            when (type) {
                null -> methodVisitor.aret()
                VOID -> methodVisitor.ret()
                I32 -> methodVisitor.iret()
                I64 -> methodVisitor.lret()
                F32 -> methodVisitor.fret()
                F64 -> methodVisitor.dret()
                V128 -> TODO("v128")
                ANYREF -> TODO()
                FUNCREF -> TODO()
            }
        }

        fun load(index: Int, type: WasmSType?, managed: Boolean = true) {
            when (type) {
                null, VOID -> aload(index, managed)
                I32 -> iload(index, managed)
                I64 -> lload(index, managed)
                F32 -> fload(index, managed)
                F64 -> dload(index, managed)
                V128 -> TODO("v128")
                ANYREF -> TODO()
                FUNCREF -> TODO()
            }
        }

        fun local(index: Int): WastLocal = func.rlocals[index]
        fun localType(index: Int): WasmSType = local(index).stype

        fun global(index: Int): WasmGlobal = module.globals[index]
        fun globalType(index: Int): WasmSType = global(index).globalType.toWasmSType()

        fun stack(remove: Int, vararg add: WasmSType) {
            repeat(remove) { typeStack.removeLast() }
            for (a in add) if (a != VOID) typeStack.add(a)
        }

        fun saveStack(): List<WasmSType> {
            return typeStack.toList()
        }

        fun restoreStack(types: List<WasmSType>, extra: WasmSType? = null) {
            typeStack = ArrayList(types)
            if (extra != null && extra != VOID) typeStack.add(extra)
        }

        inline fun pushControlStructure(cs: ControlStructure, block: () -> Unit) {
            controlStack.add(cs)
            try {
                block()
            } finally {
                controlStack.removeLast()
            }
        }

        fun drop(type: WasmSType) {
            //println("                  - DROP")
            when (type) {
                ANYREF -> TODO()
                VOID -> Unit
                I32, F32 -> methodVisitor.visitInsn(Opcodes.POP)
                I64, F64 -> methodVisitor.visitInsn(Opcodes.POP2)
                V128 -> TODO("v128")
                FUNCREF -> TODO()
            }
        }

        fun storeTemp(result: WasmType, updateStack: Boolean = true) {
            store(currentIndex, result.toWasmSType(), managed = false)
            if (updateStack) stack(1)
        }

        fun loadTemp(result: WasmType, updateStack: Boolean = true) {
            load(currentIndex, result.toWasmSType(), managed = false)
            if (updateStack) stack(0, result.toWasmSType())
        }

        fun prepareToJump(controlStructure: ControlStructure, updateStack: Boolean) {
            var toDrop = typeStack.size - controlStructure.startStack.size - 1
            if (controlStructure.result == VOID) {
                toDrop++
            }
            //println(" --- toDrop=$toDrop, controlStructure=$controlStructure, typeStack=$typeStack")
            if (toDrop > 0) {
                if (controlStructure.result != VOID) storeTemp(controlStructure.result, updateStack = false)
                for (n in 0 until toDrop) {
                    drop(typeStack[typeStack.size - 1 - n])
                }
                if (updateStack) repeat(toDrop) { typeStack.removeLast() }
                if (controlStructure.result != VOID) loadTemp(controlStructure.result, updateStack = false)
            }
        }
    }

    fun WasmType.toNullValue(): Any? {
        return when (this) {
            is CustomWasmType -> TODO()
            is WasmType.Function -> null
            //is WasmType.Global -> this.type.toNullValue()
            is WasmType.Limit -> TODO()
            VOID -> Unit
            I32 -> 0
            I64 -> 0L
            F32 -> 0f
            F64 -> 0.0
            is WasmType._ARRAY -> TODO()
            is WasmType._NULLABLE -> null
            is WasmType._VARARG -> TODO()
            WasmType._boolean -> 0
            WasmType._i16 -> 0
            WasmType._i8 -> 0
            WasmType.v128 -> TODO()
            V128 -> TODO("v128")
            ANYREF -> TODO()
            FUNCREF -> TODO()
            is WasmType.Mutable -> this.rtype.toNullValue()
        }
    }

    fun WasmType.toJDescriptor(isImport: Boolean = false): String {
        return when (this) {
            is CustomWasmType -> TODO()
            //is WasmType.Function -> "(${args.joinToString("") { it.type.toJDescriptor(isImport) }}${ if (isImport) Type.getDescriptor(WasmJIT::class.java) else "LWasmProgram;" })${retType.toJDescriptor()}"
            is WasmType.Function -> {
                if (cachedJDescriptor == null) {
                    cachedJDescriptor = "(${args.joinToString("") { it.type.toJDescriptor(isImport) }}LWasmProgram;)${retType.toJDescriptor()}"
                }
                return cachedJDescriptor!!
            }
            //is WasmType.Global -> this.type.toJDescriptor(isImport)
            is WasmType.Limit -> TODO()
            VOID -> "V"
            I32 -> "I"
            I64 -> "J"
            F32 -> "F"
            F64 -> "D"
            is WasmType._ARRAY -> TODO()
            is WasmType._NULLABLE -> TODO()
            is WasmType._VARARG -> TODO()
            WasmType._boolean -> TODO()
            WasmType._i16 -> TODO()
            WasmType._i8 -> TODO()
            WasmType.v128 -> TODO()
            V128 -> TODO("v128")
            ANYREF -> TODO()
            FUNCREF -> TODO()
            is WasmType.Mutable -> this.rtype.toJDescriptor(isImport)
        }
    }

    var trace = false
    var validate = true

    fun MethodVisitor.generateInstruction(
        i: WasmInstruction, context: GenMethodContext, indent: Int,
    ) {
        if (trace) println("${"  ".repeat(indent)}I: $i <-- ${context.typeStack}")

        fun aloadRuntime() {
            context.aload(context.runtimeLocalIndex)
        }

        when (i) {
            is WasmInstruction.CALL -> {
                val func = context.module.functions[i.funcIdx]
                aloadRuntime()
                visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    OUTPUT_CLASS_NAME,
                    func.getJvmName(),
                    func.type.toJDescriptor(func.fimport != null),
                    false
                )
                context.stack(func.type.args.size, func.type.retType.toWasmSType())
            }
            is WasmInstruction.CALL_INDIRECT -> {
                val table = context.module.tables.first()
                val funcType = context.module.types[i.typeIdx].type as WasmType.Function

                if (trace) println("${"  ".repeat(indent)} - funcType=$funcType")
                context.aload(context.runtimeLocalIndex)
                visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    OUTPUT_CLASS_NAME, "call\$indirect", funcType.getJvmDescriptor(Int::class.javaPrimitiveType!!, OUTPUT_CLASS_NAME), false
                )
                //println("CALL_INDIRECT: $funcType : context.module.tables=${context.module.tables.size}, table=${table.items.size}")
                context.stack(funcType.args.size + 1, funcType.retType.toWasmSType())

                //invoke(WasmRuntime[WasmRuntime::create_todo_exception_instance])
                //visitInsn(Opcodes.ATHROW)
            }
            is WasmInstruction.ELSE -> TODO("$i")
            WasmInstruction.End -> Unit
            is WasmInstruction.IF -> {
                val jop = when (i.compOp) {
                    null -> Opcodes.IFNE.also { context.stack(1, I32) }
                    WasmOp.Op_i32_eq -> Opcodes.IF_ICMPEQ.also { context.stack(2, I32) }
                    WasmOp.Op_i32_ne -> Opcodes.IF_ICMPNE.also { context.stack(2, I32) }
                    WasmOp.Op_i32_gt_s -> Opcodes.IF_ICMPGT.also { context.stack(2, I32) }
                    WasmOp.Op_i32_lt_s -> Opcodes.IF_ICMPLT.also { context.stack(2, I32) }
                    WasmOp.Op_i32_ge_s -> Opcodes.IF_ICMPGE.also { context.stack(2, I32) }
                    WasmOp.Op_i32_le_s -> Opcodes.IF_ICMPLE.also { context.stack(2, I32) }
                    else -> TODO("${i.compOp}")
                }

                context.stack(1)
                val stack = context.saveStack()
                val cs = ControlStructure("if", stack, i.b)
                context.pushControlStructure(cs) {
                    if (trace) println("${"  ".repeat(indent)}!!!!!!!! BLOCK TYPE: ${i.b}, input: $stack")
                    if (i.bfalse != null) {
                        IF_ELSE(jop, {
                            if (trace) println("${"  ".repeat(indent)}I: THEN:")
                            context.restoreStack(stack)
                            generateExpr(i.btrue, context, indent + 1)
                        }, {
                            if (trace) println("${"  ".repeat(indent)}I: ELSE:")
                            context.restoreStack(stack)
                            generateExpr(i.bfalse, context, indent + 1)
                        }, endLabel = cs.label)
                    } else {
                        IF(jop, endLabel = cs.label) {
                            if (trace) println("${"  ".repeat(indent)}I: THEN:")
                            context.restoreStack(stack)
                            generateExpr(i.btrue, context, indent + 1)
                        }
                    }
                    context.restoreStack(stack, i.b.toWasmSType())
                    //visitLabel(cs.label)
                    //println("IF BREAK label=${cs.label}")
                }
            }
            is WasmInstruction.Ins -> {
                when (i.op) {
                    // Floating point binop
                    WasmOp.Op_f32_add -> fadd().also { context.stack(2, F32) }
                    WasmOp.Op_f32_sub -> fsub().also { context.stack(2, F32) }
                    WasmOp.Op_f32_mul -> fmul().also { context.stack(2, F32) }
                    WasmOp.Op_f32_div -> fdiv().also { context.stack(2, F32) }
                    WasmOp.Op_f32_min -> invoke(Op_f32_min).also { context.stack(2, F32) }
                    WasmOp.Op_f32_max -> invoke(Op_f32_max).also { context.stack(2, F32) }
                    WasmOp.Op_f32_copysign -> invoke(Op_f32_copysign).also { context.stack(2, F32) }
                    // Floating point unop
                    WasmOp.Op_f32_abs -> invoke(Op_f32_abs).also { context.stack(1, F32) }
                    WasmOp.Op_f32_sqrt -> invoke(Op_f32_sqrt).also { context.stack(1, F32) }
                    WasmOp.Op_f32_neg -> invoke(Op_f32_neg).also { context.stack(1, F32) }
                    WasmOp.Op_f32_ceil -> invoke(Op_f32_ceil).also { context.stack(1, F32) }
                    WasmOp.Op_f32_floor -> invoke(Op_f32_floor).also { context.stack(1, F32) }
                    WasmOp.Op_f32_trunc -> invoke(Op_f32_trunc).also { context.stack(1, F32) }
                    WasmOp.Op_f32_nearest -> invoke(Op_f32_nearest).also { context.stack(1, F32) }

                    // Double point binop
                    WasmOp.Op_f64_add -> dadd().also { context.stack(2, F64) }
                    WasmOp.Op_f64_sub -> dsub().also { context.stack(2, F64) }
                    WasmOp.Op_f64_mul -> dmul().also { context.stack(2, F64) }
                    WasmOp.Op_f64_div -> ddiv().also { context.stack(2, F64) }
                    WasmOp.Op_f64_min -> invoke(Op_f64_min).also { context.stack(2, F64) }
                    WasmOp.Op_f64_max -> invoke(Op_f64_max).also { context.stack(2, F64) }
                    WasmOp.Op_f64_copysign -> invoke(Op_f64_copysign).also { context.stack(2, F64) }
                    // Double point unop
                    WasmOp.Op_f64_abs -> invoke(Op_f64_abs).also { context.stack(1, F64) }
                    WasmOp.Op_f64_sqrt -> invoke(Op_f64_sqrt).also { context.stack(1, F64) }
                    WasmOp.Op_f64_neg -> invoke(Op_f64_neg).also { context.stack(1, F64) }
                    WasmOp.Op_f64_ceil -> invoke(Op_f64_ceil).also { context.stack(1, F64) }
                    WasmOp.Op_f64_floor -> invoke(Op_f64_floor).also { context.stack(1, F64) }
                    WasmOp.Op_f64_trunc -> invoke(Op_f64_trunc).also { context.stack(1, F64) }
                    WasmOp.Op_f64_nearest -> invoke(Op_f64_nearest).also { context.stack(1, F64) }

                    WasmOp.Op_i32_sub -> isub().also { context.stack(2, I32) }
                    WasmOp.Op_i32_add -> iadd().also { context.stack(2, I32) }
                    WasmOp.Op_i32_mul -> imul().also { context.stack(2, I32) }
                    WasmOp.Op_i32_div_s -> idiv().also { context.stack(2, I32) }
                    WasmOp.Op_i32_rem_s -> irem().also { context.stack(2, I32) }
                    WasmOp.Op_i32_div_u -> invoke(Op_i32_div_u).also { context.stack(2, I32) }
                    WasmOp.Op_i32_rem_u -> invoke(Op_i32_rem_u).also { context.stack(2, I32) }
                    WasmOp.Op_i32_and -> iand().also { context.stack(2, I32) }
                    WasmOp.Op_i32_or -> ior().also { context.stack(2, I32) }
                    WasmOp.Op_i32_xor -> ixor().also { context.stack(2, I32) }
                    WasmOp.Op_i32_shl -> ishl().also { context.stack(2, I32) }
                    WasmOp.Op_i32_shr_s -> ishr().also { context.stack(2, I32) }
                    WasmOp.Op_i32_shr_u -> iushr().also { context.stack(2, I32) }
                    WasmOp.Op_i32_rotl -> invoke(Op_i32_rotl).also { context.stack(2, I32) }
                    WasmOp.Op_i32_rotr -> invoke(Op_i32_rotr).also { context.stack(2, I32) }
                    WasmOp.Op_i32_clz -> invoke(Op_i32_clz).also { context.stack(1, I32) }
                    WasmOp.Op_i32_ctz -> invoke(Op_i32_ctz).also { context.stack(1, I32) }

                    WasmOp.Op_i32_popcnt -> invoke(Op_i32_popcnt).also { context.stack(1, I32) }
                    WasmOp.Op_i64_popcnt -> invoke(Op_i64_popcnt).also { context.stack(1, I64) }

                    WasmOp.Op_i64_sub -> lsub().also { context.stack(2, I64) }
                    WasmOp.Op_i64_add -> ladd().also { context.stack(2, I64) }
                    WasmOp.Op_i64_mul -> lmul().also { context.stack(2, I64) }
                    WasmOp.Op_i64_div_s -> ldiv().also { context.stack(2, I64) }
                    WasmOp.Op_i64_rem_s -> lrem().also { context.stack(2, I64) }
                    WasmOp.Op_i64_div_u -> invoke(Op_i64_div_u).also { context.stack(2, I64) }
                    WasmOp.Op_i64_rem_u -> invoke(Op_i64_rem_u).also { context.stack(2, I64) }
                    WasmOp.Op_i64_and -> land().also { context.stack(2, I64) }
                    WasmOp.Op_i64_or -> lor().also { context.stack(2, I64) }
                    WasmOp.Op_i64_xor -> lxor().also { context.stack(2, I64) }
                    WasmOp.Op_i64_shl -> {
                        visitInsn(Opcodes.L2I)
                        lshl().also { context.stack(2, I64) }
                    }
                    WasmOp.Op_i64_shr_s -> {
                        visitInsn(Opcodes.L2I)
                        lshr().also { context.stack(2, I64) }
                    }
                    WasmOp.Op_i64_shr_u -> {
                        visitInsn(Opcodes.L2I)
                        lushr().also { context.stack(2, I64) }
                    }
                    WasmOp.Op_i64_rotl -> invoke(Op_i64_rotl).also { context.stack(2, I64) }
                    WasmOp.Op_i64_rotr -> invoke(Op_i64_rotr).also { context.stack(2, I64) }
                    WasmOp.Op_i64_clz -> invoke(Op_i64_clz).also { context.stack(1, I64) }
                    WasmOp.Op_i64_ctz -> invoke(Op_i64_ctz).also { context.stack(1, I64) }

                    WasmOp.Op_i32_eqz -> invoke(Op_i32_eqz).also { context.stack(1, I32) }
                    WasmOp.Op_i32_eq -> invoke(Op_i32_eq).also { context.stack(2, I32) }
                    WasmOp.Op_i32_ne -> invoke(Op_i32_ne).also { context.stack(2, I32) }
                    WasmOp.Op_i32_lt_s -> invoke(Op_i32_lt_s).also { context.stack(2, I32) }
                    WasmOp.Op_i32_le_s -> invoke(Op_i32_le_s).also { context.stack(2, I32) }
                    WasmOp.Op_i32_ge_s -> invoke(Op_i32_ge_s).also { context.stack(2, I32) }
                    WasmOp.Op_i32_gt_s -> invoke(Op_i32_gt_s).also { context.stack(2, I32) }
                    WasmOp.Op_i32_lt_u -> invoke(Op_i32_lt_u).also { context.stack(2, I32) }
                    WasmOp.Op_i32_le_u -> invoke(Op_i32_le_u).also { context.stack(2, I32) }
                    WasmOp.Op_i32_ge_u -> invoke(Op_i32_ge_u).also { context.stack(2, I32) }
                    WasmOp.Op_i32_gt_u -> invoke(Op_i32_gt_u).also { context.stack(2, I32) }

                    WasmOp.Op_i64_eqz -> invoke(Op_i64_eqz).also { context.stack(1, I32) }
                    WasmOp.Op_i64_eq -> invoke(Op_i64_eq).also { context.stack(2, I32) }
                    WasmOp.Op_i64_ne -> invoke(Op_i64_ne).also { context.stack(2, I32) }
                    WasmOp.Op_i64_lt_s -> invoke(Op_i64_lt_s).also { context.stack(2, I32) }
                    WasmOp.Op_i64_le_s -> invoke(Op_i64_le_s).also { context.stack(2, I32) }
                    WasmOp.Op_i64_ge_s -> invoke(Op_i64_ge_s).also { context.stack(2, I32) }
                    WasmOp.Op_i64_gt_s -> invoke(Op_i64_gt_s).also { context.stack(2, I32) }
                    WasmOp.Op_i64_lt_u -> invoke(Op_i64_lt_u).also { context.stack(2, I32) }
                    WasmOp.Op_i64_le_u -> invoke(Op_i64_le_u).also { context.stack(2, I32) }
                    WasmOp.Op_i64_ge_u -> invoke(Op_i64_ge_u).also { context.stack(2, I32) }
                    WasmOp.Op_i64_gt_u -> invoke(Op_i64_gt_u).also { context.stack(2, I32) }

                    WasmOp.Op_f32_le -> invoke(Op_f32_le).also { context.stack(2, I32) }
                    WasmOp.Op_f32_lt -> invoke(Op_f32_lt).also { context.stack(2, I32) }
                    WasmOp.Op_f32_eq -> invoke(Op_f32_eq).also { context.stack(2, I32) }
                    WasmOp.Op_f32_ne -> invoke(Op_f32_ne).also { context.stack(2, I32) }
                    WasmOp.Op_f32_gt -> invoke(Op_f32_gt).also { context.stack(2, I32) }
                    WasmOp.Op_f32_ge -> invoke(Op_f32_ge).also { context.stack(2, I32) }

                    WasmOp.Op_f64_le -> invoke(Op_f64_le).also { context.stack(2, I32) }
                    WasmOp.Op_f64_lt -> invoke(Op_f64_lt).also { context.stack(2, I32) }
                    WasmOp.Op_f64_eq -> invoke(Op_f64_eq).also { context.stack(2, I32) }
                    WasmOp.Op_f64_ne -> invoke(Op_f64_ne).also { context.stack(2, I32) }
                    WasmOp.Op_f64_gt -> invoke(Op_f64_gt).also { context.stack(2, I32) }
                    WasmOp.Op_f64_ge -> invoke(Op_f64_ge).also { context.stack(2, I32) }

                    WasmOp.Op_select -> {
                        val selectType = context.typeStack[context.typeStack.size - 2]
                        val func = when (selectType) {
                            VOID -> WasmRuntime::Op_selectI
                            I32 -> WasmRuntime::Op_selectI
                            I64 -> WasmRuntime::Op_selectL
                            F32 -> WasmRuntime::Op_selectF
                            F64 -> WasmRuntime::Op_selectD
                            V128 -> TODO("v128")
                            ANYREF -> TODO()
                            FUNCREF -> TODO()
                        }
                        invoke(WasmRunJVMJIT[func]).also { context.stack(3, selectType) }
                    }
                    WasmOp.Op_i64_extend8_s -> invoke(Op_i64_extend8_s).also { context.stack(1, I64) }
                    WasmOp.Op_i64_extend16_s -> invoke(Op_i64_extend16_s).also { context.stack(1, I64) }
                    WasmOp.Op_i64_extend32_s -> invoke(Op_i64_extend32_s).also { context.stack(1, I64) }
                    WasmOp.Op_i64_extend_i32_s -> invoke(Op_i64_extend_i32_s).also { context.stack(1, I64) }
                    WasmOp.Op_i64_extend_i32_u -> invoke(Op_i64_extend_i32_u).also { context.stack(1, I64) }
                    WasmOp.Op_i32_extend8_s -> invoke(Op_i32_extend8_s).also { context.stack(1, I32) }
                    WasmOp.Op_i32_extend16_s -> invoke(Op_i32_extend16_s).also { context.stack(1, I32) }
                    WasmOp.Op_i32_wrap_i64 -> invoke(Op_i32_wrap_i64).also { context.stack(1, I32) }

                    WasmOp.Op_i32_reinterpret_f32 -> invoke(Op_i32_reinterpret_f32).also { context.stack(1, I32) }
                    WasmOp.Op_f32_reinterpret_i32 -> invoke(Op_f32_reinterpret_i32).also { context.stack(1, F32) }
                    WasmOp.Op_i64_reinterpret_f64 -> invoke(Op_i64_reinterpret_f64).also { context.stack(1, I64) }
                    WasmOp.Op_f64_reinterpret_i64 -> invoke(Op_f64_reinterpret_i64).also { context.stack(1, F64) }

                    WasmOp.Op_f32_convert_i32_s -> invoke(Op_f32_convert_s_i32).also { context.stack(1, F32) }
                    WasmOp.Op_f32_convert_i32_u -> invoke(Op_f32_convert_u_i32).also { context.stack(1, F32) }
                    WasmOp.Op_f32_convert_i64_s -> invoke(Op_f32_convert_s_i64).also { context.stack(1, F32) }
                    WasmOp.Op_f32_convert_i64_u -> invoke(Op_f32_convert_u_i64).also { context.stack(1, F32) }
                    WasmOp.Op_f32_demote_f64 -> invoke(Op_f32_demote_f64).also { context.stack(1, F32) }
                    WasmOp.Op_f64_convert_i32_s -> invoke(Op_f64_convert_s_i32).also { context.stack(1, F64) }
                    WasmOp.Op_f64_convert_i32_u -> invoke(Op_f64_convert_u_i32).also { context.stack(1, F64) }
                    WasmOp.Op_f64_convert_i64_s -> invoke(Op_f64_convert_s_i64).also { context.stack(1, F64) }
                    WasmOp.Op_f64_convert_i64_u -> invoke(Op_f64_convert_u_i64).also { context.stack(1, F64) }
                    WasmOp.Op_f64_promote_f32 -> invoke(Op_f64_promote_f32).also { context.stack(1, F64) }

                    WasmOp.Op_i32_trunc_f32_u -> invoke(Op_i32_trunc_u_f32).also { context.stack(1, I32) }
                    WasmOp.Op_i32_trunc_f32_s -> invoke(Op_i32_trunc_s_f32).also { context.stack(1, I32) }
                    WasmOp.Op_i32_trunc_f64_u -> invoke(Op_i32_trunc_u_f64).also { context.stack(1, I32) }
                    WasmOp.Op_i32_trunc_f64_s -> invoke(Op_i32_trunc_s_f64).also { context.stack(1, I32) }
                    WasmOp.Op_i32_trunc_sat_f32_u -> invoke(Op_i32_trunc_sat_f32_u).also { context.stack(1, I32) }
                    WasmOp.Op_i32_trunc_sat_f32_s -> invoke(Op_i32_trunc_sat_f32_s).also { context.stack(1, I32) }
                    WasmOp.Op_i32_trunc_sat_f64_u -> invoke(Op_i32_trunc_sat_f64_u).also { context.stack(1, I32) }
                    WasmOp.Op_i32_trunc_sat_f64_s -> invoke(Op_i32_trunc_sat_f64_s).also { context.stack(1, I32) }

                    WasmOp.Op_i64_trunc_f32_u -> invoke(Op_i64_trunc_u_f32).also { context.stack(1, I64) }
                    WasmOp.Op_i64_trunc_f32_s -> invoke(Op_i64_trunc_s_f32).also { context.stack(1, I64) }
                    WasmOp.Op_i64_trunc_f64_u -> invoke(Op_i64_trunc_u_f64).also { context.stack(1, I64) }
                    WasmOp.Op_i64_trunc_f64_s -> invoke(Op_i64_trunc_s_f64).also { context.stack(1, I64) }
                    WasmOp.Op_i64_trunc_sat_f32_u -> invoke(Op_i64_trunc_sat_f32_u).also { context.stack(1, I64) }
                    WasmOp.Op_i64_trunc_sat_f32_s -> invoke(Op_i64_trunc_sat_f32_s).also { context.stack(1, I64) }
                    WasmOp.Op_i64_trunc_sat_f64_u -> invoke(Op_i64_trunc_sat_f64_u).also { context.stack(1, I64) }
                    WasmOp.Op_i64_trunc_sat_f64_s -> invoke(Op_i64_trunc_sat_f64_s).also { context.stack(1, I64) }


                    WasmOp.Op_drop -> {
                        context.drop(context.typeStack.lastOrNull() ?: ANYREF)
                        context.stack(1)
                    }
                    else -> TODO("$i")
                }
            }
            is WasmInstruction.InsConstInt -> constant(i.value).also { context.stack(0, I32) }
            is WasmInstruction.InsConstLong -> constant(i.value).also { context.stack(0, I64) }
            is WasmInstruction.InsConstFloat -> constant(i.value).also { context.stack(0, F32) }
            is WasmInstruction.InsConstDouble -> constant(i.value).also { context.stack(0, F64) }
            is WasmInstruction.InsInt -> {
                when (i.op) {
                    WasmOp.Op_i32_const -> constant(i.param).also { context.stack(0, I32) } // @TODO: This shouldn't happen
                    WasmOp.Op_local_set, WasmOp.Op_local_get, WasmOp.Op_local_tee -> {
                        val type = context.localType(i.param)
                        val jvmLocalIndex = i.param
                        when (i.op) {
                            WasmOp.Op_local_set -> {
                                context.store(jvmLocalIndex, type)
                                context.stack(1)
                            }
                            WasmOp.Op_local_get -> {
                                context.load(jvmLocalIndex, type)
                                context.stack(0, type)
                            }
                            WasmOp.Op_local_tee -> {
                                context.store(jvmLocalIndex, type) // @TODO: idup?
                                context.load(jvmLocalIndex, type)
                                context.stack(0)
                            }
                            else -> TODO("$i")
                        }
                    }
                    WasmOp.Op_global_set, WasmOp.Op_global_get -> {
                        val global = context.global(i.param)
                        val type = context.globalType(i.param)
                        when (i.op) {
                            WasmOp.Op_global_set -> {
                                val lastType = context.typeStack.lastOrNull()
                                when (lastType) {
                                    VOID, null -> TODO("$i")
                                    I32, F32 -> {
                                        context.load(context.runtimeLocalIndex, VOID)
                                        visitInsn(Opcodes.SWAP)
                                    }
                                    I64, F64 -> {
                                        context.store(context.longTempIndex, lastType)
                                        context.load(context.runtimeLocalIndex, VOID)
                                        context.load(context.longTempIndex, lastType)
                                    }
                                    V128 -> TODO("v128")
                                    ANYREF -> TODO()
                                    FUNCREF -> TODO()
                                }

                                setWasmGlobal(global)
                                context.stack(1)
                            }
                            WasmOp.Op_global_get -> {
                                context.load(context.runtimeLocalIndex, VOID)
                                getWasmGlobal(global)
                                //load(jvmLocalIndex, type)
                                context.stack(0, type)
                            }
                            else -> TODO("$i")
                        }
                    }
                    WasmOp.Op_memory_copy -> {
                        aloadRuntime()
                        invoke(Op_memory_copy)
                        context.stack(3)
                    }
                    WasmOp.Op_memory_fill -> {
                        aloadRuntime()
                        invoke(Op_memory_fill)
                        context.stack(3)
                    }
                    WasmOp.Op_memory_size -> {
                        //i.param
                        aloadRuntime()
                        invoke(Op_memory_size)
                        context.stack(0, I32)
                    }
                    WasmOp.Op_memory_grow -> {
                        aloadRuntime()
                        invoke(Op_memory_grow)
                        context.stack(1, I32)
                    }
                    else -> TODO("$i")
                }
            }
            is WasmInstruction.InsMemarg -> {
                constant(i.offset)
                context.aload(context.runtimeLocalIndex)
                when (i.op) {
                    WasmOp.Op_i32_load -> invoke(Op_i32_load).also { context.stack(1, I32) }
                    WasmOp.Op_i32_load8_s -> invoke(Op_i32_load8_s).also { context.stack(1, I32) }
                    WasmOp.Op_i32_load8_u -> invoke(Op_i32_load8_u).also { context.stack(1, I32) }
                    WasmOp.Op_i32_load16_s -> invoke(Op_i32_load16_s).also { context.stack(1, I32) }
                    WasmOp.Op_i32_load16_u -> invoke(Op_i32_load16_u).also { context.stack(1, I32) }

                    WasmOp.Op_i64_load -> invoke(Op_i64_load).also { context.stack(1, I64) }
                    WasmOp.Op_i64_load8_s -> invoke(Op_i64_load8_s).also { context.stack(1, I64) }
                    WasmOp.Op_i64_load8_u -> invoke(Op_i64_load8_u).also { context.stack(1, I64) }
                    WasmOp.Op_i64_load16_s -> invoke(Op_i64_load16_s).also { context.stack(1, I64) }
                    WasmOp.Op_i64_load16_u -> invoke(Op_i64_load16_u).also { context.stack(1, I64) }
                    WasmOp.Op_i64_load32_s -> invoke(Op_i64_load32_s).also { context.stack(1, I64) }
                    WasmOp.Op_i64_load32_u -> invoke(Op_i64_load32_u).also { context.stack(1, I64) }

                    WasmOp.Op_f32_load -> invoke(Op_f32_load).also { context.stack(1, F32) }
                    WasmOp.Op_f64_load -> invoke(Op_f64_load).also { context.stack(1, F64) }

                    WasmOp.Op_i32_store -> invoke(Op_i32_store).also { context.stack(2) }
                    WasmOp.Op_i32_store8 -> invoke(Op_i32_store8).also { context.stack(2) }
                    WasmOp.Op_i32_store16 -> invoke(Op_i32_store16).also { context.stack(2) }
                    WasmOp.Op_i64_store -> invoke(Op_i64_store).also { context.stack(2) }
                    WasmOp.Op_i64_store8 -> invoke(Op_i64_store8).also { context.stack(2) }
                    WasmOp.Op_i64_store16 -> invoke(Op_i64_store16).also { context.stack(2) }
                    WasmOp.Op_i64_store32 -> invoke(Op_i64_store32).also { context.stack(2) }

                    WasmOp.Op_f32_store -> invoke(Op_f32_store).also { context.stack(2) }
                    WasmOp.Op_f64_store -> invoke(Op_f64_store).also { context.stack(2) }

                    else -> TODO("$i")
                }
            }
            WasmInstruction.RETURN -> {
                val lastStackType = context.typeStack.lastOrNull()
                val expectedRetType = context.func.type.retType.toWasmSType()
                if (expectedRetType == VOID) {
                    for (type in context.typeStack.reversed()) {
                        context.drop(type)
                    }
                    context.typeStack.clear()
                    context.ret(VOID)
                } else {
                    check(lastStackType == expectedRetType) { "lastStackType=$lastStackType != expectedRetType=$expectedRetType"}
                    context.stack(1)
                    context.ret(lastStackType)
                }
            }
            is WasmInstruction.br_base -> {
                val controlStructure = context.getControlByRelativeIndex(i.label)
                if (i is WasmInstruction.br_if) {
                    context.stack(1)
                }
                if (trace) println("${"  ".repeat(indent)}!! $i: context.controlStack.size=${context.controlStack.size}, label=${i.label}, controlStructure=$controlStructure, stack=${context.typeStack}")
                val continueLabel = Label()
                if (i is WasmInstruction.br_if) {
                    visitJumpInsn(Opcodes.IFEQ, continueLabel)
                }

                //visitJumpInsn(Opcodes.IF_ICMPEQ, controlStructure.label)
                context.prepareToJump(controlStructure, i !is WasmInstruction.br_if)
                visitJumpInsn(Opcodes.GOTO, controlStructure.label)

                if (i is WasmInstruction.br_if) {
                    visitLabel(continueLabel)
                } else {
                    context.typeStack.clear()
                }
            }
            is WasmInstruction.br_table -> {
                context.stack(1)
                val defaultControlStructure = context.getControlByRelativeIndex(i.default)
                //context.prepareToJump(defaultControlStructure)
                visitTableSwitchInsn(
                    0, i.labels.size - 1,
                    defaultControlStructure.label,
                    *(i.labels).map { context.getControlByRelativeIndex(it).label }.toTypedArray()
                )
                context.typeStack.clear()
            }
            is WasmInstruction.BlockOrLoop -> {
                //val loop = i.itype
                //println("!!!!!!!! BLOCK TYPE BLOCK: ${i.b}")
                val stack = context.saveStack()
                val isBlock = i is WasmInstruction.block
                val cs = ControlStructure(if (isBlock) "block" else "loop", stack, i.b)
                context.pushControlStructure(cs) {
                    if (!isBlock) {
                        visitLabel(cs.label)
                        if (trace) println("${"  ".repeat(indent)}LOOP label=${cs.label}, input=${context.typeStack}, output=${i.b}")
                    }
                    generateExpr(i.expr, context, indent + 1)
                    if (isBlock) {
                        visitLabel(cs.label)
                        if (trace) println("${"  ".repeat(indent)}BLOCK label=${cs.label}, input=${context.typeStack}, output=${i.b}")
                    }
                }
                context.restoreStack(stack, i.b.toWasmSType())
            }
            WasmInstruction.nop -> {
                visitInsn(Opcodes.NOP)
            }
            WasmInstruction.unreachable -> {
                constant(context.name)
                invoke(create_unreachable_exception_instance)
                visitInsn(Opcodes.ATHROW)
                context.typeStack.clear()
            }
            //is WasmInstruction.br_if_with_op -> TODO("$i")
            is WasmInstruction.INVOKE -> TODO()
            is WasmInstruction.InsType -> TODO()
        }
    }

    fun MethodVisitor.generateExpr(expr: WasmExpr, context: GenMethodContext, indent: Int, implicitReturn: Boolean = true) {
        val instructions = expr.instructions
        //println(instructions.joinToString("\n") { " - $it" })
        for ((index, instruction) in instructions.withIndex()) {
            val nextI = instructions.getOrNull(index + 1)
            //if (instruction.op.kind == WasmOp.Kind.BINOP_COMP && nextI is WasmInstruction.IF) {
            //    nextI.compOp = instruction.op
            //    continue
            //}
            generateInstruction(instruction, context, indent)
            if (instruction is WasmInstruction.RETURN) return
            if (instruction is WasmInstruction.br) return
            if (instruction is WasmInstruction.br_table) return
            if (instruction is WasmInstruction.unreachable) return
        }

        if (implicitReturn && indent == 0 && instructions.lastOrNull() !is WasmInstruction.RETURN) {
            context.ret(context.typeStack.lastOrNull() ?: VOID)
        }
    }

    val WasmGlobal.functionType: WasmType.Function get() = WasmType.Function(listOf(), listOf(globalType))
    val WasmGlobal.getterGlobalName: String get() = "gen\$${name}"

    data class ElementItem(val func: WasmFunc, val funcName: String)
    data class ElementInfo(val tableIndex: Int, val index: Int, val elements: List<ElementItem>)

    fun generate(module: WasmModule): WasmRunJVMJIT {
        val interpreter = WasmRunInterpreter(module)

        val elements = arrayListOf<ElementInfo>()

        var usedClassMemory = 0
        val clazz = createClass(OUTPUT_CLASS_NAME, WasmRunJVMJIT::class.java, doTrace = trace, doValidate = validate, handleBytes = {
            usedClassMemory += it.size
        }) {
            for (global in module.globals) {
                val globalType = global.globalType
                visitField(Opcodes.ACC_PUBLIC, global.name, globalType.toJDescriptor(), null, globalType.toNullValue())
                //if (global.expr != null) {
                //    createMethod(global.getterGlobalName, globalType.toJava(), "L${OUTPUT_CLASS_NAME};", isStatic = true) {
                //        generateFunc(WasmFunc(-1, global.functionType, WasmCode(listOf(), global.expr)), module)
                //    }
                //}
            }

            createConstructor(WasmModule::class.java) {
                _aload(0)
                val memory = module.memories.firstOrNull() ?: WasmType.Limit(0, null)
                _aload(1)
                constant(memory.min)
                constant(memory.max ?: 0x10000)
                constructor(WasmRunJVMJIT::class.java.getDeclaredConstructor(WasmModule::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType))
                //for (global in module.globals) {
                //    if (global.expr != null) {
                //        _aload(0)
                //        visitMethodInsn(
                //            Opcodes.INVOKESTATIC,
                //            OUTPUT_CLASS_NAME, global.getterGlobalName, global.functionType.toJDescriptor(), false
                //        )
                //        this.setWasmGlobal(global)
                //    }
                //}
                for ((index, element) in module.elements.withIndex()) {
                    if (element.expr == null) continue
                    interpreter.eval(element.expr, WasmType.Function(listOf(), listOf(I32)), WasmDebugContext("elem", index))
                    val index = interpreter.popI32()
                    // Index
                    generateExpr(element.expr, GenMethodContext("element${index}", this@createConstructor, WasmFunc(-1, WasmType.Function(listOf(), listOf())), module), 0, implicitReturn = false)

                    elements += ElementInfo(element.tableIdx, index, element.getFunctions(module).map { ElementItem(it, it.getJvmName()) })
                }

                for (global in module.globals) {
                    if (trace) println("GLOBAL: $global")
                    if (global.expr == null) continue
                    //generateFunc(WasmFunc(-1, global.functionType, WasmCode(listOf(), global.expr)), module)
                    _aload(0) // runtime
                    generateExpr(global.expr, GenMethodContext("global${global.name}", this@createConstructor, WasmFunc(-1, WasmType.Function(listOf(), listOf(global.functionType))), module), 0, implicitReturn = false)
                    setWasmGlobal(global)
                }

                val MAX_CHUNK_SIZE = (32 * 1024)
                for (data in module.datas) {
                    if (trace) println("DATA: $data")
                    // Address
                    generateExpr(data.e!!, GenMethodContext("data${data.index}", this@createConstructor, WasmFunc(-1, WasmType.Function(listOf(), listOf(WasmType.i32))), module), 0, implicitReturn = false)

                    val nchunks = data.data.size divCeil MAX_CHUNK_SIZE
                    repeat(nchunks - 1) { visitInsn(Opcodes.DUP) }
                    for (n in 0 until nchunks) {
                        val offset = n * MAX_CHUNK_SIZE
                        constant(offset)
                        iadd()

                        val chunk = data.data.sliceArray(offset until minOf(data.data.size, offset + MAX_CHUNK_SIZE))

                        //println("offset=$offset, data.data=${chunk.size}")
                        constant(chunk.toBinaryString())
                        constant(chunk.size) // bytes
                        _aload(0) // runtime
                        invoke(WasmRunJVMJIT[WasmRuntime::set_memory])
                    }
                }

                ret()
            }

            for (ntype in module.types) {
                val type = ntype.type as WasmType.Function
                val functionType = WasmType.Function((type.args.map { it.type } + I32).withIndex().map { (index, it) -> WastLocal(index, it) }, listOf(type.retType))
                createMethod("call\$indirect", type.retType.toJava(), *type.args.map { it.type.toJava() }.toTypedArray(), Int::class.javaPrimitiveType, "LWasmProgram;", isStatic = true) {
                    //ret(type.retType.toWasmSType())
                    val indexIndex = type.args.size
                    val runtimeIndex = type.args.size + 1
                    val context = GenMethodContext("call\$indirect", this, WasmFunc(-1, functionType), module)

                    // Create array
                    constant(type.args.size)
                    visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object")
                    context.astore(context.longTempIndex)

                    //for (n in 0 until type.args.size) {
                    for (n in 0 until type.args.size) {
                        val stype = type.args[n].stype
                        // Array
                        context.aload(context.longTempIndex)
                        // Index
                        constant(n)
                        // Boxed Value
                        context.load(n, stype); boxType(stype)
                        //visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Object")
                        visitInsn(Opcodes.AASTORE)
                    }

                    //for ((indexInTable, funcRef) in funcRefs) {
                    //    val func = funcRef?.func ?: continue
                    //    context.iload(indexIndex)
                    //    constant(indexInTable)
                    //    IF(Opcodes.IF_ICMPEQ) {
                    //        visitMethodInsn(
                    //            Opcodes.INVOKESTATIC,
                    //            OUTPUT_CLASS_NAME, func.getJvmName(), func.type.getJvmDescriptor(OUTPUT_CLASS_NAME), false
                    //        )
                    //        ret(func.type.retType.toWasmSType())
                    //    }
                    //}
                    //val indices = funcRefs.map { it.index }
                    //println("GENERATE CALL_INDIRECT: $type : indices=$indices : funcs=${funcRefs.map { it.value.name }}")

                    constant(0) // Table index
                    context.iload(indexIndex)
                    context.aload(context.longTempIndex)
                    context.aload(runtimeIndex)
                    //constant("type=$type, indices=${indices}")
                    //constant("type=$type")
                    invoke(WasmRunJVMJIT[WasmRuntime::call_indirect])
                    unboxType(functionType.retType.toWasmSType())
                    ret(functionType.retType.toWasmSType())

                    //visitInsn(Opcodes.ATHROW)
                }
            }
            //for (table in (module.tables.filter { it.clazz == WasmFuncRef::class } as List<WasmType.TableType<WasmFuncRef>>)) {
            //    for ((type, funcRefs) in table.items.withIndex().groupBy { it.value?.func?.type }) {
            //        if (type == null) continue
            //    }
            //    //module.types
            //    //for (item in table.items) {
            //    //}
            //}

            for (func in module.functions) {
                val sretType = func.type.retType.toWasmSType()
                val retType = sretType.toJava()
                val argTypes = func.type.args.map { it.type.toJava() }.toTypedArray()
                val import = func.fimport
                val methodName = func.getJvmName()
                if (import != null) {
                    if (trace) println("FUNC<import>: $methodName : ${func.type}")
                    createMethod(methodName, retType, *argTypes, "LWasmProgram;", isStatic = true) {
                        val context = GenMethodContext(methodName, this, func, module)
                        val arrayLocalIndex = context.addLocal(null, GenMethodContext.LocalKind.TEMP)
                        val runtimeIndex = argTypes.size
                        //val arrayLocalIndex = argTypes.size + 1
                        context.aload(runtimeIndex) // Load WasmProgram

                        constant(argTypes.size)
                        visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object")
                        context.astore(arrayLocalIndex)

                        for ((index, type) in func.type.args.withIndex()) {
                            // Array
                            context.aload(arrayLocalIndex)

                            // Index
                            constant(index)

                            // Boxed Value
                            context.load(index, type.stype)
                            boxType(type.stype)

                            // Store Array[index] = BoxedInteger
                            visitInsn(Opcodes.AASTORE)
                        }

                        // Array with params
                        constant(import.moduleName)
                        constant(import.name)
                        context.aload(arrayLocalIndex)
                        invoke(WasmRunJVMJIT[WasmRunJVMJIT::invokeImport])
                        // Boxed value
                        when (sretType) {
                            VOID -> Unit
                            I32 -> {
                                visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer")
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false)
                            }
                            I64 -> {
                                visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long")
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false)
                            }
                            F32 -> {
                                visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float")
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "longValue", "()F", false)
                            }
                            F64 -> {
                                visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double")
                                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false)
                            }
                            V128 -> TODO("v128")
                            ANYREF -> TODO()
                            FUNCREF -> TODO()
                        }
                        ret(sretType)
                    }
                } else {
                    //createMethod(func.export?.name ?: func.name, func.type.retType.toJava(), *argTypes, WasmJIT::class.java, isStatic = true) {
                    if (trace) println("FUNC<code>: $methodName : ${func.type}")
                    //println("FUNC[${func.index}]: $methodName : ${func.type}")
                    createMethod(methodName, retType, *argTypes, "LWasmProgram;", isStatic = true) {
                        generateFunc(func, module)
                    }
                }
            }

            //if (module.asserts.isNotEmpty()) {
            if (true) {
                createMethod("run\$asserts", Void::class.javaPrimitiveType, "LWasmProgram;", isStatic = true, maxLocals = 3) {
                    if (module.asserts.isNotEmpty()) {
                        constant(0); visitVarInsn(Opcodes.ISTORE, 1)
                        constant(0); visitVarInsn(Opcodes.ISTORE, 2)
                        for (assert in module.asserts) {
                            when (assert) {
                                is WasmAssertReturn -> {
                                    // @TODO: Check i32, etc.
                                    val genMethodContext = GenMethodContext(
                                        "run\$asserts",
                                        this,
                                        WasmFunc(-1, WasmType.Function(listOf(), listOf())),
                                        module
                                    )
                                    generateExpr(assert.actual, genMethodContext, indent = 0, implicitReturn = false)
                                    val returnType = genMethodContext.typeStack.lastOrNull()
                                    if (assert.expect != null) {
                                        generateExpr(
                                            assert.expect!!,
                                            genMethodContext,
                                            indent = 0,
                                            implicitReturn = false
                                        )
                                    } else {
                                        check(returnType == null)
                                    }
                                    constant(assert.msg)
                                    when (returnType) {
                                        null, VOID -> invoke(WasmRunJVMJIT[WasmRuntime::assert_return_void])
                                        I32 -> invoke(WasmRunJVMJIT[WasmRuntime::assert_return_i32])
                                        I64 -> invoke(WasmRunJVMJIT[WasmRuntime::assert_return_i64])
                                        F32 -> invoke(WasmRunJVMJIT[WasmRuntime::assert_return_f32])
                                        F64 -> invoke(WasmRunJVMJIT[WasmRuntime::assert_return_f64])
                                        V128 -> TODO("v128")
                                        ANYREF -> TODO()
                                        FUNCREF -> TODO()
                                    }

                                    visitVarInsn(Opcodes.ILOAD, 1)
                                    visitInsn(Opcodes.IADD)
                                    visitVarInsn(Opcodes.ISTORE, 1)

                                    visitIincInsn(2, 1)
                                }

                                else -> Unit
                            }
                        }
                        visitVarInsn(Opcodes.ILOAD, 1)
                        visitVarInsn(Opcodes.ILOAD, 2)
                        invoke(WasmRunJVMJIT[WasmRuntime::assert_summary])
                    }
                    ret()
                }
            }
        }

        val instance = clazz.getDeclaredConstructor(WasmModule::class.java).newInstance(module)
        //println(clazz.declaredMethods.toList())
        //println(clazz.declaredMethods.firstOrNull { it.name == "fib" }!!.invoke(null, 16))
        return (instance as WasmRunJVMJIT).also {
            it.setElements(elements)
            it.usedClassMemory = usedClassMemory
        }
    }

    fun WasmFunc.getJvmName(): String {
        val func = this
        val import = func.fimport
        if (import != null) {
            return "${import.moduleName}\$${import.name}"
        } else {
            return (func.exportName ?: func.name).replace(".", "\$")
        }
    }

    fun WasmType.Function.getJvmDescriptor(vararg extra: Any): String {
        val args = this.args.map { Type.getType(it.type.toJava()) } + extra.map { if (it is Class<*>) Type.getType(it) else Type.getObjectType("$it") }
        return Type.getMethodDescriptor(Type.getType(this.retType.toJava()), *args.toTypedArray())
    }

    fun MethodVisitor.getWasmGlobal(global: WasmGlobal) {
        visitFieldInsn(
            Opcodes.GETFIELD,
            OUTPUT_CLASS_NAME,
            global.name,
            global.globalType.toJDescriptor()
        )
    }
    fun MethodVisitor.setWasmGlobal(global: WasmGlobal) {
        visitFieldInsn(
            Opcodes.PUTFIELD,
            OUTPUT_CLASS_NAME,
            global.name,
            global.globalType.toJDescriptor()
        )
    }

    fun MethodVisitor.boxType(stype: WasmSType) {
        when (stype) {
            VOID -> TODO()
            I32 -> visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
            I64 -> visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
            F32 -> visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false)
            F64 -> visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
            V128 -> TODO("v128")
            ANYREF -> TODO()
            FUNCREF -> TODO()
        }
    }

    fun MethodVisitor.unboxType(stype: WasmSType) {
        when (stype) {
            VOID -> visitInsn(Opcodes.POP)
            I32 -> { visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer"); visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false) }
            I64 -> { visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long"); visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false) }
            F32 -> { visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float"); visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false) }
            F64 -> { visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double"); visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false) }
            V128 -> TODO("v128")
            ANYREF -> TODO()
            FUNCREF -> TODO()
        }
    }

    fun WasmType.toJava(): Class<*> {
        return when (this) {
            VOID -> Void::class.javaPrimitiveType!!
            I32 -> Int::class.javaPrimitiveType!!
            I64 -> Long::class.javaPrimitiveType!!
            F32 -> Float::class.javaPrimitiveType!!
            F64 -> Double::class.javaPrimitiveType!!
            V128 -> TODO("v128")
            is WasmType.Function -> TODO()
            //is WasmType.Global -> TODO()
            is WasmType.Limit -> TODO()
            is WasmType._ARRAY -> TODO()
            is WasmType._NULLABLE -> TODO()
            is WasmType._VARARG -> TODO()
            WasmType._boolean -> Boolean::class.javaPrimitiveType!!
            WasmType._i16 -> Short::class.javaPrimitiveType!!
            WasmType._i8 -> Byte::class.javaPrimitiveType!!
            WasmType.v128 -> TODO()
            is CustomWasmType -> TODO()
            ANYREF -> TODO()
            FUNCREF -> TODO()
            is WasmType.Mutable -> this.rtype.toJava()
        }
    }

    open fun getClassVisitor(cw: ClassWriter, doValidate: Boolean): ClassVisitor {
        //return if (doValidate) CheckClassAdapter(cw) else cw
        return cw
    }

    inline fun createClass(
        name: String,
        parent: Class<*> = java.lang.Object::class.java,
        vararg interfaces: Class<*>,
        doTrace: Boolean,
        doValidate: Boolean,
        handleBytes: (ByteArray) -> Unit = { },
        block: ClassVisitor.() -> Unit
    ): Class<*> {
        //val cv = TraceClassVisitor(null, ASMifier(), PrintWriter(System.out))
        val cw = ClassWriter(ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)

        val cr: ClassVisitor = getClassVisitor(cw, doValidate)
        //val cw = if (doTrace) TraceClassVisitor(cw2, ASMifier(), PrintWriter(System.out)) else cw2
        //MethodVisitor
        //val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)
        cr.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, Type.getInternalName(parent), interfaces.map { Type.getInternalName(it) }.toTypedArray())
        block(cr)
        cr.visitEnd()
        val bytes = cw.toByteArray()

        // DUMP bytecode
        if (doTrace) dumpJVMBytecode(bytes)

        handleBytes(bytes)

        val classLoader = ByteArrayClassLoader(bytes, name)
        val clazz = classLoader.loadClass(name)
        return clazz
    }

    open fun dumpJVMBytecode(bytes: ByteArray) {
    }

    inline fun ClassVisitor.createConstructor(vararg params: Class<*>?, crossinline block: MethodVisitor.() -> Unit) {
        createMethod("<init>", Void::class.javaPrimitiveType, *params, isStatic = false) {
            block()
        }
    }

    inline fun ClassVisitor.createMethod(name: String, ret: Class<*>?, vararg params: Any?, isStatic: Boolean = false, maxStack: Int = 0, maxLocals: Int = 0, crossinline block: MethodVisitor.() -> Unit) {
        try {
            if (name.contains(".")) error("Invalid method name '$name'")
            val myMethod: MethodVisitor = visitMethod(
                Opcodes.ACC_PUBLIC or (if (isStatic) Opcodes.ACC_STATIC else 0),
                name,
                Type.getMethodDescriptor(Type.getType(ret), *params.map {
                    if (it is Class<*>) Type.getType(it) else Type.getType(it.toString())
                }.toTypedArray()),
                null, null
            )
            val doTrace = false
            //val doTrace = true
            useMethodVisitor(myMethod, doTrace) { mv ->
                mv.visitCode()
                block(mv)
                mv.visitMaxs(maxStack, maxLocals)
                mv.visitEnd()
            }
        } catch (e: Throwable) {
            throw Exception("Error generating '$name' method", e)
        }
    }

    open fun useMethodVisitor(myMethod: MethodVisitor, doTrace: Boolean, block: (mv: MethodVisitor) -> Unit) {
        block(myMethod)
    }

    companion object {
        fun Member.isStatic(): Boolean = Modifier.isStatic(modifiers)

        fun MethodVisitor.ret(type: WasmSType?) {
            when (type) {
                null, ANYREF, FUNCREF -> aret()
                VOID -> ret()
                I32 -> iret()
                I64 -> lret()
                F32 -> fret()
                F64 -> dret()
                V128 -> TODO("v128")
            }
        }

        fun MethodVisitor.ret() = visitInsn(Opcodes.RETURN)
        fun MethodVisitor.aret() = visitInsn(Opcodes.ARETURN)
        fun MethodVisitor.iret() = visitInsn(Opcodes.IRETURN)
        fun MethodVisitor.lret() = visitInsn(Opcodes.LRETURN)
        fun MethodVisitor.fret() = visitInsn(Opcodes.FRETURN)
        fun MethodVisitor.dret() = visitInsn(Opcodes.DRETURN)

        fun MethodVisitor.field(field: Field) {
            visitFieldInsn(
                if (field.isStatic()) Opcodes.GETSTATIC else Opcodes.GETFIELD,
                field.declaringClass.internalName(),
                field.name,
                field.type.descriptor()
            )
        }

        fun MethodVisitor.constant(value: Any) {
            when {
                value is Int && value in Byte.MIN_VALUE..Byte.MAX_VALUE -> visitIntInsn(Opcodes.BIPUSH, value)
                value is Int && value in Short.MIN_VALUE..Short.MAX_VALUE -> visitIntInsn(Opcodes.SIPUSH, value)
                value is String -> {
                    if (value.length >= 32768) TODO("String constant too long: ${value.length}")
                    visitLdcInsn(value)
                }
                else -> {
                    visitLdcInsn(value)
                }
            }
        }

        inline fun MethodVisitor.IF(op: Int, endLabel: Label = Label(), block: () -> Unit) {
            visitJumpInsn(negateCompOpcode(op), endLabel)
            block()
            // if (trace) println("IF: visitLabel(end) - $end")
            visitLabel(endLabel)
        }

        inline fun MethodVisitor.IF_ELSE(op: Int, btrue: () -> Unit, bfalse: () -> Unit = {}, endLabel: Label = Label(), unit: Unit = Unit) {
            val belse = Label()
            val bend = endLabel
            visitJumpInsn(negateCompOpcode(op), belse)
            btrue()
            visitJumpInsn(Opcodes.GOTO, bend)
            visitLabel(belse)
            // if (trace) println("IF_ELSE: visitLabel(belse) - $belse")
            bfalse()
            visitLabel(bend)
            // if (trace) println("IF_ELSE: visitLabel(bend) - $bend")
        }

        fun MethodVisitor.invoke(method: Method, vararg args: Any, isInterface: Boolean = false) {
            for (arg in args) {
                when (arg) {
                    is Field -> field(arg)
                    else -> constant(arg)
                }
            }
            visitMethodInsn(
                if (method.isStatic()) Opcodes.INVOKESTATIC else Opcodes.INVOKEVIRTUAL,
                method.declaringClass.internalName(), method.name, Type.getMethodDescriptor(method), isInterface
            )
        }

        fun MethodVisitor.constructor(const: Constructor<out Any>) {
            visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                const.declaringClass.internalName(), "<init>", Type.getConstructorDescriptor(const), false
            )
        }

        fun Class<out Any>.internalName(): String = Type.getInternalName(this)
        fun Class<out Any>.descriptor(): String = Type.getDescriptor(this)

        fun MethodVisitor._aload(i: Int) = visitVarInsn(Opcodes.ALOAD, i)
        fun MethodVisitor._iload(i: Int) = visitVarInsn(Opcodes.ILOAD, i)
        fun MethodVisitor._lload(i: Int) = visitVarInsn(Opcodes.LLOAD, i)
        fun MethodVisitor._fload(i: Int) = visitVarInsn(Opcodes.FLOAD, i)
        fun MethodVisitor._dload(i: Int) = visitVarInsn(Opcodes.DLOAD, i)

        fun MethodVisitor._astore(i: Int) = visitVarInsn(Opcodes.ASTORE, i)
        fun MethodVisitor._istore(i: Int) = visitVarInsn(Opcodes.ISTORE, i)
        fun MethodVisitor._lstore(i: Int) = visitVarInsn(Opcodes.LSTORE, i)
        fun MethodVisitor._fstore(i: Int) = visitVarInsn(Opcodes.FSTORE, i)
        fun MethodVisitor._dstore(i: Int) = visitVarInsn(Opcodes.DSTORE, i)

        fun MethodVisitor.iadd() = visitInsn(Opcodes.IADD)
        fun MethodVisitor.isub() = visitInsn(Opcodes.ISUB)
        fun MethodVisitor.imul() = visitInsn(Opcodes.IMUL)
        fun MethodVisitor.idiv() = visitInsn(Opcodes.IDIV)
        fun MethodVisitor.irem() = visitInsn(Opcodes.IREM)
        fun MethodVisitor.iand() = visitInsn(Opcodes.IAND)
        fun MethodVisitor.ior() = visitInsn(Opcodes.IOR)
        fun MethodVisitor.ixor() = visitInsn(Opcodes.IXOR)
        fun MethodVisitor.ishl() = visitInsn(Opcodes.ISHL)
        fun MethodVisitor.iushr() = visitInsn(Opcodes.IUSHR)
        fun MethodVisitor.ishr() = visitInsn(Opcodes.ISHR)

        fun MethodVisitor.ladd() = visitInsn(Opcodes.LADD)
        fun MethodVisitor.lsub() = visitInsn(Opcodes.LSUB)
        fun MethodVisitor.lmul() = visitInsn(Opcodes.LMUL)
        fun MethodVisitor.ldiv() = visitInsn(Opcodes.LDIV)
        fun MethodVisitor.lrem() = visitInsn(Opcodes.LREM)
        fun MethodVisitor.land() = visitInsn(Opcodes.LAND)
        fun MethodVisitor.lor() = visitInsn(Opcodes.LOR)
        fun MethodVisitor.lxor() = visitInsn(Opcodes.LXOR)
        fun MethodVisitor.lshl() = visitInsn(Opcodes.LSHL)
        fun MethodVisitor.lushr() = visitInsn(Opcodes.LUSHR)
        fun MethodVisitor.lshr() = visitInsn(Opcodes.LSHR)

        fun MethodVisitor.fadd() = visitInsn(Opcodes.FADD)
        fun MethodVisitor.fsub() = visitInsn(Opcodes.FSUB)
        fun MethodVisitor.fmul() = visitInsn(Opcodes.FMUL)
        fun MethodVisitor.fdiv() = visitInsn(Opcodes.FDIV)
        fun MethodVisitor.frem() = visitInsn(Opcodes.FREM)

        fun MethodVisitor.dadd() = visitInsn(Opcodes.DADD)
        fun MethodVisitor.dsub() = visitInsn(Opcodes.DSUB)
        fun MethodVisitor.dmul() = visitInsn(Opcodes.DMUL)
        fun MethodVisitor.ddiv() = visitInsn(Opcodes.DDIV)
        fun MethodVisitor.drem() = visitInsn(Opcodes.DREM)

        fun MethodVisitor.ifeq(label: Label) = visitJumpInsn(Opcodes.IFEQ, label)
        fun MethodVisitor.ifne(label: Label) = visitJumpInsn(Opcodes.IFNE, label)

        //fun Type.internalName(): String = Type.(this)

        class ByteArrayClassLoader(private val classBytes: ByteArray, val className: String) : SecureClassLoader() {
            @Throws(ClassNotFoundException::class)
            override fun findClass(name: String): Class<*> {
                if (this.className != name) throw ClassNotFoundException()
                return defineClass(name, classBytes, 0, classBytes.size)
            }
        }


        fun ByteArray.toBinaryString(): String {
            val out = CharArray(size divCeil 2)
            for (n in 0 until size step 2) {
                if (n + 1 >= size) {
                    out[n / 2] = this[n + 0].unsigned.toChar()
                } else {
                    val low = this[n + 0]
                    val high = this[n + 1]
                    out[n / 2] = (low.unsigned or (high.unsigned shl 8)).toChar()
                }
            }
            return String(out)
        }

        fun negateCompOpcode(opcode: Int): Int = when (opcode) {
            Opcodes.IFEQ -> Opcodes.IFNE
            Opcodes.IFNE -> Opcodes.IFEQ
            Opcodes.IFLT -> Opcodes.IFGE
            Opcodes.IFGE -> Opcodes.IFLT
            Opcodes.IFGT -> Opcodes.IFLE
            Opcodes.IFLE -> Opcodes.IFGT
            Opcodes.IF_ICMPEQ -> Opcodes.IF_ICMPNE
            Opcodes.IF_ICMPNE -> Opcodes.IF_ICMPEQ
            Opcodes.IF_ICMPLT -> Opcodes.IF_ICMPGE
            Opcodes.IF_ICMPGE -> Opcodes.IF_ICMPLT
            Opcodes.IF_ICMPGT -> Opcodes.IF_ICMPLE
            Opcodes.IF_ICMPLE -> Opcodes.IF_ICMPGT
            Opcodes.IF_ACMPEQ -> Opcodes.IF_ACMPNE
            Opcodes.IF_ACMPNE -> Opcodes.IF_ACMPEQ
            else -> error("Unsupported opcode $opcode")
        }

        operator fun get(func: KFunction<*>): Method {
            return declaredMethodsByName[func.name] ?: error("Can't find method ${func.name}")
        }

        val declaredMethodsByName by lazy {
            WasmRuntime::class.java.declaredMethods.associateBy { it.name }
        }

        val create_unreachable_exception_instance = this[WasmRuntime::create_unreachable_exception_instance]
        val Op_i32_load = this[WasmRuntime::Op_i32_load]
        val Op_i32_load8_s = this[WasmRuntime::Op_i32_load8_s]
        val Op_i32_load8_u = this[WasmRuntime::Op_i32_load8_u]
        val Op_i32_load16_s = this[WasmRuntime::Op_i32_load16_s]
        val Op_i32_load16_u = this[WasmRuntime::Op_i32_load16_u]
        val Op_i64_load = this[WasmRuntime::Op_i64_load]
        val Op_i64_load8_s = this[WasmRuntime::Op_i64_load8_s]
        val Op_i64_load8_u = this[WasmRuntime::Op_i64_load8_u]
        val Op_i64_load16_s = this[WasmRuntime::Op_i64_load16_s]
        val Op_i64_load16_u = this[WasmRuntime::Op_i64_load16_u]
        val Op_i64_load32_s = this[WasmRuntime::Op_i64_load32_s]
        val Op_i64_load32_u = this[WasmRuntime::Op_i64_load32_u]
        val Op_f32_load = this[WasmRuntime::Op_f32_load]
        val Op_f64_load = this[WasmRuntime::Op_f64_load]
        val Op_i32_store = this[WasmRuntime::Op_i32_store]
        val Op_i32_store8 = this[WasmRuntime::Op_i32_store8]
        val Op_i32_store16 = this[WasmRuntime::Op_i32_store16]
        val Op_i64_store = this[WasmRuntime::Op_i64_store]
        val Op_i64_store8 = this[WasmRuntime::Op_i64_store8]
        val Op_i64_store16 = this[WasmRuntime::Op_i64_store16]
        val Op_i64_store32 = this[WasmRuntime::Op_i64_store32]
        val Op_f32_store = this[WasmRuntime::Op_f32_store]
        val Op_f64_store = this[WasmRuntime::Op_f64_store]
        val Op_i32_eqz = this[WasmRuntime::Op_i32_eqz]
        val Op_i32_eq = this[WasmRuntime::Op_i32_eq]
        val Op_i32_ne = this[WasmRuntime::Op_i32_ne]
        val Op_i32_lt_s = this[WasmRuntime::Op_i32_lt_s]
        val Op_i32_le_s = this[WasmRuntime::Op_i32_le_s]
        val Op_i32_ge_s = this[WasmRuntime::Op_i32_ge_s]
        val Op_i32_gt_s = this[WasmRuntime::Op_i32_gt_s]
        val Op_i32_lt_u = this[WasmRuntime::Op_i32_lt_u]
        val Op_i32_le_u = this[WasmRuntime::Op_i32_le_u]
        val Op_i32_ge_u = this[WasmRuntime::Op_i32_ge_u]
        val Op_i32_gt_u = this[WasmRuntime::Op_i32_gt_u]
        val Op_i64_eqz = this[WasmRuntime::Op_i64_eqz]
        val Op_i64_eq = this[WasmRuntime::Op_i64_eq]
        val Op_i64_ne = this[WasmRuntime::Op_i64_ne]
        val Op_i64_lt_s = this[WasmRuntime::Op_i64_lt_s]
        val Op_i64_le_s = this[WasmRuntime::Op_i64_le_s]
        val Op_i64_ge_s = this[WasmRuntime::Op_i64_ge_s]
        val Op_i64_gt_s = this[WasmRuntime::Op_i64_gt_s]
        val Op_i64_lt_u = this[WasmRuntime::Op_i64_lt_u]
        val Op_i64_le_u = this[WasmRuntime::Op_i64_le_u]
        val Op_i64_ge_u = this[WasmRuntime::Op_i64_ge_u]
        val Op_i64_gt_u = this[WasmRuntime::Op_i64_gt_u]
        val Op_f32_le = this[WasmRuntime::Op_f32_le]
        val Op_f32_lt = this[WasmRuntime::Op_f32_lt]
        val Op_f32_eq = this[WasmRuntime::Op_f32_eq]
        val Op_f32_ne = this[WasmRuntime::Op_f32_ne]
        val Op_f32_gt = this[WasmRuntime::Op_f32_gt]
        val Op_f32_ge = this[WasmRuntime::Op_f32_ge]
        val Op_f64_le = this[WasmRuntime::Op_f64_le]
        val Op_f64_lt = this[WasmRuntime::Op_f64_lt]
        val Op_f64_eq = this[WasmRuntime::Op_f64_eq]
        val Op_f64_ne = this[WasmRuntime::Op_f64_ne]
        val Op_f64_gt = this[WasmRuntime::Op_f64_gt]
        val Op_f64_ge = this[WasmRuntime::Op_f64_ge]
        val Op_selectI = this[WasmRuntime::Op_selectI]
        val Op_selectL = this[WasmRuntime::Op_selectL]
        val Op_selectF = this[WasmRuntime::Op_selectF]
        val Op_selectD = this[WasmRuntime::Op_selectD]
        val Op_i32_div_u = this[WasmRuntime::Op_i32_div_u]
        val Op_i32_rem_u = this[WasmRuntime::Op_i32_rem_u]
        val Op_i32_clz = this[WasmRuntime::Op_i32_clz]
        val Op_i32_ctz = this[WasmRuntime::Op_i32_ctz]
        val Op_i32_popcnt = this[WasmRuntime::Op_i32_popcnt]
        val Op_i64_popcnt = this[WasmRuntime::Op_i64_popcnt]
        val Op_i32_rotl = this[WasmRuntime::Op_i32_rotl]
        val Op_i32_rotr = this[WasmRuntime::Op_i32_rotr]
        val Op_i64_div_u = this[WasmRuntime::Op_i64_div_u]
        val Op_i64_rem_u = this[WasmRuntime::Op_i64_rem_u]
        val Op_i64_clz = this[WasmRuntime::Op_i64_clz]
        val Op_i64_ctz = this[WasmRuntime::Op_i64_ctz]
        val Op_i64_rotl = this[WasmRuntime::Op_i64_rotl]
        val Op_i64_rotr = this[WasmRuntime::Op_i64_rotr]
        val Op_i64_extend8_s = this[WasmRuntime::Op_i64_extend8_s]
        val Op_i64_extend16_s = this[WasmRuntime::Op_i64_extend16_s]
        val Op_i64_extend32_s = this[WasmRuntime::Op_i64_extend32_s]
        val Op_i64_extend_i32_u = this[WasmRuntime::Op_i64_extend_i32_u]
        val Op_i64_extend_i32_s = this[WasmRuntime::Op_i64_extend_i32_s]
        val Op_i32_wrap_i64 = this[WasmRuntime::Op_i32_wrap_i64]
        val Op_i32_extend8_s = this[WasmRuntime::Op_i32_extend8_s]
        val Op_i32_extend16_s = this[WasmRuntime::Op_i32_extend16_s]
        val Op_i32_reinterpret_f32 = this[WasmRuntime::Op_i32_reinterpret_f32]
        val Op_f32_reinterpret_i32 = this[WasmRuntime::Op_f32_reinterpret_i32]
        val Op_i64_reinterpret_f64 = this[WasmRuntime::Op_i64_reinterpret_f64]
        val Op_f64_reinterpret_i64 = this[WasmRuntime::Op_f64_reinterpret_i64]
        val Op_f32_convert_s_i32 = this[WasmRuntime::Op_f32_convert_s_i32]
        val Op_f32_convert_u_i32 = this[WasmRuntime::Op_f32_convert_u_i32]
        val Op_f32_convert_s_i64 = this[WasmRuntime::Op_f32_convert_s_i64]
        val Op_f32_convert_u_i64 = this[WasmRuntime::Op_f32_convert_u_i64]
        val Op_f32_demote_f64 = this[WasmRuntime::Op_f32_demote_f64]
        val Op_f64_convert_s_i32 = this[WasmRuntime::Op_f64_convert_s_i32]
        val Op_f64_convert_u_i32 = this[WasmRuntime::Op_f64_convert_u_i32]
        val Op_f64_convert_s_i64 = this[WasmRuntime::Op_f64_convert_s_i64]
        val Op_f64_convert_u_i64 = this[WasmRuntime::Op_f64_convert_u_i64]
        val Op_f64_promote_f32 = this[WasmRuntime::Op_f64_promote_f32]
        val Op_i32_trunc_u_f32 = this[WasmRuntime::Op_i32_trunc_u_f32]
        val Op_i32_trunc_s_f32 = this[WasmRuntime::Op_i32_trunc_s_f32]
        val Op_i32_trunc_u_f64 = this[WasmRuntime::Op_i32_trunc_u_f64]
        val Op_i32_trunc_s_f64 = this[WasmRuntime::Op_i32_trunc_s_f64]
        val Op_i32_trunc_sat_f32_u = this[WasmRuntime::Op_i32_trunc_sat_f32_u]
        val Op_i32_trunc_sat_f32_s = this[WasmRuntime::Op_i32_trunc_sat_f32_s]
        val Op_i32_trunc_sat_f64_u = this[WasmRuntime::Op_i32_trunc_sat_f64_u]
        val Op_i32_trunc_sat_f64_s = this[WasmRuntime::Op_i32_trunc_sat_f64_s]
        val Op_i64_trunc_u_f32 = this[WasmRuntime::Op_i64_trunc_u_f32]
        val Op_i64_trunc_s_f32 = this[WasmRuntime::Op_i64_trunc_s_f32]
        val Op_i64_trunc_u_f64 = this[WasmRuntime::Op_i64_trunc_u_f64]
        val Op_i64_trunc_s_f64 = this[WasmRuntime::Op_i64_trunc_s_f64]
        val Op_i64_trunc_sat_f32_u = this[WasmRuntime::Op_i64_trunc_sat_f32_u]
        val Op_i64_trunc_sat_f32_s = this[WasmRuntime::Op_i64_trunc_sat_f32_s]
        val Op_i64_trunc_sat_f64_u = this[WasmRuntime::Op_i64_trunc_sat_f64_u]
        val Op_i64_trunc_sat_f64_s = this[WasmRuntime::Op_i64_trunc_sat_f64_s]
        val Op_f32_min = this[WasmRuntime::Op_f32_min]
        val Op_f32_max = this[WasmRuntime::Op_f32_max]
        val Op_f32_copysign = this[WasmRuntime::Op_f32_copysign]
        val Op_f32_abs = this[WasmRuntime::Op_f32_abs]
        val Op_f32_sqrt = this[WasmRuntime::Op_f32_sqrt]
        val Op_f32_neg = this[WasmRuntime::Op_f32_neg]
        val Op_f32_ceil = this[WasmRuntime::Op_f32_ceil]
        val Op_f32_floor = this[WasmRuntime::Op_f32_floor]
        val Op_f32_trunc = this[WasmRuntime::Op_f32_trunc]
        val Op_f32_nearest = this[WasmRuntime::Op_f32_nearest]
        val Op_f64_min = this[WasmRuntime::Op_f64_min]
        val Op_f64_max = this[WasmRuntime::Op_f64_max]
        val Op_f64_copysign = this[WasmRuntime::Op_f64_copysign]
        val Op_f64_abs = this[WasmRuntime::Op_f64_abs]
        val Op_f64_sqrt = this[WasmRuntime::Op_f64_sqrt]
        val Op_f64_neg = this[WasmRuntime::Op_f64_neg]
        val Op_f64_ceil = this[WasmRuntime::Op_f64_ceil]
        val Op_f64_floor = this[WasmRuntime::Op_f64_floor]
        val Op_f64_trunc = this[WasmRuntime::Op_f64_trunc]
        val Op_f64_nearest = this[WasmRuntime::Op_f64_nearest]
        val Op_memory_size = this[WasmRuntime::Op_memory_size]
        val Op_memory_grow = this[WasmRuntime::Op_memory_grow]
        val Op_memory_copy = this[WasmRuntime::Op_memory_copy]
        val Op_memory_fill = this[WasmRuntime::Op_memory_fill]
    }
}
