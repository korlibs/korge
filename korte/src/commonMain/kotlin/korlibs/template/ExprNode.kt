package korlibs.template

import korlibs.template.dynamic.Dynamic2
import korlibs.template.dynamic.DynamicContext
import korlibs.template.internal.StrReader
import korlibs.template.internal.isDigit
import korlibs.template.internal.isLetterDigitOrUnderscore
import korlibs.template.internal.unescape
import korlibs.template.util.ListReader
import kotlin.coroutines.cancellation.CancellationException

interface ExprNode : DynamicContext {
    suspend fun eval(context: Template.EvalContext): Any?

    data class VAR(val name: String) : ExprNode {
        override suspend fun eval(context: Template.EvalContext): Any? {
            return context.config.variableProcessor(context, name)
        }
    }

    data class LIT(val value: Any?) : ExprNode {
        override suspend fun eval(context: Template.EvalContext): Any? = value
    }

    data class ARRAY_LIT(val items: List<ExprNode>) : ExprNode {
        override suspend fun eval(context: Template.EvalContext): Any? {
            return items.map { it.eval(context) }
        }
    }

    data class OBJECT_LIT(val items: List<Pair<ExprNode, ExprNode>>) : ExprNode {
        override suspend fun eval(context: Template.EvalContext): Any? {
            return items.map { it.first.eval(context) to it.second.eval(context) }.toMap()
        }
    }

    data class FILTER(val name: String, val expr: ExprNode, val params: List<ExprNode>, val tok: ExprNode.Token) : ExprNode {
        override suspend fun eval(context: Template.EvalContext): Any? {
            val filter = context.config.filters[name] ?: context.config.filters["unknown"] ?: context.config.unknownFilter
            return context.filterCtxPool.alloc {
                it.tok = tok
                it.name = name
                it.context = context
                it.subject = expr.eval(context)
                it.args = params.map { it.eval(context) }
                filter.eval(it)
            }
        }
    }

    data class ACCESS(val expr: ExprNode, val name: ExprNode) : ExprNode {
        override suspend fun eval(context: Template.EvalContext): Any? {
            val obj = expr.eval(context)
            val key = name.eval(context)
            return try {
                return Dynamic2.accessAny(obj, key, context.mapper)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                try {
                    Dynamic2.callAny(obj, "invoke", listOf(key), mapper = context.mapper)
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    null
                }
            }
        }
    }

    data class CALL(val method: ExprNode, val args: List<ExprNode>) : ExprNode {
        override suspend fun eval(context: Template.EvalContext): Any? {
            val processedArgs = args.map { it.eval(context) }
            when (method) {
                is ExprNode.ACCESS -> {
                    val obj = method.expr.eval(context)
                    val methodName = method.name.eval(context)
                    //println("" + obj + ":" + methodName)
                    if (obj is Map<*, *>) {
                        val k = obj[methodName]
                        if (k is Template.DynamicInvokable) {
                            return k.invoke(context, processedArgs)
                        }
                    }
                    //return obj.dynamicCallMethod(methodName, *processedArgs.toTypedArray(), mapper = context.mapper)
                    return Dynamic2.callAny(obj, methodName, processedArgs.toList(), mapper = context.mapper)
                }
                is ExprNode.VAR -> {
                    val func = context.config.functions[method.name]
                    if (func != null) {
                        return func.eval(processedArgs, context)
                    }
                }
            }
            //return method.eval(context).dynamicCall(processedArgs.toTypedArray(), mapper = context.mapper)
            return Dynamic2.callAny(method.eval(context), processedArgs.toList(), mapper = context.mapper)
        }
    }

    data class BINOP(val l: ExprNode, val r: ExprNode, val op: String) : ExprNode {
        override suspend fun eval(context: Template.EvalContext): Any? {
            val lr = l.eval(context)
            val rr = r.eval(context)
            return when (op) {
                "~" -> lr.toDynamicString() + rr.toDynamicString()
                ".." -> DefaultFunctions.Range.eval(listOf(lr, rr), context)
                else -> Dynamic2.binop(lr, rr, op)
            }
        }
    }

    data class TERNARY(val cond: ExprNode, val etrue: ExprNode, val efalse: ExprNode) : ExprNode {
        override suspend fun eval(context: Template.EvalContext): Any? {
            return if (cond.eval(context).toDynamicBool()) {
                etrue.eval(context)
            } else {
                efalse.eval(context)
            }
        }
    }

    data class UNOP(val r: ExprNode, val op: String) : ExprNode {
        override suspend fun eval(context: Template.EvalContext): Any? {
            return when (op) {
                "", "+" -> r.eval(context)
                else -> Dynamic2.unop(r.eval(context), op)
            }
        }
    }

    companion object {
        fun parse(tag: korlibs.template.Token.TTag): ExprNode = parse(tag.content, tag.posContext)

        fun parse(str: String, context: FilePosContext): ExprNode {
            val tokens = ExprNode.Token.tokenize(str, context)
            if (tokens.list.isEmpty()) context.exception("No expression")
            return ExprNode.parseFullExpr(tokens).also {
                tokens.expectEnd()
            }
        }

        fun parse(str: String, fileName: String = "expression"): ExprNode {
            return ExprNode.parse(str, FilePosContext(FileContext(fileName, str), 1))
        }

        fun parseId(r: ListReader<Token>): String {
            return r.tryRead()?.text ?: (r.tryPrev() ?: r.ctx)?.exception("Expected id") ?: TODO()
        }

        fun expect(r: ListReader<Token>, vararg tokens: String) {
            val token = r.tryRead() ?: r.prevOrContext().exception("Expected ${tokens.joinToString(", ")} but found end")
            if (token.text !in tokens) token.exception("Expected ${tokens.joinToString(", ")} but found $token")
        }

        fun parseFullExpr(r: ListReader<Token>): ExprNode {
            try {
                val result = ExprNode.parseExpr(r)
                if (r.hasMore) {
                    r.peek()
                        .exception("Expected expression at " + r.peek() + " :: " + r.list.map { it.text }.joinToString(""))
                }
                return result
            } catch (e: ListReader.OutOfBoundsException) {
                r.list.last().exception("Incomplete expression")
            }
        }

        private val BINOPS_PRIORITIES_LIST = listOf(
            listOf("*", "/", "%"),
            listOf("+", "-", "~"),
            listOf("==", "===", "!=", "!==", "<", ">", "<=", ">=", "<=>"),
            listOf("&&"),
            listOf("||"),
            listOf("and"),
            listOf("or"),
            listOf("in"),
            listOf("contains"),
            listOf(".."),
            listOf("?:")
        )

        private val BINOPS = BINOPS_PRIORITIES_LIST.withIndex()
            .flatMap { (index, ops) -> ops.map { it to index } }
            .toMap()

        fun binopPr(str: String) = BINOPS[str] ?: 0

        fun parseBinExpr(r: ListReader<Token>): ExprNode {
            var result = parseFinal(r)
            while (r.hasMore) {
                //if (r.peek() !is ExprNode.Token.TOperator || r.peek().text !in ExprNode.BINOPS) break
                if (r.peek().text !in ExprNode.BINOPS) break
                val operator = r.read().text
                val right = parseFinal(r)
                if (result is BINOP) {
                    val a = result.l
                    val lop = result.op
                    val b = result.r
                    val rop = operator
                    val c = right
                    val lopPr = binopPr(lop)
                    val ropPr = binopPr(rop)
                    if (lopPr > ropPr) {
                        result = BINOP(a, BINOP(b, c, rop), lop)
                        continue
                    }
                }
                result = BINOP(result, right, operator)
            }
            return result
        }

        fun parseTernaryExpr(r: ListReader<Token>): ExprNode {
            var left = this.parseBinExpr(r)
            if (r.hasMore) {
                if (r.peek().text == "?") {
                    r.skip()
                    val middle = parseExpr(r)
                    r.expect(":")
                    val right = parseExpr(r)
                    left = TERNARY(left, middle, right)
                }
            }
            return left
        }

        fun parseExpr(r: ListReader<Token>): ExprNode {
            return parseTernaryExpr(r)
        }

        private fun parseFinal(r: ListReader<Token>): ExprNode {
            if (!r.hasMore) r.prevOrContext().exception("Expected expression")
            val tok = r.peek().text.toUpperCase()
            var construct: ExprNode = when (tok) {
                "!", "~", "-", "+", "NOT" -> {
                    val op = tok
                    r.skip()
                    UNOP(
                        parseFinal(r), when (op) {
                            "NOT" -> "!"
                            else -> op
                        }
                    )
                }
                "(" -> {
                    r.read()
                    val result = ExprNode.parseExpr(r)
                    if (r.read().text != ")") throw RuntimeException("Expected ')'")
                    UNOP(result, "")
                }
                // Array literal
                "[" -> {
                    r.read()
                    val items = arrayListOf<ExprNode>()
                    loop@ while (r.hasMore && r.peek().text != "]") {
                        items += ExprNode.parseExpr(r)
                        when (r.peek().text) {
                            "," -> r.read()
                            "]" -> continue@loop
                            else -> r.peek().exception("Expected , or ]")
                        }
                    }
                    r.expect("]")
                    ARRAY_LIT(items)
                }
                // Object literal
                "{" -> {
                    r.read()
                    val items = arrayListOf<Pair<ExprNode, ExprNode>>()
                    loop@ while (r.hasMore && r.peek().text != "}") {
                        val k = ExprNode.parseFinal(r)
                        r.expect(":")
                        val v = ExprNode.parseExpr(r)
                        items += k to v
                        when (r.peek().text) {
                            "," -> r.read()
                            "}" -> continue@loop
                            else -> r.peek().exception("Expected , or }")
                        }
                    }
                    r.expect("}")
                    OBJECT_LIT(items)
                }
                else -> {
                    // Number
                    if (r.peek() is ExprNode.Token.TNumber) {
                        val ntext = r.read().text
                        when (ntext.toDouble()) {
                            ntext.toIntOrNull()?.toDouble() -> LIT(ntext.toIntOrNull() ?: 0)
                            ntext.toLongOrNull()?.toDouble() -> LIT(ntext.toLongOrNull() ?: 0L)
                            else -> LIT(ntext.toDoubleOrNull() ?: 0.0)
                        }
                    }
                    // String
                    else if (r.peek() is ExprNode.Token.TString) {
                        LIT((r.read() as Token.TString).processedValue)
                    }
                    // ID
                    else {
                        val str = r.read().text
                        when (str) {
                            "true" -> LIT(true)
                            "false" -> LIT(false)
                            "null", "nil" -> LIT(null)
                            else -> VAR(str)
                        }
                    }
                }
            }

            loop@ while (r.hasMore) {
                when (r.peek().text) {
                    "." -> {
                        r.read()
                        val id = r.read().text
                        construct = ACCESS(construct, LIT(id))
                        continue@loop
                    }
                    "[" -> {
                        r.read()
                        val expr = ExprNode.parseExpr(r)
                        construct = ACCESS(construct, expr)
                        val end = r.read()
                        if (end.text != "]") end.exception("Expected ']' but found $end")
                    }
                    "|" -> {
                        val tok = r.read()
                        val name = r.tryRead()?.text ?: ""
                        val args = arrayListOf<ExprNode>()
                        if (name.isEmpty()) tok.exception("Missing filter name")
                        if (r.hasMore) {
                            when (r.peek().text) {
                                // jekyll/liquid syntax
                                ":" -> {
                                    r.read()
                                    callargsloop@ while (r.hasMore) {
                                        args += ExprNode.parseExpr(r)
                                        if (r.hasMore && r.peek().text == ",") r.read()
                                    }
                                }
                                // twig syntax
                                "(" -> {
                                    r.read()
                                    callargsloop@ while (r.hasMore && r.peek().text != ")") {
                                        args += ExprNode.parseExpr(r)
                                        when (r.expectPeek(",", ")").text) {
                                            "," -> r.read()
                                            ")" -> break@callargsloop
                                        }
                                    }
                                    r.expect(")")
                                }
                            }
                        }
                        construct = FILTER(name, construct, args, tok)
                    }
                    "(" -> {
                        r.read()
                        val args = arrayListOf<ExprNode>()
                        callargsloop@ while (r.hasMore && r.peek().text != ")") {
                            args += ExprNode.parseExpr(r)
                            when (r.expectPeek(",", ")").text) {
                                "," -> r.read()
                                ")" -> break@callargsloop
                            }
                        }
                        r.expect(")")
                        construct = CALL(construct, args)
                    }
                    else -> break@loop
                }
            }
            return construct
        }
    }

    interface Token : TokenContext {
        val text: String

        data class TId(override val text: String) : ExprNode.Token, TokenContext by TokenContext.Mixin()
        data class TNumber(override val text: String) : ExprNode.Token, TokenContext by TokenContext.Mixin()
        data class TString(override val text: String, val processedValue: String) : ExprNode.Token, TokenContext by TokenContext.Mixin()
        data class TOperator(override val text: String) : ExprNode.Token, TokenContext by TokenContext.Mixin()
        data class TEnd(override val text: String = "") : ExprNode.Token, TokenContext by TokenContext.Mixin()

        companion object {
            private val OPERATORS = setOf(
                "(", ")",
                "[", "]",
                "{", "}",
                "&&", "||",
                "&", "|", "^",
                "==", "===", "!=", "!==", "<", ">", "<=", ">=", "<=>",
                "?:",
                "..",
                "+", "-", "*", "/", "%", "**",
                "!", "~",
                ".", ",", ";", ":", "?",
                "="
            )

            fun ExprNode.Token.annotate(context: FilePosContext, tpos: Int) = this.apply {
                pos = context.pos + tpos
                file = context.file
            }

            fun tokenize(str: String, context: FilePosContext): ListReader<Token> {
                val r = StrReader(str)
                val out = arrayListOf<ExprNode.Token>()
                fun emit(str: ExprNode.Token, tpos: Int) {
                    str.annotate(context, tpos)
                    out += str
                }
                while (r.hasMore) {
                    val start = r.pos
                    r.skipSpaces()
                    val dstart = r.pos
                    val id = r.readWhile(Char::isLetterDigitOrUnderscore)
                    if (id.isNotEmpty()) {
                        if (id[0].isDigit()) {
                            if (r.peek() == '.' && r.peek(2)[1].isDigit()) {
                                r.skip()
                                val decimalPart = r.readWhile(Char::isLetterDigitOrUnderscore)
                                emit(ExprNode.Token.TNumber("$id.$decimalPart"), dstart)
                            } else {
                                emit(ExprNode.Token.TNumber(id), dstart)
                            }
                        } else {
                            emit(ExprNode.Token.TId(id), dstart)
                        }
                    }
                    r.skipSpaces()
                    val dstart2 = r.pos
                    if (r.peek(3) in ExprNode.Token.OPERATORS) emit(ExprNode.Token.TOperator(r.read(3)), dstart2)
                    if (r.peek(2) in ExprNode.Token.OPERATORS) emit(ExprNode.Token.TOperator(r.read(2)), dstart2)
                    if (r.peek(1) in ExprNode.Token.OPERATORS) emit(ExprNode.Token.TOperator(r.read(1)), dstart2)
                    if (r.peek() == '\'' || r.peek() == '"') {
                        val dstart3 = r.pos
                        val strStart = r.read()
                        val strBody = r.readUntil(strStart) ?: context.withPosAdd(dstart3).exception("String literal not closed")
                        val strEnd = r.read()
                        emit(ExprNode.Token.TString(strStart + strBody + strEnd, strBody.unescape()), dstart3)
                    }
                    val end = r.pos
                    if (end == start) {
                        context.withPosAdd(end).exception("Don't know how to handle '${r.peek()}'")
                    }
                }
                val dstart = r.pos
                //emit(ExprNode.Token.TEnd(), dstart)
                return ListReader(out, ExprNode.Token.TEnd().annotate(context, dstart))
            }
        }
    }
}

fun ListReader<ExprNode.Token>.expectEnd() {
    if (hasMore) peek().exception("Unexpected token '${peek().text}'")
}

fun ListReader<ExprNode.Token>.tryRead(vararg types: String): ExprNode.Token? {
    val token = this.peek()
    if (token.text in types) {
        this.read()
        return token
    } else {
        return null
    }
}

fun ListReader<ExprNode.Token>.expectPeek(vararg types: String): ExprNode.Token {
    val token = this.peek()
    if (token.text !in types) throw RuntimeException("Expected ${types.joinToString(", ")} but found '${token.text}'")
    return token
}

fun ListReader<ExprNode.Token>.expect(vararg types: String): ExprNode.Token {
    val token = this.read()
    if (token.text !in types) throw RuntimeException("Expected ${types.joinToString(", ")}")
    return token
}

fun ListReader<ExprNode.Token>.parseExpr() = ExprNode.parseExpr(this)
fun ListReader<ExprNode.Token>.parseId() = ExprNode.parseId(this)
fun ListReader<ExprNode.Token>.parseIdList(): List<String> {
    val ids = arrayListOf<String>()
    do {
        ids += parseId()
    } while (tryRead(",") != null)
    return ids
}
