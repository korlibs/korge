package korlibs.wasm

import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.util.*

// https://webassembly.github.io/spec/core/_download/WebAssembly.pdf
class WasmReaderBinary {
    fun toModule() = WasmModule(
        functions = functions.values.toList(),
        datas = datas,
        types = types,
        globals = globals.values.toList(),
        elements = elements.toList(),
        tables = tables,
        memories = memories,
        exports = exports,
        startFunc = startFunc,
    )

    companion object {
        val INT_FUNC_TYPE = WasmType.Function(listOf(), listOf(WasmType.i32))
    }

    fun read(s: SyncStream): WasmReaderBinary = s.readModule()

    private fun SyncStream.readLEB128_L(signed: Boolean, bits: Int = 64): Long {
        var result = 0L
        var shift = 0
        while (true) {
            val byte = readU8().toLong()
            result = result or ((byte and 0x7F) shl shift)
            shift += 7
            if ((byte and 0x80) == 0L) {
                if (signed && shift < bits && (byte and 0x40) != 0L) {
                    return result or (0L.inv() shl shift)
                }
                return result
            }
        }
    }

    private fun SyncStream.readLEB128_I(signed: Boolean, bits: Int = 32): Int {
        var result = 0
        var shift = 0
        while (true) {
            val byte = readU8()
            result = result or ((byte and 0x7F) shl shift)
            shift += 7
            if ((byte and 0x80) == 0) {
                if (signed && shift < bits && (byte and 0x40) != 0) {
                    return result or (0.inv() shl shift)
                }
                return result
            }
        }
    }

    fun SyncStream.readLEB128(): Int = readLEB128_I(false, 32)
    fun SyncStream.readLEB128S(): Int = readLEB128_I(true, 32)
    fun SyncStream.readLEB128Long(): Long = readLEB128_L(false)
    fun SyncStream.readLEB128SLong(): Long = readLEB128_L(true)

    fun SyncStream.readName() = readString(readLEB128(), UTF8)

    var types = listOf<NamedWasmType>(); private set
    var tables = listOf<WasmType.TableType<*>>(); private set
    var memories = listOf<WasmType.Limit>(); private set
    var functions = LinkedHashMap<Int, WasmFunc>(); private set
    var globals = LinkedHashMap<Int, WasmGlobal>(); private set
    var elements = listOf<WasmElement>(); private set
    var imports = listOf<WasmImport>(); private set
    var exports = listOf<WasmExport>(); private set
    var datas = listOf<WasmData>(); private set
    var startFunc: Int = -1; private set
    var dataCount: Int = -1; private set

    val exportsByName by lazy { exports.associateBy { it.name } }

    fun getFuncByName(name: String): WasmFunc? = exportsByName[name]?.obj as? WasmFunc?

    fun SyncStream.readModule(): WasmReaderBinary {
        if (readString(4) != "\u0000asm") invalidOp("Not a WASM file")
        val version = readS32LE()
        if (version != 1) error("Only WASM version 1 supported")
        while (!eof) readSection()


        /*
        for ((index, function) in functions) {
            println("- ${function.name} ($index) -------------")

            exporter.dump(this@Wasm, function)

            val code = function.code
            if (code != null) {
                val expr = code.expr
                //for (item in expr.ins) println(item)


                val ast = expr.toAst(function.type)
                for ((rindex, local) in (code.locals.flatMap { it }.withIndex())) {
                    val idx = rindex + function.type.args.size
                    println("var l$idx: $local = 0")
                }
                println(ast.dump())
            }
        }
        */
        //println("available: $available")
        return this@WasmReaderBinary
    }

    // 5.5.2 Sections
    fun SyncStream.readSection() {
        val type = readLEB128()
        val len = readLEB128()
        val content = readStream(len)
        //println("$type")
        content.apply {
            when (type) {
                0 -> {
                    val name = readStringVL()
                    println("Unsupported custom section $type '$name'")
                    //TODO("Unsupported custom section '$name'")
                }
                1 -> readTypesSection()
                2 -> readImportSection()
                3 -> readFunctionSection()
                4 -> readTableSection()
                5 -> readMemorySection()
                6 -> readGlobalSection()
                7 -> readExportSection()
                8 -> readStartSection()
                9 -> readElementSection()
                10 -> readCodeSection()
                11 -> readDataSection()
                12 -> readDataCountSection()
                else -> println("Unsupported section $type")
            }
        }
    }

    fun SyncStream.readData(index: Int): WasmData {
        val memindex = readLEB128()
        val expr = readExpr()
        val data = readBytesExact(readLEB128())
        return WasmData(
            memindex = memindex, data = data, index = index, e = expr
        )
    }

    fun SyncStream.readDataSection() {
        datas = readVec { readData(it) }
        for ((index, data) in datas.withIndex()) {
            trace { "// DATA[$index]: ${data.data.size}: ${data.memindex}, ${data.e}" }
        }
    }

    fun SyncStream.readCodeLocals(): List<WasmType> {
        val n = readLEB128()
        val type = readType()
        return (0 until n).map { type }
    }

    fun SyncStream.readCode(): WasmCode {
        val size = readLEB128()
        val ss = readBytesExact(size).openSync()
        val locals = ss.readVec { index -> ss.readCodeLocals().map { WastLocal(index, it) } }
        val expr = ss.readExpr()
        return WasmCode(null, locals, expr)
    }

    fun SyncStream.readCodeSection() {
        val offset = importFunctionsOffset
        for ((index, code) in readVec { readCode() }.withIndex()) {
            functions[offset + index]?.code = code
        }
        for ((index, func) in functions) {
            trace { "// CODE[$index]: ${func.code}" }
        }
    }

    fun SyncStream.readTypesSection() {
        types = readVec { NamedWasmType(it, "type$it", readType()) }
        for ((index, type) in types.withIndex()) {
            trace { "// TYPE[$index]: $type" }
        }
    }

    var doTrace = false
    fun doTrace(value: Boolean): WasmReaderBinary {
        this.doTrace = value
        return this
    }
    inline private fun trace(str: () -> String) {
        if (doTrace) println(str())
    }

    fun SyncStream.readType(): WasmType {
        val type = readU8()
        //println("%02X".format(type))
        return when (type) {
            0x40 -> WasmType.void
            0x60 -> WasmType.Function(readVec { WastLocal(it, readType()) }, readVec { readType() })
            0x6F -> TODO("externref")
            0x70 -> TODO("funcref")
            0x7F -> WasmType.i32
            0x7B -> WasmType.v128
            0x7E -> WasmType.i64
            0x7D -> WasmType.f32
            0x7C -> WasmType.f64
            else -> invalidOp("Unknown type $type")
        }
    }

    fun SyncStream.readBlockType(): WasmType = readType()

    fun SyncStream.readImportSection() {
        imports = readVec { readImport() }
        for ((index, import) in imports.withIndex()) {
            trace { "// IMPORT[$index]: $import" }
        }
    }


    fun SyncStream.readMemtype(): WasmType.Limit {
        val limitType = readU8()
        return when (limitType) {
            0x00 -> WasmType.Limit(readLEB128(), null)
            0x01 -> WasmType.Limit(readLEB128(), readLEB128())
            else -> invalidOp("invalid limitType $limitType")
        }
    }

    fun SyncStream.readTableType(): WasmType.TableType<*> {
        val elemType = readU8()
        if (elemType != 0x70) invalidOp("Invalid elementType $elemType")
        return WasmType.TableType(readMemtype(), WasmFuncRef::class)
    }

    fun SyncStream.readGlobalType(): WasmType {
        val t = readType()
        val mut = readU8()
        return if (mut != 0) WasmType.Mutable(t) else t
    }


    val INDEX_FUNCTIONS = 0
    val INDEX_TABLES = 1
    val INDEX_MEMTYPES = 2
    val INDEX_GLOBALS = 3
    val indicesInTables = arrayOf(0, 0, 0, 0)
    val importsInTables = arrayOf(0, 0, 0, 0)
    val importFunctionsOffset get() = importsInTables[INDEX_FUNCTIONS]
    //var functionIndex = 0

    fun SyncStream.readImport(): WasmImport {
        val moduleName = readName()
        val name = readName()
        val indexSpace = readU8()
        val index = indicesInTables[indexSpace]++
        importsInTables[indexSpace] = indicesInTables[indexSpace]
        val type = when (indexSpace) {
            INDEX_FUNCTIONS -> types[readLEB128()].type
            INDEX_TABLES -> readTableType()
            INDEX_MEMTYPES -> readMemtype()
            INDEX_GLOBALS -> readGlobalType()
            else -> invalidOp("Unsupported import=$indexSpace")
        }
        val fimport = WasmImport(moduleName, name, indexSpace, index, type)
        when (indexSpace) {
            INDEX_FUNCTIONS -> functions[index] =
                    WasmFunc(index, type as WasmType.Function, code = null, fimport = fimport)
            INDEX_GLOBALS -> globals[index] = WasmGlobal(type as WasmType, index, expr = null, gimport = fimport)
        }
        //println("$nm::$name = $type")
        return fimport
    }


    fun SyncStream.readExportSection() {
        exports = readVec { readExport() }
        for ((index, export) in exports.withIndex()) {
            trace { "// EXPORT[$index]: $export" }
        }
    }

    // The start section has the id 8. It decodes into an optional start function that represents the component of a module.
    fun SyncStream.readStartSection() {
        startFunc = readLEB128()
        trace { "// START: $startFunc" }
    }

    fun SyncStream.readDataCountSection() {
        dataCount = readLEB128()
        trace { "// DATA_COUNT: $dataCount" }
    }

    fun SyncStream.readExport(): WasmExport {
        val name = readName()
        val tid = readU8()
        val idx = readLEB128()
        val obj: Any? = when (tid) {
            0x00 -> functions[idx]
            0x01 -> tables[idx]
            0x02 -> memories[idx]
            0x03 -> globals[idx]
            else -> invalidOp("Unsupported export=$tid")
        }
        val export = WasmExport(name, tid, idx, obj)

        if (tid == 0) {
            functions[idx]?.addExport(export)
        }

        //println("export[$name] = $tid[$idx] -- $obj")
        return export
    }

    fun SyncStream.readFunctionSection() {
        val funcs = readVec {
            val index = readLEB128()
            WasmFunc(indicesInTables[INDEX_FUNCTIONS]++, types[index].type as WasmType.Function)
        }
        for (func in funcs) functions[func.index] = func
        for ((index, func) in functions) {
            trace { "// FUNC[$index]: $func" }
        }
    }

    fun SyncStream.readTableSection() {
        val tableTypes = readVec { readTableType() }
        this@WasmReaderBinary.tables = tableTypes
        for ((index, table) in tableTypes.withIndex()) {
            trace { "// TABLE[$index]: $table" }
        }
        //trace("// TABLE REMAINING BYTES: $availableRead")
    }

    fun SyncStream.readMemorySection() {
        val memoryTypes = readVec { readMemtype() }
        this@WasmReaderBinary.memories = memoryTypes
        for ((index, memory) in memoryTypes.withIndex()) {
            trace { "// MEMORY[$index]: $memory" }
        }
        //trace("// MEMORY REMAINING BYTES: $availableRead")
    }

    fun SyncStream.readGlobalSection() {
        val glbs = readVec { readGlobal() }
        for (g in glbs) globals[g.index] = g
        for ((index, global) in globals) {
            trace { "// GLOBAL[$index]: $global" }
        }
    }

    fun SyncStream.readTableIdx() = readLEB128()
    fun SyncStream.readFuncIdx() = readLEB128()


    fun SyncStream.readElement(): WasmElement =
        WasmElement(tableIdx = readTableIdx(), expr = readExpr(), funcIdxs = readVec { readFuncIdx() })

    fun SyncStream.readElementSection() {
        elements = readVec { readElement() }

        for ((index, e) in elements.withIndex()) {
            trace { "// ELEMENT[$index]: $e" }
        }
    }

    fun SyncStream.readGlobal(): WasmGlobal {
        val gt = readGlobalType()
        val e = readExpr()
        trace { "// GLOBAL: $gt, $e" }
        return WasmGlobal(gt, indicesInTables[INDEX_GLOBALS]++, e)
    }


    //enum class unop {
    //    // int
    //    clz, ctz, popcnt,
    //    // float
    //    abs, neg, sqrt, ceil, floor, trunc, nearest
    //}

    //enum class binop {
    //    add, sub, mul, div_sx, rem_sx, and, or, xor, shl, shr_sx, rotl, rotr,

    //    // float
    //    div, min, max, copysign // add, sub, mul,
    //}


    fun SyncStream.readExpr(): WasmExpr {
        val seq = arrayListOf<WasmInstruction>()
        while (true) {
            val i = readInstr()
            //println("i: $i")
            if (i === WasmInstruction.End) break
            seq += i
            if (i is WasmInstruction.ELSE) break
        }
        //println("----------")
        return WasmExpr(seq)
    }

    fun SyncStream.peekU8() = keepPosition { readU8() }

    fun <T> SyncStream.readVec(callback: (Int) -> T): List<T> {
        return (0 until readLEB128()).map { callback(it) }
    }

    fun SyncStream.readInstr(): WasmInstruction {
        //println("%08X: ${position.toInt().hex}")
        val op = readU8()

        val i = when (op) {
            0x00 -> WasmInstruction.unreachable
            0x01 -> WasmInstruction.nop
            0x02 -> WasmInstruction.block(readBlockType(), readExpr())
            0x03 -> WasmInstruction.loop(readBlockType(), readExpr())
            0x04 -> {
                val bt = readBlockType()
                val in1 = readExpr()
                val lastInstruction = in1.instructions.lastOrNull()
                if (lastInstruction is WasmInstruction.ELSE) {
                    val _else = in1.instructions.last() as WasmInstruction.ELSE
                    WasmInstruction.IF(bt, WasmExpr(in1.instructions.dropLast(1)), _else.code)
                    //WasmInstruction.IF(bt, in1, _else.code)
                } else {
                    WasmInstruction.IF(bt, in1, null)
                }
            }
            0x05 -> WasmInstruction.ELSE(readExpr())
            0x0B -> WasmInstruction.End
            0x0C -> WasmInstruction.br(readLEB128())
            0x0D -> WasmInstruction.br_if(readLEB128())
            0x0E -> WasmInstruction.br_table(readVec { readLEB128() }, readLEB128())
            0x0F -> WasmInstruction.RETURN
            0x10 -> WasmInstruction.CALL(readLEB128())
            0x11 -> WasmInstruction.CALL_INDIRECT(readLEB128(), readU8())
            0x1A, 0x1B -> WasmInstruction.Ins(WasmOp[op])
            in 0x20..0x24 -> WasmInstruction.InsInt(WasmOp[op], readLEB128())
            in 0x28..0x3E -> WasmInstruction.InsMemarg(WasmOp[op], readLEB128(), readLEB128())
            // memory.size, memory.grow
            0x3F, 0x40 -> WasmInstruction.InsInt(WasmOp[op], readLEB128())
            0x41 -> WasmInstruction.InsConstInt(readLEB128S(), WasmOp[op])
            0x42 -> WasmInstruction.InsConstLong(readLEB128SLong(), WasmOp[op])
            0x43 -> WasmInstruction.InsConstFloat(readF32LE(), WasmOp[op])
            0x44 -> WasmInstruction.InsConstDouble(readF64LE(), WasmOp[op])
            in 0x45..0xBF -> WasmInstruction.Ins(WasmOp[op])
            in 0xC0..0xC4 -> WasmInstruction.Ins(WasmOp[op])
            in 0xFB..0xFE -> {
                val eop = (op shl 8) or readU8()
                if (eop in 0xFC0A..0xFC0B) {
                    WasmInstruction.InsInt(WasmOp[eop], 0)
                } else {
                    WasmInstruction.Ins(WasmOp[eop])
                }
            }
            else -> invalidOp("Unsupported 0x%02X".format(op))
        }

        //println(" ---> $op: $oop [$i]")
        return i
    }
}

class WasmExpr(val instructions: List<WasmInstruction>) {
    constructor(vararg instructions: WasmInstruction) : this(instructions.toList())
    var interpreterCode: WasmInterpreterCode? = null
    override fun toString(): String = "WasmExpr[${instructions.size}]{${instructions.joinToString(",") { "${it.op}" }}}"
}

val List<WasmExpr>.instructions: List<WasmInstruction> get() = flatMap { it.instructions }
