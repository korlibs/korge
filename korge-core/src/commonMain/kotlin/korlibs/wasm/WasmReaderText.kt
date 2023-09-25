package korlibs.wasm

import korlibs.datastructure.*
import korlibs.io.lang.*
import korlibs.io.util.*

/**
 * <https://webassembly.github.io/wabt/demo/wat2wasm/>
 */
class WasmReaderText {
    val modules = arrayListOf<WasmModuleBuilder>()
    val currentModule: WasmModuleBuilder get() {
        if (modules.isEmpty()) modules.add(WasmModuleBuilder())
        return modules.last()
    }

    class WasmFuncBuilder(val module: WasmModuleBuilder) {
        var funcName: String? = null
        var exportName: String? = null
        var results = arrayListOf<WasmType>()
        val params = arrayListOf<WastLocal>()
        val justLocals = arrayListOf<WastLocal>()
        val locals = arrayListOf<WastLocal>()
        val localsByName = LinkedHashMap<String, WastLocal>()
        val instructions = arrayListOf<WasmInstruction>()

        val funcType: WasmType.Function by lazy { WasmType.Function(params, results) }

        fun addVar(vname: String?, type: WasmType, isParam: Boolean): WasmFuncBuilder {
            val local = if (vname != null) WastLocal(vname, type, locals.size) else WastLocal(locals.size, type)
            return addVar(local, isParam)
        }

        fun addVar(local: WastLocal, isParam: Boolean): WasmFuncBuilder {
            when {
                isParam -> params += local
                else -> justLocals += local
            }
            locals += local
            localsByName[local.name] = local
            return this
        }

        fun addResult(type: WasmType): WasmFuncBuilder {
            results += type
            return this
        }

        fun buildFunc(builder: WasmModuleBuilder): WasmFunc {
            val funcIndex = builder.funcs.size
            val code = WasmCode(params, listOf(justLocals), WasmExpr(instructions))
            val export = (funcName ?: exportName)?.let { WasmExport(it, -1, -1, null) }
            //println("export=$export")
            val func = WasmFunc(funcIndex, funcType, code = code).also { it.addExport(export) }
            return func
        }

        data class BlockDef(val kind: String, val id: String?)
        val blocks = arrayListOf<BlockDef>()

        fun relativeIndexOfBlockById(id: String?): Int? {
            val absIndex = blocks.indexOfLast { it.id == id }.takeIf { it >= 0 } ?: return null
            return blocks.size - absIndex - 1
        }

        inline fun <T> pushBlock(kind: String, id: String? = null, block: () -> T): T {
            pushBlock(kind, id)
            try {
                return block()
            } finally {
                popBlock()
            }
        }

        fun pushBlock(kind: String, id: String? = null): BlockDef {
            return BlockDef(kind, id).also { blocks.add(it) }
        }
        fun popBlock() {
            blocks.removeLast()
        }
    }

    class WasmModuleBuilder {
        var lastDataPtr = 0
        val funcs = arrayListOf<WasmFunc>()
        val datas = arrayListOf<WasmData>()
        val types = arrayListOf<NamedWasmType>()
        val elements = arrayListOf<WasmElement>()
        val tables = arrayListOf<WasmType.TableType<*>>()
        val memories = arrayListOf<WasmType.Limit>()
        val exports = arrayListOf<WasmExport>()
        val globals = arrayListOf<WasmGlobal>()
        val asserts = arrayListOf<WasmAssert>()
        var startFunc = -1

        val funcsByName = LinkedHashMap<String, WasmFunc>()
        val globalsByName = LinkedHashMap<String, WasmGlobal>()
        val typesByName = LinkedHashMap<String, NamedWasmType>()
        val tablesByName = LinkedHashMap<String, WasmType.TableType<*>>()

        fun addTable(table: WasmType.TableType<*>, name: String) {
            tables += table
            tablesByName[name] = table
        }

        fun addType(type: NamedWasmType) {
            types += type
            typesByName[type.name] = type
        }

        fun addFunc(func: WasmFunc): WasmFunc {
            funcs += func
            funcsByName[func.name] = func
            return func
        }

        fun addGlobal(global: WasmGlobal): WasmGlobal {
            globals += global
            globalsByName[global.name] = global
            return global
        }

        fun buildModule(): WasmModule {
            return WasmModule(
                functions = funcs,
                datas = datas,
                types = types,
                globals = globals,
                elements = elements,
                tables = tables,
                memories = memories,
                exports = exports,
                startFunc = startFunc,
                asserts = asserts,
            )
        }
    }

    fun readTopLevel(wast: String): WasmReaderText {
        for (block in WastParser.parseBlocks(wast)) {
            readTopLevel(block)
        }
        return this
    }

    fun readType(block: WastBlockOrValue): WasmType {
        return when (block) {
            is WastValue -> {
                when (block.value) {
                    "void" -> WasmSType.VOID
                    "i32" -> WasmSType.I32
                    "i64" -> WasmSType.I64
                    "f32" -> WasmSType.F32
                    "f64" -> WasmSType.F64
                    "v128" -> WasmSType.V128
                    "anyref", "externref" -> WasmSType.ANYREF
                    "funcref" -> WasmSType.FUNCREF
                    else -> TODO("$block")
                }
            }
            is WastBlock -> {
                when (block.name) {
                    "func" -> {
                        val results = arrayListOf<WasmType>()
                        val params = arrayListOf<WastLocal>()
                        for (param in block.params) {
                            if (param !is WastBlock) TODO("$param")
                            when (param.name) {
                                "param" -> {
                                    // @TODO: Name
                                    for (p in param.params.map { readType(it) }) {
                                        params += WastLocal(params.size, p)
                                    }
                                }
                                "result" -> {
                                    results += param.params.map { readType(it) }
                                }
                            }
                        }
                        WasmType.Function(params, results)
                    }
                    "mut" -> {
                        WasmType.Mutable(readType(block.params.first()))
                    }
                    else -> {
                        TODO("$block")
                    }
                }
            }
        }
    }

    fun readTopLevel(block: WastBlock) {
        when (block.name) {
            "module" -> {
                val builder = WasmModuleBuilder()
                modules += builder
                for (item in block.blockParams) {
                    readModuleLevel(item, builder)
                }
            }
            "assert_return", "invoke" -> {
                val actual = readCodeLevel(block.blockParams[0], WasmFuncBuilder(WasmModuleBuilder()))
                val expected = block.blockParams?.getOrNull(1)?.let { readCodeLevel(it, WasmFuncBuilder(WasmModuleBuilder())) }
                currentModule.asserts += WasmAssertReturn(actual, expected, "$block")
                //println("assert_return: actual=$actual, expected=$expected")
            }
            "assert_trap" -> {
                Unit
            }
            "assert_invalid" -> {
                Unit
            }
            "assert_malformed" -> {
                Unit
            }
            else -> TODO("${block.name}")
        }
        return Unit
    }

    fun readModuleLevel(block: WastBlock, builder: WasmModuleBuilder) {
        when (block.name) {
            "type" -> {
                val typeName = block.valueParams.first().value
                val type = readType(block.params.last())
                builder.addType(NamedWasmType(builder.types.size, typeName, type))
            }
            "table" -> {
                var name = "\$"
                var type = ""
                for (vtype in block.valueParams) {
                    if (vtype.value.startsWith("\$")) {
                        name = vtype.value
                    } else {
                        type = vtype.value
                    }
                }
                val table = WasmType.TableType(WasmType.Limit(0), WasmFuncRef::class)
                builder.addTable(table, name)
                when (type) {
                    "funcref" -> {
                        for (param in block.blockParams) {
                            when (param.name) {
                                "elem" -> {
                                    val elemName = param.valueParams.first().value
                                    val func = builder.funcsByName[elemName] ?: error("Can't find func '$elemName'")
                                    //table.items.add(func)
                                    TODO("elemName=$elemName, func=$func")
                                }
                                else -> TODO()
                            }
                        }
                    }
                    else -> {
                        TODO("SKIP table=$block")
                    }
                }

            }
            "elem" -> {
                println("SKIP elem=$block")
            }
            "data" -> {
                val expr = block.blockParams.firstOrNull()?.let { readCodeLevel(it, WasmFuncBuilder(builder).addResult(WasmSType.I32)) }
                    ?: WasmExpr(WasmInstruction.InsConstInt(currentModule.lastDataPtr))
                val other = block.valueParams.last()
                val bytes = ByteArray(other.value.length) { other.value[it].code.toByte() }
                currentModule.lastDataPtr += bytes.size
                currentModule.datas += WasmData(0, bytes, currentModule.datas.size, expr)
            }
            "global" -> {
                val globalIndex = currentModule.globals.size
                var gname = "g$globalIndex"
                var type: WasmType? = null
                var expr: WasmExpr? = null
                for (param in block.params) {
                    when {
                        param is WastValue && param.value.startsWith("\$") -> {
                            gname = param.value
                        }
                        type == null -> {
                            type = readType(param)
                        }
                        else -> {
                            expr = readCodeLevel(param as WastBlock, WasmFuncBuilder(builder).addResult(type))
                        }
                    }
                }
                currentModule.addGlobal(WasmGlobal(type ?: WasmSType.VOID, globalIndex, expr, name = gname))
                println("SKIP global=$block")
            }
            "memory" -> {
                val params = block.valueParams.map { it.value.toInt() }
                currentModule.memories += WasmType.Limit(params.getOrNull(0) ?: 0, params.getOrNull(1))
            }
            "func" -> {
                val funcBuilder = WasmFuncBuilder(builder)

                for ((index, param) in block.params.withIndex()) {
                    //println("param=$param")
                    if (index == 0 && param is WastValue) {
                        funcBuilder.funcName = param.value
                        continue
                    }
                    if (param is WastBlock) {
                        var vname: String? = null
                        when (param.name) {
                            "param", "local" -> {
                                for (pp in param.params) {
                                    if (pp is WastValue && pp.value.startsWith("\$")) {
                                        vname = pp.value
                                    } else {
                                        funcBuilder.addVar(vname, readType(pp), isParam = param.name == "param")
                                        vname = null
                                    }
                                }
                            }
                            "result" -> {
                                funcBuilder.addResult(readType(param.params[0]))
                                //TODO("name=$name, type=$type")
                            }
                            "export" -> {
                                funcBuilder.exportName = param.valueParams[0].value
                            }
                            "import" -> {
                                TODO("import=$param")
                            }
                            "type" -> {
                                val typeName = param.valueParams.first().value
                                val type = builder.typesByName[typeName] ?: error("Can't find type '$typeName'")
                                val ftype = type as? WasmType.Function? ?: error("Type $type is not a function")
                                for (arg in ftype.args) funcBuilder.addVar(arg, isParam = true)
                                funcBuilder.addResult(ftype.retType)
                            }
                            else -> {
                                funcBuilder.instructions += readCodeLevel(param, funcBuilder).instructions
                            }
                        }
                    }
                }

                builder.addFunc(funcBuilder.buildFunc(builder))
            }
            else -> TODO("${block.name}")
        }
    }

    fun readCodeLevel(exprs: List<WastBlock>, func: WasmFuncBuilder): WasmExpr {
        return WasmExpr(exprs.map { readCodeLevel(it, func) }.instructions)
    }

    class ExprParamsResult(
        val exprs: List<WasmExpr>,
        val results: List<WasmType>,
        val types: List<NamedWasmType>,
    ) {
        val instructions get() = exprs.instructions
    }

    fun readExprParams(expr: WastBlock, func: WasmFuncBuilder): ExprParamsResult {
        val results = arrayListOf<WasmType>()
        val types = arrayListOf<NamedWasmType>()
        val exprs = expr.blockParams.mapNotNull {
            when (it.name) {
                "result" -> {
                    results += it.params.map { readType(it) }
                    null
                }
                "type" -> {
                    val typeName = it.valueParams.first().value
                    types += func.module.typesByName[typeName] ?: error("Can't find type '$typeName'")
                    null
                }
                else -> {
                    readCodeLevel(it, func)
                }
            }
        }
        return ExprParamsResult(exprs, results, types)
    }

    fun readCodeLevel(expr: WastBlock, func: WasmFuncBuilder): WasmExpr {
        when (expr.name) {
            "invoke", "call" -> {
                val funcName = expr.valueParams.first().value
                val fnc = currentModule.funcsByName[funcName]
                    ?: run { println(currentModule.funcsByName.keys); error("Can't find function '$funcName' in ${currentModule.funcsByName.keys}") }
                return WasmExpr(readExprParams(expr, func).instructions + WasmInstruction.CALL(fnc.index))
            }
            "call_indirect" -> {
                val result = readExprParams(expr, func)
                val funcType = result.types.first()
                //println("TODO: call_indirect: $expr, funcType=$funcType")
                return WasmExpr(readExprParams(expr, func).instructions + WasmInstruction.CALL_INDIRECT(funcType.index, 0))
            }
            "if" -> {
                return func.pushBlock("if", expr.valueParams.firstOrNull()?.value) {
                    var results = arrayListOf<WasmType>()
                    var params = arrayListOf<WasmType>()
                    var btrue: WasmExpr? = null
                    var bfalse: WasmExpr? = null
                    val conds = arrayListOf<WasmExpr>()
                    for (param in expr.blockParams) {
                        when (param.name) {
                            "result" -> results += readType(param.params.first())
                            "param" -> params += readType(param.params.first())
                            "then" -> btrue = readCodeLevel(param.blockParams, func)
                            "else" -> bfalse = readCodeLevel(param.blockParams, func)
                            else -> {
                                conds += readCodeLevel(param, func)
                            }
                        }
                    }
                    WasmExpr(
                        conds.instructions + WasmInstruction.IF(
                            results.firstOrNull() ?: WasmSType.VOID,
                            btrue!!,
                            bfalse
                        )
                    )
                }
            }
            "block", "loop" -> {
                return func.pushBlock(expr.name, expr.valueParams.firstOrNull()?.value) {
                    val result = readExprParams(expr, func)
                    val vexpr = WasmExpr(result.instructions)
                    val blockType = result.results.firstOrNull() ?: WasmSType.VOID
                    WasmExpr(
                        when (expr.name) {
                            "block" -> WasmInstruction.block(blockType, vexpr)
                            "loop" -> WasmInstruction.loop(blockType, vexpr)
                            else -> TODO()
                        }
                    )
                }
            }
            "return" -> {
                return WasmExpr(readExprParams(expr, func).instructions + WasmInstruction.RETURN)
            }
            "f32.const" -> {
                val valueStr = expr.valueParams.first().value
                val value = valueStr.toNumberExOrNull()?.toFloat() ?: error("Can't interpret literal '$valueStr'")
                check(readExprParams(expr, func).exprs.isEmpty())
                return WasmExpr(WasmInstruction.InsConstFloat(value))
            }
            "f64.const" -> {
                val valueStr = expr.valueParams.first().value
                val value = valueStr.toNumberExOrNull()?.toDouble() ?: error("Can't interpret literal '$valueStr'")
                check(readExprParams(expr, func).exprs.isEmpty())
                return WasmExpr(WasmInstruction.InsConstDouble(value))
            }
            "i32.const" -> {
                val valueStr = expr.valueParams.first().value
                val value = valueStr.toNumberExOrNull()?.toInt() ?: error("Can't interpret literal '$valueStr'")
                check(readExprParams(expr, func).exprs.isEmpty())
                return WasmExpr(WasmInstruction.InsConstInt(value))
            }
            "i64.const" -> {
                val valueStr = expr.valueParams.first().value
                val value = valueStr.toNumberExOrNull()?.toLong() ?: error("Can't interpret literal '$valueStr'")
                check(readExprParams(expr, func).exprs.isEmpty())
                return WasmExpr(WasmInstruction.InsConstLong(value))
            }
            "global.set", "global.get" -> {
                val valueStr = expr.valueParams.first().value
                val global = currentModule.globalsByName[valueStr] ?: error("Can't find global '$valueStr'")
                return WasmExpr(readExprParams(expr, func).instructions + WasmInstruction.InsInt(
                    when (expr.name) {
                        "global.set" -> WasmOp.Op_global_set
                        else -> WasmOp.Op_global_get
                    },
                    global.index
                ))
            }
            "local.set", "local.get", "local.tee" -> {
                val localName = expr.valueParams.first().value
                val localIndex = localName.toNumberExOrNull()?.toInt()
                    ?: func.localsByName[localName]?.index ?: error("Can't find local '$localName'")
                return WasmExpr(readExprParams(expr, func).instructions + WasmInstruction.InsInt(
                    when (expr.name) {
                        "local.set" -> WasmOp.Op_local_set
                        "local.get" -> WasmOp.Op_local_get
                        "local.tee" -> WasmOp.Op_local_tee
                        else -> TODO()
                    },
                    localIndex
                ))
            }
            "ref.null" -> {
                val kind = expr.valueParams.last().value
                return WasmExpr(readExprParams(expr, func).instructions + WasmInstruction.InsType(WasmOp.Op_ref_null, when (kind) {
                    "extern" -> WasmSType.ANYREF
                    "func" -> WasmSType.FUNCREF
                    else -> TODO("kind=$kind")
                }))
            }
            "unreachable" -> {
                return WasmExpr(readExprParams(expr, func).instructions + WasmInstruction.unreachable)
            }
            "nop" -> {
                return WasmExpr(readExprParams(expr, func).instructions + WasmInstruction.nop)
            }
            "br", "br_if", "br_table" -> {
                //TODO("${expr.name}")
                val labelNames = expr.valueParams.map { it.value }
                val labels = labelNames.map {
                    it.toIntOrNull()
                        ?: func.relativeIndexOfBlockById(it)
                        ?: error("it=$it is not an integer")
                }
                val instruction = when (expr.name) {
                    "br" -> WasmInstruction.br(labels.first())
                    "br_if" -> WasmInstruction.br_if(labels.first())
                    "br_table" -> WasmInstruction.br_table(labels.dropLast(1), labels.last())
                    else -> TODO()
                }
                return WasmExpr(readExprParams(expr, func).instructions + instruction)
            }
            else -> {
                val op = WasmOp.getOrNull(expr.name) ?: error("Invalid opcode '${expr.name}' in $expr")
                when {
                    op.kind.memoryTransfer -> {
                        var offset = 0
                        var align = 0
                        for (param in expr.valueParams) {
                            when {
                                param.value.startsWith("offset=") -> offset = param.value.substring(7).toLong().toInt()
                                param.value.startsWith("align=") -> align = param.value.substring(6).toLong().toInt()
                            }
                        }
                        //println("expr.valueParams=${expr.valueParams}")
                        return WasmExpr(readExprParams(expr, func).instructions + WasmInstruction.InsMemarg(op, align, offset))
                    }
                    expr.valueParams.isNotEmpty() -> {
                        val localName = expr.valueParams.first().value
                        val arg = localName.toNumberExOrNull()?.toInt()
                            ?: func.funcType.argsByName[localName]?.index
                            ?: error("Can't find localName=$localName in $expr in func=$func")
                        return WasmExpr(WasmInstruction.InsInt(op, arg))
                    }
                    else -> {
                        return WasmExpr(readExprParams(expr, func).instructions + WasmInstruction.Ins(op))
                    }
                }
            }
        }
    }

    fun String.toNumberExOrNull(radix: Int = 10): Number? {
        //println("this=$this, radix=$radix")
        if (startsWith("-")) {
            val value = this.substring(1).toNumberExOrNull(radix) ?: return null
            return if (value is Long) -value else if (value is Double) -value else TODO()
        }
        if (startsWith("+")) return this.substring(1).toNumberExOrNull(radix)
        if (this == "inf") return Double.POSITIVE_INFINITY
        if (this == "nan") return Double.NaN
        if (this.startsWith("nan:")) {
            val nanStr = this.substring(4)
            if (nanStr == "canonical") return Double.NaN
            if (nanStr == "arithmetic") return Double.NaN
            val nanNumber = nanStr.toNumberExOrNull() ?: error("INVALID nanStr=$nanStr")

            if (nanNumber.toLong() == nanNumber.toInt().toLong()) {
                return Float.fromBits(0x7F800000 or nanNumber.toInt())
            } else {
                return Double.fromBits(0x7FF0000000000000L or nanNumber.toLong())
            }
        }
        if (startsWith("0x") || startsWith("0X")) return this.substring(2).toNumberUnprefixedExOrNull(16)
        if (startsWith("0o") || startsWith("0O")) return this.substring(2).toNumberUnprefixedExOrNull(8)
        if (startsWith("0b") || startsWith("0B")) return this.substring(2).toNumberUnprefixedExOrNull(2)
        //if (this.contains("_")) TODO("$this")
        val cleanStr = this.replace("_", "")
        //println("cleanStr=$cleanStr, radix=$radix")
        return cleanStr.toLongOrNull(radix)
            ?: cleanStr.toULongOrNull(radix)?.toLong()
            ?: cleanStr.toDoubleOrNull()
    }

    fun String.toNumberUnprefixedExOrNull(radix: Int = 10): Number? {
        if (startsWith("-")) {
            val value = this.substring(1).toNumberUnprefixedExOrNull(radix) ?: return null
            return if (value is Long) -value else if (value is Double) -value else TODO()
        }
        if (this.startsWith("+")) return this.substring(1).toNumberUnprefixedExOrNull(radix)
        if (this == "inf") return Double.POSITIVE_INFINITY
        val cleanStr = this.replace("_", "")
        if (radix == 16 && cleanStr.contains("p")) {
            val (dec, exp) = cleanStr.split("p")
            val (dec1, dec2) = (dec.split(".") + listOf("0"))
            val decNum1 = dec1.toNumberUnprefixedExOrNull(16)
            val decNum2 = dec2.toNumberUnprefixedExOrNull(16)
            val expNum = exp.toNumberUnprefixedExOrNull(16)
            val str = "${decNum1}.${decNum2}e${expNum}"
            val num = str.toNumberUnprefixedExOrNull(10)
            if (num == null) TODO("num=$num, str=$str, dec=$dec, exp=$exp, this=$this, dec1=$dec1, dec2=$dec2")
            return num
        }
        //println("cleanStr=$cleanStr, radix=$radix")
        return cleanStr.toLongOrNull(radix)
            ?: cleanStr.toULongOrNull(radix)?.toLong()
            ?: cleanStr.toDoubleOrNull()
    }


    object WastParser {
        fun parseBlock(wast: String): WastBlock {
            return parseBlocks(wast).first()
        }
        fun parseBlocks(wast: String): List<WastBlock> {
            val tokens = StrReader(wast).wastTokenize()
            //for (token in tokens) println(token)
            val lreader = ListReader(tokens)
            //println("BLOCK: ${lreader.position}/${lreader.size}")
            val blocks = lreader.parseLevel(level = 0)
            //println("BLOCK: ${lreader.position}/${lreader.size}")
            return blocks.filterIsInstance<WastBlock>()
        }

        fun ListReader<Token>.parseLevel(comment: String? = null, level: Int = 0): List<WastBlockOrValue> {
            val out = arrayListOf<WastBlockOrValue>()
            var rcomment: String? = null
            loop@ while (hasMore) {
                val item = peek()
                when (item) {
                    OPEN_BRAC -> {
                        read()
                        //println("${"  ".repeat(level)}OPEN : ${peek(0)}")

                        // IGNORE EMPTY BLOCKS
                        if (peek() == CLOSE_BRAC) {
                            read()
                            continue@loop
                        }

                        val block = parseLevel(rcomment, level = level + 1)
                        rcomment = null
                        out.add(WastBlock((block.first() as WastValue).value, block.drop(1), rcomment))
                        if (eof) break@loop
                        check(read() == CLOSE_BRAC)
                    }
                    CLOSE_BRAC -> {
                        //println("${"  ".repeat(level)}CLOSE")
                        rcomment = null
                        break@loop
                    }
                    is COMMENT -> {
                        rcomment = read().str
                    }
                    else -> {
                        out.add(WastValue(read().str))
                        rcomment = null
                    }
                }
            }
            //if (out.firstOrNull() is Block) return out.first() as Block
            //return Block(out.first().toString(), out.drop(1), comment)
            return out
        }


        fun StrReader.wastTokenize(): List<Token> {
            val out = arrayListOf<Token>()
            loop@ while (!eof) {
                val peek = peekChar()
                if (tryLit("nan:", consume = true) != null || tryLit("-nan:", consume = true) != null) {
                    out += SpecialNum(
                        "nan",
                        readWhile { it in '0'..'9' || it in 'a'..'z' || it in 'A'..'Z' || it == '.' || it == '-' || it == '+' || it == '_' }
                    )
                    continue
                }
                when (peek) {
                    ' ', '\t', '\r', '\n' -> {
                        readChar() // skip
                    }
                    ';' -> {
                        readChar()
                        val comment = readUntil('\n') ?: ""
                        out += COMMENT(comment)
                    }
                    '(' -> run {
                        read()
                        if (peekChar() == ';') {
                            readChar()
                            val comment = readUntil(";)")
                            out += COMMENT(comment)
                        } else {
                            out += OPEN_BRAC
                        }
                    }
                    ')' -> run { read(); out += CLOSE_BRAC }
                    '=' -> run { out += Op(read(1)) }
                    '"' -> {
                        readChar()
                        var str = StringBuilder()
                        loop@ while (!eof) {
                            val pp = peekChar()
                            when (pp) {
                                '\\' -> {
                                    val p1 = read()
                                    val p2 = read()
                                    str.append(
                                        when (p2) {
                                            '\\' -> '\\'
                                            '\"' -> '\"'
                                            '\'' -> '\''
                                            't' -> '\t'
                                            'r' -> '\r'
                                            'n' -> '\n'
                                            in '0'..'9', in 'a'..'f', in 'A'..'F' -> {
                                                val p3 = read()
                                                val vh = HexUtil.unhex(p2)
                                                val vl = HexUtil.unhex(p3)
                                                ((vh shl 4) or vl).toChar()
                                            }
                                            else -> TODO("unknown string escape sequence $p1$p2")
                                        }
                                    )
                                }
                                '\"' -> {
                                    readChar()
                                    break@loop
                                }
                                else -> str.append(read())
                            }
                        }
                        out += Str(str.toString())
                    }
                    in '0'..'9', '-', '+' -> {
                        out += Num(readWhile { it in '0'..'9' || it in 'a'..'z' || it in 'A'..'Z' || it == '.' || it == '-' || it == '+' || it == '_' })
                    }
                    in 'a'..'z', in 'A'..'Z', '$', '%', '_', '.', '/' -> {
                        out += Id(readWhile { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '$' || it == '%' || it == '_' || it == '.' || it == '>' || it == '/' || it == '-' || it == '+' || it == '=' })
                    }
                    else -> invalidOp("Unknown '$peek' at ${this.pos} near '${this.peek(16)}'")
                }
            }
            return out
        }

        interface Token {
            val str: String
        }

        data object OPEN_BRAC : Token {
            override val str = "("
        }

        data object CLOSE_BRAC : Token {
            override val str = ")"
        }

        data class COMMENT(val comment: String) : Token {
            override val str = comment
        }

        data class Op(val op: String) : Token {
            override val str = op
        }

        data class Str(val string: String) : Token {
            override val str = string
        }

        data class Num(val num: String) : Token {
            override val str = num
        }

        data class SpecialNum(val special: String, val num: String) : Token {
            override val str = "$special:$num"
        }

        data class Id(val id: String) : Token {
            override val str = id
        }

        class ParamsReader(val params: List<Any?>) {
            val reader = ListReader(params)
            val hasMore: Boolean get() = reader.hasMore

            fun rest(): List<Any?> = mapWhile({ reader.hasMore }) { reader.read() }
            fun restBlock(): List<WastBlock> = rest().map { it as WastBlock }
            fun peek() = reader.peek()
            fun read() = reader.read()
            fun string() = reader.read() as? String? ?: error("$this at index=${reader.position} is not a String")
            fun block() = reader.read() as? WastBlock? ?: error("$this at index=${reader.position} is not a Block")
        }

        object HexUtil {
            fun unhex(char: Char): Int = when (char) {
                in '0'..'9' -> (char - '0')
                in 'a'..'f' -> (char - 'a') + 10
                in 'A'..'F' -> (char - 'A') + 10
                else -> throw RuntimeException("Not an hex character")
            }
        }


        fun StrReader.readUntil(str: String): String {
            var out = ""
            while (hasMore) {
                if (peek(str.length) == str) {
                    skip(str.length)
                    break
                }
                out += readChar()
            }
            return out
        }
    }

    sealed interface WastBlockOrValue

    data class WastValue(val value: String) : WastBlockOrValue {
        override fun toString(): String = value
    }

    data class WastBlock(val name: String, val params: List<WastBlockOrValue>, val comment: String? = null) : WastBlockOrValue {
        val valueParams by lazy { params.filterIsInstance<WastValue>() }
        val blockParams by lazy { params.filterIsInstance<WastBlock>() }
        fun reader() = WastParser.ParamsReader(params)
        val nparams get() = params.size
        fun string(index: Int) = params[index] as? String? ?: error("$this at index=$index is not a String")
        fun block(index: Int) = params[index] as? WastBlock? ?: error("$this at index=$index is not a Block")
        override fun toString(): String = "($name ${params.joinToString(" ")})"
    }
}
