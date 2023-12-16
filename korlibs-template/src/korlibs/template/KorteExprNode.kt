package korlibs.template

import korlibs.template.dynamic.*
import korlibs.template.internal.*
import korlibs.template.util.*
import kotlin.coroutines.cancellation.*

interface KorteExprNode : KorteDynamicContext {
    suspend fun eval(context: KorteTemplate.EvalContext): Any?

    data class VAR(val name: String) : KorteExprNode {
        override suspend fun eval(context: KorteTemplate.EvalContext): Any? {
            return context.config.variableProcessor(context, name)
        }
    }

    data class LIT(val value: Any?) : KorteExprNode {
        override suspend fun eval(context: KorteTemplate.EvalContext): Any? = value
    }

    data class ARRAY_LIT(val items: List<KorteExprNode>) : KorteExprNode {
        override suspend fun eval(context: KorteTemplate.EvalContext): Any? {
            return items.map { it.eval(context) }
        }
    }

    data class OBJECT_LIT(val items: List<Pair<KorteExprNode, KorteExprNode>>) : KorteExprNode {
        override suspend fun eval(context: KorteTemplate.EvalContext): Any? {
            return items.map { it.first.eval(context) to it.second.eval(context) }.toMap()
        }
    }

    data class FILTER(val name: String, val expr: KorteExprNode, val params: List<KorteExprNode>, val tok: KorteExprNode.Token) : KorteExprNode {
        override suspend fun eval(context: KorteTemplate.EvalContext): Any? {
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

    data class ACCESS(val expr: KorteExprNode, val name: KorteExprNode) : KorteExprNode {
        override suspend fun eval(context: KorteTemplate.EvalContext): Any? {
            val obj = expr.eval(context)
            val key = name.eval(context)
            return try {
                return KorteDynamic2.accessAny(obj, key, context.mapper)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                try {
                    KorteDynamic2.callAny(obj, "invoke", listOf(key), mapper = context.mapper)
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    null
                }
            }
        }
    }

    data class CALL(val method: KorteExprNode, val args: List<KorteExprNode>) : KorteExprNode {
        override suspend fun eval(context: KorteTemplate.EvalContext): Any? {
            val processedArgs = args.map { it.eval(context) }
            when (method) {
                is KorteExprNode.ACCESS -> {
                    val obj = method.expr.eval(context)
                    val methodName = method.name.eval(context)
                    //println("" + obj + ":" + methodName)
                    if (obj is Map<*, *>) {
                        val k = obj[methodName]
                        if (k is KorteTemplate.DynamicInvokable) {
                            return k.invoke(context, processedArgs)
                        }
                    }
                    //return obj.dynamicCallMethod(methodName, *processedArgs.toTypedArray(), mapper = context.mapper)
                    return KorteDynamic2.callAny(obj, methodName, processedArgs.toList(), mapper = context.mapper)
                }
                is KorteExprNode.VAR -> {
                    val func = context.config.functions[method.name]
                    if (func != null) {
                        return func.eval(processedArgs, context)
                    }
                }
            }
            //return method.eval(context).dynamicCall(processedArgs.toTypedArray(), mapper = context.mapper)
            return KorteDynamic2.callAny(method.eval(context), processedArgs.toList(), mapper = context.mapper)
        }
    }

    data class BINOP(val l: KorteExprNode, val r: KorteExprNode, val op: String) : KorteExprNode {
        override suspend fun eval(context: KorteTemplate.EvalContext): Any? {
            val lr = l.eval(context)
            val rr = r.eval(context)
            return when (op) {
                "~" -> lr.toDynamicString() + rr.toDynamicString()
                ".." -> KorteDefaultFunctions.Range.eval(listOf(lr, rr), context)
                else -> KorteDynamic2.binop(lr, rr, op)
            }
        }
    }

    data class TERNARY(val cond: KorteExprNode, val etrue: KorteExprNode, val efalse: KorteExprNode) : KorteExprNode {
        override suspend fun eval(context: KorteTemplate.EvalContext): Any? {
            return if (cond.eval(context).toDynamicBool()) {
                etrue.eval(context)
            } else {
                efalse.eval(context)
            }
        }
    }

    data class UNOP(val r: KorteExprNode, val op: String) : KorteExprNode {
        override suspend fun eval(context: KorteTemplate.EvalContext): Any? {
            return when (op) {
                "", "+" -> r.eval(context)
                else -> KorteDynamic2.unop(r.eval(context), op)
            }
        }
    }

    companion object {
        fun parse(tag: korlibs.template.KorteToken.TTag): KorteExprNode = parse(tag.content, tag.posContext)

        fun parse(str: String, context: KorteFilePosContext): KorteExprNode {
            val tokens = KorteExprNode.Token.tokenize(str, context)
            if (tokens.list.isEmpty()) context.exception("No expression")
            return KorteExprNode.parseFullExpr(tokens).also {
                tokens.expectEnd()
            }
        }

        fun parse(str: String, fileName: String = "expression"): KorteExprNode {
            return KorteExprNode.parse(str, KorteFilePosContext(KorteFileContext(fileName, str), 1))
        }

        fun parseId(r: KorteListReader<Token>): String {
            return r.tryRead()?.text ?: (r.tryPrev() ?: r.ctx)?.exception("Expected id") ?: TODO()
        }

        fun expect(r: KorteListReader<Token>, vararg tokens: String) {
            val token = r.tryRead() ?: r.prevOrContext().exception("Expected ${tokens.joinToString(", ")} but found end")
            if (token.text !in tokens) token.exception("Expected ${tokens.joinToString(", ")} but found $token")
        }

        fun parseFullExpr(r: KorteListReader<Token>): KorteExprNode {
            try {
                val result = KorteExprNode.parseExpr(r)
                if (r.hasMore) {
                    r.peek()
                        .exception("Expected expression at " + r.peek() + " :: " + r.list.map { it.text }.joinToString(""))
                }
                return result
            } catch (e: KorteListReader.OutOfBoundsException) {
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

        fun parseBinExpr(r: KorteListReader<Token>): KorteExprNode {
            var result = parseFinal(r)
            while (r.hasMore) {
                //if (r.peek() !is ExprNode.Token.TOperator || r.peek().text !in ExprNode.BINOPS) break
                if (r.peek().text !in KorteExprNode.BINOPS) break
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

        fun parseTernaryExpr(r: KorteListReader<Token>): KorteExprNode {
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

        fun parseExpr(r: KorteListReader<Token>): KorteExprNode {
            return parseTernaryExpr(r)
        }

        private fun parseFinal(r: KorteListReader<Token>): KorteExprNode {
            if (!r.hasMore) r.prevOrContext().exception("Expected expression")
            val tok = r.peek().text.uppercase()
            var construct: KorteExprNode = when (tok) {
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
                    val result = KorteExprNode.parseExpr(r)
                    if (r.read().text != ")") throw RuntimeException("Expected ')'")
                    UNOP(result, "")
                }
                // Array literal
                "[" -> {
                    r.read()
                    val items = arrayListOf<KorteExprNode>()
                    loop@ while (r.hasMore && r.peek().text != "]") {
                        items += KorteExprNode.parseExpr(r)
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
                    val items = arrayListOf<Pair<KorteExprNode, KorteExprNode>>()
                    loop@ while (r.hasMore && r.peek().text != "}") {
                        val k = KorteExprNode.parseFinal(r)
                        r.expect(":")
                        val v = KorteExprNode.parseExpr(r)
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
                    if (r.peek() is KorteExprNode.Token.TNumber) {
                        val ntext = r.read().text
                        when (ntext.toDouble()) {
                            ntext.toIntOrNull()?.toDouble() -> LIT(ntext.toIntOrNull() ?: 0)
                            ntext.toLongOrNull()?.toDouble() -> LIT(ntext.toLongOrNull() ?: 0L)
                            else -> LIT(ntext.toDoubleOrNull() ?: 0.0)
                        }
                    }
                    // String
                    else if (r.peek() is KorteExprNode.Token.TString) {
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
                        val expr = KorteExprNode.parseExpr(r)
                        construct = ACCESS(construct, expr)
                        val end = r.read()
                        if (end.text != "]") end.exception("Expected ']' but found $end")
                    }

                    "|" -> {
                        val tok = r.read()
                        val name = r.tryRead()?.text ?: ""
                        val args = arrayListOf<KorteExprNode>()
                        if (name.isEmpty()) tok.exception("Missing filter name")
                        if (r.hasMore) {
                            when (r.peek().text) {
                                // jekyll/liquid syntax
                                ":" -> {
                                    r.read()
                                    callargsloop@ while (r.hasMore) {
                                        args += KorteExprNode.parseExpr(r)
                                        if (r.hasMore && r.peek().text == ",") r.read()
                                    }
                                }
                                // twig syntax
                                "(" -> {
                                    r.read()
                                    callargsloop@ while (r.hasMore && r.peek().text != ")") {
                                        args += KorteExprNode.parseExpr(r)
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
                        val args = arrayListOf<KorteExprNode>()
                        callargsloop@ while (r.hasMore && r.peek().text != ")") {
                            args += KorteExprNode.parseExpr(r)
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

    interface Token : KorteTokenContext {
        val text: String

        data class TId(override val text: String) : KorteExprNode.Token, KorteTokenContext by KorteTokenContext.Mixin()
        data class TNumber(override val text: String) : KorteExprNode.Token, KorteTokenContext by KorteTokenContext.Mixin()
        data class TString(override val text: String, val processedValue: String) : KorteExprNode.Token, KorteTokenContext by KorteTokenContext.Mixin()
        data class TOperator(override val text: String) : KorteExprNode.Token, KorteTokenContext by KorteTokenContext.Mixin()
        data class TEnd(override val text: String = "") : KorteExprNode.Token, KorteTokenContext by KorteTokenContext.Mixin()

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

            fun KorteExprNode.Token.annotate(context: KorteFilePosContext, tpos: Int) = this.apply {
                pos = context.pos + tpos
                file = context.file
            }

            fun tokenize(str: String, context: KorteFilePosContext): KorteListReader<Token> {
                val r = KorteStrReader(str)
                val out = arrayListOf<KorteExprNode.Token>()
                fun emit(str: KorteExprNode.Token, tpos: Int) {
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
                            if (r.peekChar() == '.' && r.peek(2)[1].isDigit()) {
                                r.skip()
                                val decimalPart = r.readWhile(Char::isLetterDigitOrUnderscore)
                                emit(KorteExprNode.Token.TNumber("$id.$decimalPart"), dstart)
                            } else {
                                emit(KorteExprNode.Token.TNumber(id), dstart)
                            }
                        } else {
                            emit(KorteExprNode.Token.TId(id), dstart)
                        }
                    }
                    r.skipSpaces()
                    val dstart2 = r.pos
                    if (r.peek(3) in OPERATORS) emit(TOperator(r.read(3)), dstart2)
                    if (r.peek(2) in OPERATORS) emit(TOperator(r.read(2)), dstart2)
                    if (r.peek(1) in OPERATORS) emit(TOperator(r.read(1)), dstart2)
                    if (r.peekChar() == '\'' || r.peekChar() == '"') {
                        val dstart3 = r.pos
                        val strStart = r.read()
                        val strBody = r.readUntil(strStart) ?: context.withPosAdd(dstart3).exception("String literal not closed")
                        val strEnd = r.read()
                        emit(KorteExprNode.Token.TString(strStart + strBody + strEnd, strBody.unescape()), dstart3)
                    }
                    val end = r.pos
                    if (end == start) {
                        context.withPosAdd(end).exception("Don't know how to handle '${r.peekChar()}'")
                    }
                }
                val dstart = r.pos
                //emit(ExprNode.Token.TEnd(), dstart)
                return KorteListReader(out, TEnd().annotate(context, dstart))
            }
        }
    }
}

fun KorteListReader<KorteExprNode.Token>.expectEnd() {
    if (hasMore) peek().exception("Unexpected token '${peek().text}'")
}

fun KorteListReader<KorteExprNode.Token>.tryRead(vararg types: String): KorteExprNode.Token? {
    val token = this.peek()
    if (token.text in types) {
        this.read()
        return token
    } else {
        return null
    }
}

fun KorteListReader<KorteExprNode.Token>.expectPeek(vararg types: String): KorteExprNode.Token {
    val token = this.peek()
    if (token.text !in types) throw RuntimeException("Expected ${types.joinToString(", ")} but found '${token.text}'")
    return token
}

fun KorteListReader<KorteExprNode.Token>.expect(vararg types: String): KorteExprNode.Token {
    val token = this.read()
    if (token.text !in types) throw RuntimeException("Expected ${types.joinToString(", ")}")
    return token
}

fun KorteListReader<KorteExprNode.Token>.parseExpr() = KorteExprNode.parseExpr(this)
fun KorteListReader<KorteExprNode.Token>.parseId() = KorteExprNode.parseId(this)
fun KorteListReader<KorteExprNode.Token>.parseIdList(): List<String> {
    val ids = arrayListOf<String>()
    do {
        ids += parseId()
    } while (tryRead(",") != null)
    return ids
}
