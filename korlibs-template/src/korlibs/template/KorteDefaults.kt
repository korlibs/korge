package korlibs.template

import korlibs.template.dynamic.*
import korlibs.template.internal.*
import korlibs.util.*
import kotlin.coroutines.cancellation.*
import kotlin.math.*

//@Suppress("unused")
object KorteDefaultFilters {
    val Capitalize = KorteFilter("capitalize") { subject.toDynamicString().lowercase().capitalize() }
    val Join = KorteFilter("join") {
        subject.toDynamicList().joinToString(args[0].toDynamicString()) { it.toDynamicString() }
    }
    val First = KorteFilter("first") { subject.toDynamicList().firstOrNull() }
    val Last = KorteFilter("last") { subject.toDynamicList().lastOrNull() }
    val Split = KorteFilter("split") { subject.toDynamicString().split(args[0].toDynamicString())  }
    val Concat = KorteFilter("concat") { subject.toDynamicString() + args[0].toDynamicString()  }
    val Length = KorteFilter("length") { subject.dynamicLength() }
    val Quote = KorteFilter("quote") { subject.toDynamicString().quote() }
    val Raw = KorteFilter("raw") { KorteRawString(subject.toDynamicString()) }
    val Replace = KorteFilter("replace") { subject.toDynamicString().replace(args[0].toDynamicString(), args[1].toDynamicString()) }
    val Reverse =
        KorteFilter("reverse") { (subject as? String)?.reversed() ?: subject.toDynamicList().reversed() }

    val Slice = KorteFilter("slice") {
        val lengthArg = args.getOrNull(1)
        val start = args.getOrNull(0).toDynamicInt()
        val length = lengthArg?.toDynamicInt() ?: subject.dynamicLength()
        if (subject is String) {
            val str = subject.toDynamicString()
            str.slice(start.coerceIn(0, str.length) until (start + length).coerceIn(0, str.length))
        } else {
            val list = subject.toDynamicList()
            list.slice(start.coerceIn(0, list.size) until (start + length).coerceIn(0, list.size))
        }
    }

    val Sort = KorteFilter("sort") {
        if (args.isEmpty()) {
            subject.toDynamicList().sortedBy { it.toDynamicString() }
        } else {
            subject.toDynamicList()
                .map { it to KorteDynamic2.accessAny(it, args[0], mapper).toDynamicString() }
                .sortedBy { it.second }
                .map { it.first }
        }
    }
    val Trim = KorteFilter("trim") { subject.toDynamicString().trim() }

    val Lower = KorteFilter("lower") { subject.toDynamicString().lowercase() }
    val Upper = KorteFilter("upper") { subject.toDynamicString().uppercase() }
    val Downcase = KorteFilter("downcase") { subject.toDynamicString().lowercase() }
    val Upcase = KorteFilter("upcase") { subject.toDynamicString().uppercase() }

    val Merge = KorteFilter("merge") {
        val arg = args.getOrNull(0)
        subject.toDynamicList() + arg.toDynamicList()
    }
    val JsonEncode = KorteFilter("json_encode") {
        Json_stringify(subject)
    }
    val Format = KorteFilter("format") {
        subject.toDynamicString().format(*(args.toTypedArray() as Array<out Any>))
    }
    // EXTRA from Kotlin
    val Chunked = KorteFilter("chunked") {
        subject.toDynamicList().chunked(args[0].toDynamicInt())
    }
    val WhereExp = KorteFilter("where_exp") {
        val ctx = this.context
        val list = this.subject.toDynamicList()
        val itemName = if (args.size >= 2) args[0].toDynamicString() else "it"
        val itemExprStr = args.last().toDynamicString()
        val itemExpr = KorteExprNode.parse(itemExprStr, KorteFilePosContext(KorteFileContext("", itemExprStr), 0))

        ctx.createScope {
            list.filter {
                ctx.scope.set(itemName, it)
                itemExpr.eval(ctx).toDynamicBool()
            }
        }
    }
    val Where = KorteFilter("where") {
        val itemName = args[0]
        val itemValue = args[1]
        subject.toDynamicList().filter { KorteDynamic2.contains(KorteDynamic2.accessAny(it, itemName, mapper), itemValue) }

    }
    val Map = KorteFilter("map") {
        val key = this.args[0].toDynamicString()
        this.subject.toDynamicList().map { KorteDynamic2.accessAny(it, key, mapper) }
    }
    val Size = KorteFilter("size") { subject.dynamicLength() }
    val Uniq = KorteFilter("uniq") {
        this.toDynamicList().distinct()
    }

    val Abs = KorteFilter("abs") {
        val subject = subject
        when (subject) {
            is Int -> subject.absoluteValue
            is Double -> subject.absoluteValue
            is Long -> subject.absoluteValue
            else -> subject.toDynamicDouble().absoluteValue
        }
    }

    val AtMost = KorteFilter("at_most") {
        val l = subject.toDynamicNumber()
        val r = args[0].toDynamicNumber()
        if (l >= r) r else l
    }

    val AtLeast = KorteFilter("at_least") {
        val l = subject.toDynamicNumber()
        val r = args[0].toDynamicNumber()
        if (l <= r) r else l
    }

    val Ceil = KorteFilter("ceil") {
        ceil(subject.toDynamicNumber().toDouble()).toDynamicCastToType(subject)
    }
    val Floor = KorteFilter("floor") {
        floor(subject.toDynamicNumber().toDouble()).toDynamicCastToType(subject)
    }
    val Round = KorteFilter("round") {
        round(subject.toDynamicNumber().toDouble()).toDynamicCastToType(subject)
    }
    val Times = KorteFilter("times") {
        (subject.toDynamicDouble() * args[0].toDynamicDouble()).toDynamicCastToType(combineTypes(subject, args[0]))
    }
    val Modulo = KorteFilter("modulo") {
        (subject.toDynamicDouble() % args[0].toDynamicDouble()).toDynamicCastToType(combineTypes(subject, args[0]))
    }
    val DividedBy = KorteFilter("divided_by") {
        (subject.toDynamicDouble() / args[0].toDynamicDouble()).toDynamicCastToType(combineTypes(subject, args[0]))
    }
    val Minus = KorteFilter("minus") {
        (subject.toDynamicDouble() - args[0].toDynamicDouble()).toDynamicCastToType(combineTypes(subject, args[0]))
    }
    val Plus = KorteFilter("plus") {
        (subject.toDynamicDouble() + args[0].toDynamicDouble()).toDynamicCastToType(combineTypes(subject, args[0]))
    }
    val Default = KorteFilter("default") {
        if (subject == null || subject == false || subject == "") args[0] else subject
    }

    val ALL = listOf(
        // String
        Capitalize, Lower, Upper, Downcase, Upcase, Quote, Raw, Replace, Trim,
        // Array
        Join, Split, Concat, WhereExp, Where, First, Last, Map, Size, Uniq, Length, Chunked, Sort, Merge,
        // Array/String
        Reverse,  Slice,
        // Math
        Abs, AtMost, AtLeast, Ceil, Floor, Round, Times, Modulo, DividedBy, Minus, Plus,
        // Any
        JsonEncode, Format
    )
}

@Suppress("unused")
object KorteDefaultFunctions {
    val Cycle = KorteFunction("cycle") { args ->
        val list = args.getOrNull(0).toDynamicList()
        val index = args.getOrNull(1).toDynamicInt()
        list[index umod list.size]
    }

    val Range = KorteFunction("range") { args ->
        val left = args.getOrNull(0)
        val right = args.getOrNull(1)
        val step = (args.getOrNull(2) ?: 1).toDynamicInt()
        if (left is Number || right is Number) {
            val l = left.toDynamicInt()
            val r = right.toDynamicInt()
            ((l..r) step step).toList()
        } else {
            TODO("Unsupported '$left'/'$right' for ranges")
        }
    }


    val Parent = KorteFunction("parent") {
        //ctx.tempDropTemplate {
        val blockName = currentBlock?.name

        if (blockName != null) {
            captureRaw {
                currentBlock?.parent?.eval(this)
            }
        } else {
            ""
        }
    }

    val ALL = listOf(Cycle, Range, Parent)
}

@Suppress("unused")
object KorteDefaultTags {
    val BlockTag = KorteTag("block", setOf(), setOf("end", "endblock")) {
        val part = chunks.first()
        val tr = part.tag.tokens
        val name = KorteExprNode.parseId(tr)
        if (name.isEmpty()) throw IllegalArgumentException("block without name")
        val contentType = if (tr.hasMore) KorteExprNode.parseId(tr) else null
        tr.expectEnd()
        context.template.addBlock(name, part.body)
        DefaultBlocks.BlockBlock(name, contentType ?: this.context.template.templateContent.contentType)
    }

    val Capture = KorteTag("capture", setOf(), setOf("end", "endcapture")) {
        val main = chunks[0]
        val tr = main.tag.tokens
        val varname = KorteExprNode.parseId(tr)
        val contentType = if (tr.hasMore) KorteExprNode.parseId(tr) else null
        tr.expectEnd()
        DefaultBlocks.BlockCapture(varname, main.body, contentType)
    }

    val Debug = KorteTag("debug", setOf(), null) {
        DefaultBlocks.BlockDebug(chunks[0].tag.expr)
    }

    val Empty = KorteTag("", setOf(""), null) {
        KorteBlock.group(chunks.map { it.body })
    }

    val Extends = KorteTag("extends", setOf(), null) {
        val part = chunks.first()
        val parent = KorteExprNode.parseExpr(part.tag.tokens)
        DefaultBlocks.BlockExtends(parent)
    }

    val For = KorteTag("for", setOf("else"), setOf("end", "endfor")) {
        val main = chunks[0]
        val elseTag = chunks.getOrNull(1)?.body
        val tr = main.tag.tokens
        val varnames = arrayListOf<String>()
        do {
            varnames += KorteExprNode.parseId(tr)
        } while (tr.tryRead(",") != null)
        KorteExprNode.expect(tr, "in")
        val expr = KorteExprNode.parseExpr(tr)
        tr.expectEnd()
        DefaultBlocks.BlockFor(varnames, expr, main.body, elseTag)
    }

    fun KorteTag.BuildContext.BuildIf(isIf: Boolean): KorteBlock {
        class Branch(val part: KorteTag.Part) {
            val expr get() = part.tag.expr
            val body get() = part.body
            val realExpr get() = if (part.tag.name.contains("unless")) {
                KorteExprNode.UNOP(expr, "!")
            } else {
                expr
            }
        }

        val branches = arrayListOf<Branch>()
        var elseBranch: KorteBlock? = null

        for (part in chunks) {
            when (part.tag.name) {
                "if", "elseif", "elsif", "unless", "elseunless" -> branches += Branch(part)
                "else" -> elseBranch = part.body
            }
        }


        val branchesRev = branches.reversed()
        val firstBranch = branchesRev.first()

        var node: KorteBlock = DefaultBlocks.BlockIf(firstBranch.realExpr, firstBranch.body, elseBranch)
        for (branch in branchesRev.takeLast(branchesRev.size - 1)) {
            node = DefaultBlocks.BlockIf(branch.realExpr, branch.body, node)
        }

        return node
    }

    val If = KorteTag("if", setOf("else", "elseif", "elsif", "elseunless"), setOf("end", "endif")) { BuildIf(isIf = true) }
    val Unless = KorteTag("unless", setOf("else", "elseif", "elsif", "elseunless"), setOf("end", "endunless")) { BuildIf(isIf = true) }

    val Import = KorteTag("import", setOf(), null) {
        val part = chunks.first()
        val s = part.tag.tokens
        val file = s.parseExpr()
        s.expect("as")
        val name = s.read().text
        s.expectEnd()
        DefaultBlocks.BlockImport(file, name)
    }

    val Include = KorteTag("include", setOf(), null) {
        val main = chunks.first()
        val tr = main.tag.tokens
        val expr = KorteExprNode.parseExpr(tr)
        val params = linkedMapOf<String, KorteExprNode>()
        while (tr.hasMore) {
            val id = KorteExprNode.parseId(tr)
            tr.expect("=")
            val expr = KorteExprNode.parseExpr(tr)
            params[id] = expr
        }
        tr.expectEnd()
        DefaultBlocks.BlockInclude(expr, params, main.tag.posContext, main.tag.content)
    }

    val Macro = KorteTag("macro", setOf(), setOf("end", "endmacro")) {
        val part = chunks[0]
        val s = part.tag.tokens
        val funcname = s.parseId()
        s.expect("(")
        val params = s.parseIdList()
        s.expect(")")
        s.expectEnd()
        DefaultBlocks.BlockMacro(funcname, params, part.body)
    }

    val Set = KorteTag("set", setOf(), null) {
        val main = chunks[0]
        val tr = main.tag.tokens
        val varname = KorteExprNode.parseId(tr)
        KorteExprNode.expect(tr, "=")
        val expr = KorteExprNode.parseExpr(tr)
        tr.expectEnd()
        DefaultBlocks.BlockSet(varname, expr)
    }

    val Assign = KorteTag("assign", setOf(), null) {
        Set.buildNode(this)
    }

    val Switch = KorteTag("switch", setOf("case", "default"), setOf("end", "endswitch")) {
        var subject: KorteExprNode? = null
        val cases = arrayListOf<Pair<KorteExprNode, KorteBlock>>()
        var defaultCase: KorteBlock? = null

        for (part in this.chunks) {
            val body = part.body
            when (part.tag.name) {
                "switch" -> subject = part.tag.expr
                "case" -> cases += part.tag.expr to body
                "default" -> defaultCase = body
            }
        }
        if (subject == null) error("No subject set in switch")
        //println(this.chunks)
        object : KorteBlock {
            override suspend fun eval(context: KorteTemplate.EvalContext) {
                val subjectValue = subject.eval(context)
                for ((case, block) in cases) {
                    if (subjectValue == case.eval(context)) {
                        block.eval(context)
                        return
                    }
                }
                defaultCase?.eval(context)
                return
            }
        }
    }

    val ALL = listOf(
        BlockTag,
        Capture, Debug,
        Empty, Extends, For, If, Unless, Switch, Import, Include, Macro, Set,
        // Liquid
        Assign
    )
}

var KorteTemplateConfig.debugPrintln by korteExtraProperty({ extra }) { { v: Any? -> println(v) } }

object DefaultBlocks {
    data class BlockBlock(val name: String, val contentType: String?) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            //val oldBlock = context.currentBlock
            //try {
            //	val block = context.leafTemplate.getBlock(name)
            //	context.currentBlock = block
            //	block.block.eval(context)
            //} finally {
            //	context.currentBlock = oldBlock
            //}
            context.leafTemplate.getBlock(name).eval(context)
        }
    }

    data class BlockCapture(val varname: String, val content: KorteBlock, val contentType: String? = null) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            val result = context.capture {
                content.eval(context)
            }
            context.scope.set(varname, KorteRawString(result, contentType))
        }
    }

    data class BlockDebug(val expr: KorteExprNode) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            context.config.debugPrintln(expr.eval(context))
        }
    }

    data class BlockExpr(val expr: KorteExprNode) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            context.config.writeBlockExpressionResult(context, expr.eval(context))
        }
    }

    data class BlockExtends(val expr: KorteExprNode) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            val result = expr.eval(context)
            val parentTemplate = KorteTemplate.TemplateEvalContext(context.templates.getLayout(result.toDynamicString()))
            context.currentTemplate.parent = parentTemplate
            parentTemplate.eval(context)
            throw KorteTemplate.StopEvaluatingException()
            //context.template.parent
        }
    }

    data class BlockFor(val varnames: List<String>, val expr: KorteExprNode, val loop: KorteBlock, val elseNode: KorteBlock?) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            context.createScope {
                var index = 0
                val items = expr.eval(context).toDynamicList()
                val loopValue = hashMapOf<String, Any?>()
                context.scope.set("loop", loopValue)
                loopValue["length"] = items.size
                for (v in items) {
                    if (v is Pair<*, *> && varnames.size >= 2) {
                        context.scope.set(varnames[0], v.first)
                        context.scope.set(varnames[1], v.second)
                    } else {
                        context.scope.set(varnames[0], v)
                    }
                    loopValue["index"] = index + 1
                    loopValue["index0"] = index
                    loopValue["revindex"] = items.size - index - 1
                    loopValue["revindex0"] = items.size - index
                    loopValue["first"] = (index == 0)
                    loopValue["last"] = (index == items.size - 1)
                    loop.eval(context)
                    index++
                }
                if (index == 0) {
                    elseNode?.eval(context)
                }
            }
        }
    }

    data class BlockGroup(val children: List<KorteBlock>) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            for (n in children) n.eval(context)
        }
    }

    data class BlockIf(val cond: KorteExprNode, val trueContent: KorteBlock, val falseContent: KorteBlock?) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            if (cond.eval(context).toDynamicBool()) {
                trueContent.eval(context)
            } else {
                falseContent?.eval(context)
            }
        }
    }

    data class BlockImport(val fileExpr: KorteExprNode, val exportName: String) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            val ctx =
                KorteTemplate.TemplateEvalContext(context.templates.getInclude(fileExpr.eval(context).toString())).exec()
                    .context
            context.scope.set(exportName, ctx.macros)
        }
    }

    data class BlockInclude(
        val fileNameExpr: KorteExprNode,
        val params: LinkedHashMap<String, KorteExprNode>,
        val filePos: KorteFilePosContext,
        val tagContent: String
    ) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            val fileName = fileNameExpr.eval(context).toDynamicString()
            val evalParams = params.mapValues { it.value.eval(context) }.toMutableMap()
            context.createScope {
                context.scope.set("include", evalParams)
                val includeTemplate = try {
                    context.templates.getInclude(fileName)
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    korteException("Can't include template ($tagContent): ${e.message}", filePos)
                }
                KorteTemplate.TemplateEvalContext(includeTemplate).eval(context)
            }
        }
    }

    data class BlockMacro(val funcname: String, val args: List<String>, val body: KorteBlock) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            context.macros[funcname] = KorteTemplate.Macro(funcname, args, body)
        }
    }

    data class BlockSet(val varname: String, val expr: KorteExprNode) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            context.scope.set(varname, expr.eval(context))
        }
    }

    data class BlockText(val content: String) : KorteBlock {
        override suspend fun eval(context: KorteTemplate.EvalContext) {
            context.write(content)
        }
    }
}
