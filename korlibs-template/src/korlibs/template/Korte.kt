package korlibs.template

import korlibs.io.serialization.yaml.*
import korlibs.template.dynamic.*
import korlibs.template.internal.*
import korlibs.template.util.*
import kotlin.collections.set

open class KorteTemplates(
    var root: KorteNewTemplateProvider,
    var includes: KorteNewTemplateProvider = root,
    var layouts: KorteNewTemplateProvider = root,
    val config: KorteTemplateConfig = KorteTemplateConfig(),
    var cache: Boolean = true
) {
    @PublishedApi
    internal val tcache = KorteAsyncCache()

    fun invalidateCache() {
        tcache.invalidateAll()
    }

    @PublishedApi
    internal suspend fun cache(name: String, callback: suspend () -> KorteTemplate): KorteTemplate = when {
        cache -> tcache.call(name) { callback() }
        else -> callback()
    }

    open suspend fun getInclude(name: String): KorteTemplate = cache("include/$name") {
        KorteTemplate(name, this@KorteTemplates, includes.newGetSure(name), config).init()
    }

    open suspend fun getLayout(name: String): KorteTemplate = cache("layout/$name") {
        KorteTemplate(name, this@KorteTemplates, layouts.newGetSure(name), config).init()
    }

    open suspend fun get(name: String): KorteTemplate = cache("base/$name") {
        KorteTemplate(name, this@KorteTemplates, root.newGetSure(name), config).init()
    }

    suspend fun render(name: String, vararg args: Pair<String, Any?>): String = get(name).invoke(*args)
    suspend fun render(name: String, args: Any?): String {
        val template = get(name)
        val renderered = template(args)
        return renderered
    }
    suspend fun prender(name: String, vararg args: Pair<String, Any?>): KorteAsyncTextWriterContainer =
        get(name).prender(*args)

    suspend fun prender(name: String, args: Any?): KorteAsyncTextWriterContainer = get(name).prender(args)
}

open class KorteTemplateContent(
    val text: String,
    val contentType: String? = null,
    val chunkProcessor: ((String) -> String) = { it }
)

class KorteTemplate internal constructor(
    val name: String,
    val templates: KorteTemplates,
    val templateContent: KorteTemplateContent,
    val config: KorteTemplateConfig = KorteTemplateConfig()
) {
    val template get() = templateContent.text
    // @TODO: Move to parse plugin + extra
    var frontMatter: Map<String, Any?>? = null

    val blocks = hashMapOf<String, KorteBlock>()
    val parseContext = ParseContext(this, config)
    val templateTokens = KorteToken.tokenize(template, KorteFilePosContext(KorteFileContext(name, template), 0))
    lateinit var rootNode: KorteBlock; private set

    suspend fun init(): KorteTemplate {
        rootNode = KorteBlock.parse(templateTokens, parseContext)
        // @TODO: Move to parse plugin + extra
        if (frontMatter != null) {
            val layout = frontMatter?.get("layout")
            if (layout != null) {
                rootNode = DefaultBlocks.BlockGroup(
                    listOf(
                        DefaultBlocks.BlockCapture("content", rootNode, templateContent.contentType),
                        DefaultBlocks.BlockExtends(KorteExprNode.LIT(layout))
                    )
                )
            }
        }
        return this
    }

    class ParseContext(val template: KorteTemplate, val config: KorteTemplateConfig) {
        val templates: KorteTemplates get() = template.templates
    }

    class Scope(val map: Any?, val mapper: KorteObjectMapper2, val parent: KorteTemplate.Scope? = null) : KorteDynamicContext {
        // operator
        suspend fun get(key: Any?): Any? = KorteDynamic2.accessAny(map, key, mapper) ?: parent?.get(key)

        // operator
        suspend fun set(key: Any?, value: Any?) {
            KorteDynamic2.setAny(map, key, value, mapper)
        }
    }

    data class ExecResult(val context: KorteTemplate.EvalContext, val str: String)

    interface DynamicInvokable {
        suspend fun invoke(ctx: KorteTemplate.EvalContext, args: List<Any?>): Any?
    }

    class Macro(val name: String, val argNames: List<String>, val code: KorteBlock) : DynamicInvokable {
        override suspend fun invoke(ctx: KorteTemplate.EvalContext, args: List<Any?>): Any? {
            return ctx.createScope {
                for ((key, value) in this.argNames.zip(args)) {
                    ctx.scope.set(key, value)
                }
                KorteRawString(ctx.capture {
                    code.eval(ctx)
                })
            }
        }
    }

    data class BlockInTemplateEval(val name: String, val block: KorteBlock, val template: TemplateEvalContext) {
        val parent: BlockInTemplateEval?
            get() {
                return template.parent?.getBlockOrNull(name)
            }

        suspend fun eval(ctx: EvalContext) = ctx.setTempTemplate(template) {
            val oldBlock = ctx.currentBlock
            try {
                ctx.currentBlock = this
                return@setTempTemplate block.eval(ctx)
            } finally {
                ctx.currentBlock = oldBlock
            }
        }
    }

    class TemplateEvalContext(val template: KorteTemplate) {
        val name: String = template.name
        val templates: KorteTemplates get() = template.templates

        var parent: TemplateEvalContext? = null
        val root: TemplateEvalContext get() = parent?.root ?: this

        fun getBlockOrNull(name: String): BlockInTemplateEval? =
            template.blocks[name]?.let { BlockInTemplateEval(name, it, this@TemplateEvalContext) }
                ?: parent?.getBlockOrNull(name)

        fun getBlock(name: String): BlockInTemplateEval =
            getBlockOrNull(name) ?: BlockInTemplateEval(name, DefaultBlocks.BlockText(""), this)

        class WithArgs(val context: TemplateEvalContext, val args: Any?, val mapper: KorteObjectMapper2, val parentScope: KorteTemplate.Scope? = null) :
            KorteAsyncTextWriterContainer {
            override suspend fun write(writer: suspend (String) -> Unit) {
                context.exec2(args, mapper, parentScope, writer)
            }
        }

        fun withArgs(args: Any?, mapper: KorteObjectMapper2 = KorteMapper2, parentScope: KorteTemplate.Scope? = null) = WithArgs(this, args, mapper, parentScope)

        suspend fun exec2(args: Any?, mapper: KorteObjectMapper2, parentScope: KorteTemplate.Scope? = null, writer: suspend (String) -> Unit): KorteTemplate.EvalContext {
            val scope = Scope(args, mapper, parentScope)
            if (template.frontMatter != null) for ((k, v) in template.frontMatter!!) scope.set(k, v)
            val context = KorteTemplate.EvalContext(this, scope, template.config, mapper = mapper, write = writer)
            eval(context)
            return context
        }

        suspend fun exec(args: Any?, mapper: KorteObjectMapper2 = KorteMapper2, parentScope: KorteTemplate.Scope? = null): ExecResult {
            val str = StringBuilder()
            val scope = Scope(args, mapper, parentScope)
            if (template.frontMatter != null) for ((k, v) in template.frontMatter!!) scope.set(k, v)
            val context = KorteTemplate.EvalContext(this, scope, template.config, mapper, write = { str.append(it) })
            eval(context)
            return ExecResult(context, str.toString())
        }

        suspend fun exec(vararg args: Pair<String, Any?>, mapper: KorteObjectMapper2 = KorteMapper2, parentScope: KorteTemplate.Scope? = null): ExecResult =
            exec(hashMapOf(*args), mapper, parentScope)

        operator suspend fun invoke(args: Any?, mapper: KorteObjectMapper2 = KorteMapper2, parentScope: KorteTemplate.Scope? = null): String = exec(args, mapper, parentScope).str
        operator suspend fun invoke(vararg args: Pair<String, Any?>, mapper: KorteObjectMapper2 = KorteMapper2, parentScope: KorteTemplate.Scope? = null): String =
            exec(hashMapOf(*args), mapper, parentScope).str

        suspend fun eval(context: KorteTemplate.EvalContext) {
            try {
                context.setTempTemplate(this) {
                    context.createScope { template.rootNode.eval(context) }
                }
            } catch (e: StopEvaluatingException) {
            }
        }
    }

    class StopEvaluatingException : Exception()

    class EvalContext(
        var currentTemplate: TemplateEvalContext,
        var scope: KorteTemplate.Scope,
        val config: KorteTemplateConfig,
        val mapper: KorteObjectMapper2,
        var write: suspend (str: String) -> Unit
    ) : KorteDynamicContext {
        val leafTemplate: TemplateEvalContext = currentTemplate
        val templates = currentTemplate.templates
        val macros = hashMapOf<String, Macro>()
        var currentBlock: BlockInTemplateEval? = null

        internal val filterCtxPool = Pool { KorteFilter.Ctx() }

        inline fun <T> setTempTemplate(template: TemplateEvalContext, callback: () -> T): T {
            val oldTemplate = this.currentTemplate
            try {
                this.currentTemplate = template
                return callback()
            } finally {
                this.currentTemplate = oldTemplate
            }
        }

        inline fun capture(callback: () -> Unit): String = this.run {
            var out = ""
            val old = write
            try {
                write = { out += it }
                callback()
            } finally {
                write = old
            }
            out
        }

        inline fun captureRaw(callback: () -> Unit): KorteRawString = KorteRawString(capture(callback))

        inline fun <T> createScope(content: MutableMap<*, *> = LinkedHashMap<Any?, Any?>(), callback: () -> T): T {
            val old = this.scope
            try {
                this.scope = KorteTemplate.Scope(content, mapper, old)
                return callback()
            } finally {
                this.scope = old
            }
        }
    }

    fun addBlock(name: String, body: KorteBlock) {
        blocks[name] = body
    }

    //suspend operator fun invoke(hashMap: Any?, mapper: ObjectMapper2 = Mapper2): String = Template.TemplateEvalContext(this).invoke(hashMap, mapper = mapper)
    //suspend operator fun invoke(vararg args: Pair<String, Any?>, mapper: ObjectMapper2 = Mapper2): String = Template.TemplateEvalContext(this).invoke(*args, mapper = mapper)

    suspend fun createEvalContext() = KorteTemplate.TemplateEvalContext(this)
    suspend operator fun invoke(hashMap: Any?, mapper: KorteObjectMapper2 = KorteMapper2): String =
        createEvalContext().invoke(hashMap, mapper = mapper)

    suspend operator fun invoke(vararg args: Pair<String, Any?>, mapper: KorteObjectMapper2 = KorteMapper2): String =
        createEvalContext().invoke(*args, mapper = mapper)

    suspend fun prender(vararg args: Pair<String, Any?>, mapper: KorteObjectMapper2 = KorteMapper2): KorteAsyncTextWriterContainer {
        return createEvalContext().withArgs(HashMap(args.toMap()), mapper)
    }

    suspend fun prender(args: Any?, mapper: KorteObjectMapper2 = KorteMapper2): KorteAsyncTextWriterContainer {
        return createEvalContext().withArgs(args, mapper)
    }
}

suspend fun KorteTemplate(
    template: String,
    templates: KorteTemplates,
    includes: KorteNewTemplateProvider = templates.includes,
    layouts: KorteNewTemplateProvider = templates.layouts,
    config: KorteTemplateConfig = templates.config,
    cache: Boolean = templates.cache,
): KorteTemplate {
    val root = KorteTemplateProvider(mapOf("template" to template))
    return KorteTemplates(
        root = root,
        includes = includes,
        layouts = layouts,
        config = config,
        cache = cache,
    ).get("template")
}

suspend fun KorteTemplate(template: String, config: KorteTemplateConfig = KorteTemplateConfig()): KorteTemplate = KorteTemplates(
    KorteTemplateProvider(mapOf("template" to template)),
    config = config
).get("template")

open class KorteTemplateConfig(
    extraTags: List<KorteTag> = listOf(),
    extraFilters: List<KorteFilter> = listOf(),
    extraFunctions: List<KorteFunction> = listOf(),
    var unknownFilter: KorteFilter = KorteFilter("unknown") { tok.exception("Unknown filter '$name'") },
    val autoEscapeMode: KorteAutoEscapeMode = KorteAutoEscapeMode.HTML,
    // Here we can convert markdown into html if required. This is available at the template level + content + named blocks
    val contentTypeProcessor: (content: String, contentType: String?) -> String = { content, _ -> content },
    val frontMatterParser: (String) -> Any? = { Yaml.decode(it) },
    @Suppress("UNUSED_PARAMETER") dummy: Unit = Unit, // To avoid tailing lambda
) {
    val extra = LinkedHashMap<String, Any>()

    val integratedFunctions = KorteDefaultFunctions.ALL
    val integratedFilters = KorteDefaultFilters.ALL
    val integratedTags = KorteDefaultTags.ALL

    private val allFunctions = integratedFunctions + extraFunctions
    private val allTags = integratedTags + extraTags
    private val allFilters = integratedFilters + extraFilters

    val tags = hashMapOf<String, KorteTag>().apply {
        for (tag in allTags) {
            this[tag.name] = tag
            for (alias in tag.aliases) this[alias] = tag
        }
    }

    val filters = hashMapOf<String, KorteFilter>().apply {
        for (filter in allFilters) this[filter.name] = filter
    }

    val functions = hashMapOf<String, KorteFunction>().apply {
        for (func in allFunctions) this[func.name] = func
    }

    fun register(vararg its: KorteTag) = this.apply { for (it in its) tags[it.name] = it }
    fun register(vararg its: KorteFilter) = this.apply { for (it in its) filters[it.name] = it }
    fun register(vararg its: KorteFunction) = this.apply { for (it in its) functions[it.name] = it }

    var variableProcessor: KorteVariableProcessor = { name ->
        scope.get(name)
    }

    fun replaceVariablePocessor(func: suspend KorteTemplate.EvalContext.(name: String, previous: KorteVariableProcessor) -> Any?) {
        val previous = variableProcessor
        variableProcessor = { eval ->
            this.func(eval, previous)
        }
    }

    var writeBlockExpressionResult: KorteWriteBlockExpressionResultFunction = { value ->
        this.write(when (value) {
            is KorteRawString -> contentTypeProcessor(value.str, value.contentType)
            else -> autoEscapeMode.transform(contentTypeProcessor(KorteDynamic2.toString(value), null))
        })
    }

    fun replaceWriteBlockExpressionResult(func: suspend KorteTemplate.EvalContext.(value: Any?, previous: KorteWriteBlockExpressionResultFunction) -> Unit) {
        val previous = writeBlockExpressionResult
        writeBlockExpressionResult = { eval ->
            this.func(eval, previous)
        }
    }
}

typealias KorteWriteBlockExpressionResultFunction = suspend KorteTemplate.EvalContext.(value: Any?) -> Unit
typealias KorteVariableProcessor = suspend KorteTemplate.EvalContext.(name: String) -> Any?

open class KorteTemplateConfigWithTemplates(
    extraTags: List<KorteTag> = listOf(),
    extraFilters: List<KorteFilter> = listOf(),
    extraFunctions: List<KorteFunction> = listOf()
) : KorteTemplateConfig(extraTags, extraFilters, extraFunctions) {
    var templates = KorteTemplates(KorteTemplateProvider(mapOf()), config = this)
    fun cache(value: Boolean) = this.apply { templates.cache = value }
    fun root(root: KorteNewTemplateProvider, includes: KorteNewTemplateProvider = root, layouts: KorteNewTemplateProvider = root) =
        this.apply {
            templates.root = root
            templates.includes = includes
            templates.layouts = layouts
        }
}

interface KorteNewTemplateProvider {
    suspend fun newGet(template: String): KorteTemplateContent?
}

interface KorteTemplateProvider : KorteNewTemplateProvider {
    class NotFoundException(val template: String) : RuntimeException("Can't find template '$template'")

    override suspend fun newGet(template: String): KorteTemplateContent? = get(template)?.let { KorteTemplateContent(it) }
    suspend fun get(template: String): String?
}

suspend fun KorteNewTemplateProvider.newGetSure(template: String) = newGet(template)
    ?: throw KorteTemplateProvider.NotFoundException(template)

suspend fun KorteTemplateProvider.getSure(template: String) = get(template)
    ?: throw KorteTemplateProvider.NotFoundException(template)

fun KorteTemplateProvider(map: Map<String, String>): KorteTemplateProvider = object : KorteTemplateProvider {
    override suspend fun get(template: String): String? = map[template]
}

fun KorteTemplateProvider(vararg map: Pair<String, String>): KorteTemplateProvider = KorteTemplateProvider(map.toMap())

fun KorteNewTemplateProvider(map: Map<String, KorteTemplateContent>): KorteNewTemplateProvider = object : KorteNewTemplateProvider {
    override suspend fun newGet(template: String): KorteTemplateContent? = map[template]
}
fun KorteNewTemplateProvider(vararg map: Pair<String, KorteTemplateContent>): KorteNewTemplateProvider = KorteNewTemplateProvider(map.toMap())

data class KorteTag(val name: String, val nextList: Set<String>, val end: Set<String>?, val aliases: List<String> = listOf(), val buildNode: suspend BuildContext.() -> KorteBlock) : KorteDynamicContext {
    data class Part(val tag: KorteToken.TTag, val body: KorteBlock)
    data class BuildContext(val context: KorteTemplate.ParseContext, val chunks: List<KorteTag.Part>)
}

class KorteRawString(val str: String, val contentType: String? = null) {
    override fun toString(): String = str
}

data class KorteFilter(val name: String, val eval: suspend Ctx.() -> Any?) {
    class Ctx : KorteDynamicContext {
        lateinit var context: KorteTemplate.EvalContext
        lateinit var tok: KorteExprNode.Token
        lateinit var name: String
        val mapper get() = context.mapper
        var subject: Any? = null
        var args: List<Any?> = listOf()
    }
}

data class KorteFunction(val name: String, val eval: suspend KorteTemplate.EvalContext.(args: List<Any?>) -> Any?) {
    suspend fun eval(args: List<Any?>, context: KorteTemplate.EvalContext) = eval.invoke(context, args)
}

open class KorteException(val msg: String, val context: KorteFilePosContext) : RuntimeException() {

    override val message: String get() = "$msg at $context"

    //override fun toString(): String = message
}

fun korteException(msg: String, context: KorteFilePosContext): Nothing = throw KorteException(msg, context)

interface KorteBlock : KorteDynamicContext {
    suspend fun eval(context: KorteTemplate.EvalContext)

    companion object {
        fun group(children: List<KorteBlock>): KorteBlock =
            if (children.size == 1) children[0] else DefaultBlocks.BlockGroup(children)

        class Parse(val tokens: List<KorteToken>, val parseContext: KorteTemplate.ParseContext) {
            val tr = KorteListReader(tokens, tokens.lastOrNull())

            suspend fun handle(tag: KorteTag, token: KorteToken.TTag): KorteBlock {
                val parts = arrayListOf<KorteTag.Part>()
                var currentToken = token
                val children = arrayListOf<KorteBlock>()

                fun emitPart() {
                    parts += KorteTag.Part(currentToken, group(children.toList()))
                }

                loop@ while (!tr.eof) {
                    val it = tr.read()
                    when (it) {
                        is KorteToken.TLiteral -> {
                            var text = it.content
                            // it.content.startsWith("---")
                            if (children.isEmpty() && it.content.startsWith("---")) {
                                val lines = it.content.split('\n')
                                if (lines[0] == "---") {
                                    val slines = lines.drop(1)
                                    val index = slines.indexOf("---")
                                    if (index >= 0) {
                                        val yamlLines = slines.slice(0 until index)
                                        val outside = slines.slice(index + 1 until slines.size)
                                        val yamlText = yamlLines.joinToString("\n")
                                        val yaml = parseContext.config.frontMatterParser(yamlText)
                                        if (yaml is Map<*, *>) {
                                            parseContext.template.frontMatter = yaml as Map<String, Any?>
                                        }
                                        text = outside.joinToString("\n")
                                    }
                                }
                            }
                            children += DefaultBlocks.BlockText(parseContext.template.templateContent.chunkProcessor(text))
                        }
                        is KorteToken.TExpr -> {
                            children += DefaultBlocks.BlockExpr(KorteExprNode.parse(it.content, it.posContext))
                        }
                        is KorteToken.TTag -> {
                            when (it.name) {
                                in (tag.end ?: setOf()) -> break@loop
                                in tag.nextList -> {
                                    emitPart()
                                    currentToken = it
                                    children.clear()
                                }
                                else -> {
                                    val newtag = parseContext.config.tags[it.name]
                                        ?: it.exception("Can't find tag ${it.name} with content ${it.content}")
                                    children += when {
                                        newtag.end != null -> handle(newtag, it)
                                        else -> newtag.buildNode(
                                            KorteTag.BuildContext(
                                                parseContext,
                                                listOf(KorteTag.Part(it, DefaultBlocks.BlockText("")))
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        else -> break@loop
                    }
                }

                emitPart()

                return tag.buildNode(KorteTag.BuildContext(parseContext, parts))
            }
        }

        suspend fun parse(tokens: List<KorteToken>, parseContext: KorteTemplate.ParseContext): KorteBlock {
            return Parse(tokens, parseContext).handle(KorteDefaultTags.Empty, KorteToken.TTag("", ""))
        }
    }
}

class KorteAutoEscapeMode(val transform: (String) -> String) {
    companion object {
        val HTML = KorteAutoEscapeMode { it.htmlspecialchars() }
        val RAW = KorteAutoEscapeMode { it }
    }
}

data class KorteFileContext(val fileName: String, val fileContent: String) {
    val lines by lazy { fileContent.split("\n") }
    val lineOffsets by lazy {
        ArrayList<Int>().apply {
            var offset = 0
            for (line in lines) {
                add(offset)
                offset += line.length
            }
            add(fileContent.length)
        }
    }
    fun findRow0At(pos: Int): Int {
        for (n in 0 until lineOffsets.size - 1) {
            val start = lineOffsets[n]
            val end = lineOffsets[n + 1]
            if (pos in start until end) return n
        }
        return -1
    }

    companion object {
        val DUMMY = KorteFileContext("unknown", "")
    }
}

data class KorteFilePosContext(val file: KorteFileContext, val pos: Int) {
    val fileName get() = file.fileName
    val fileContent get() = file.fileContent
    val row0: Int by lazy { file.findRow0At(pos) }
    val row get() = row0 + 1
    val column0 get() = pos - file.lineOffsets[row0]
    val column get() = column0 + 1

    fun withPosAdd(add: Int) = this.copy(pos = pos + add)

    fun exception(msg: String): Nothing = korteException(msg, this)

    override fun toString(): String = "$fileName:$row:$column"
}

interface KorteTokenContext {
    var file: KorteFileContext
    var pos: Int
    val posContext: KorteFilePosContext get() = KorteFilePosContext(file, pos)

    fun exception(msg: String): Nothing = posContext.exception(msg)

    class Mixin : KorteTokenContext {
        override var file: KorteFileContext = KorteFileContext.DUMMY
        override var pos: Int = -1
    }
}

sealed class KorteToken : KorteTokenContext {
    var trimLeft = false
    var trimRight = false

    data class TLiteral(val content: String) : KorteToken(), KorteTokenContext by KorteTokenContext.Mixin()
    data class TExpr(val content: String) : KorteToken(), KorteTokenContext by KorteTokenContext.Mixin()
    data class TTag(val name: String, val content: String) : KorteToken(), KorteTokenContext by KorteTokenContext.Mixin() {
        val tokens by lazy { KorteExprNode.Token.tokenize(content, posContext) }
        val expr by lazy { KorteExprNode.parse(this) }
    }

    companion object {
        // @TODO: Use StrReader
        fun tokenize(str: String, context: KorteFilePosContext): List<KorteToken> {
            val out = arrayListOf<KorteToken>()
            var lastPos = 0

            fun emit(token: KorteToken, pos: Int) {
                if (token is TLiteral && token.content.isEmpty()) return
                out += token
                token.file = context.file
                token.pos = context.pos + pos
            }

            var pos = 0
            loop@ while (pos < str.length) {
                val c = str[pos++]
                // {# {% {{ }} %} #}
                if (c == '{') {
                    if (pos >= str.length) break
                    val c2 = str[pos++]
                    when (c2) {
                        // Comment
                        '#' -> {
                            val startPos = pos - 2
                            if (lastPos != startPos) {
                                emit(TLiteral(str.substring(lastPos until startPos)), startPos)
                            }
                            val endCommentP1 = str.indexOf("#}", startIndex = pos)
                            val endComment = if (endCommentP1 >= 0) endCommentP1 + 2 else str.length
                            lastPos = endComment
                            pos = endComment
                        }
                        '{', '%' -> {
                            val startPos = pos - 2
                            val pos2 = if (c2 == '{') str.indexOf("}}", pos) else str.indexOf("%}", pos)
                            if (pos2 < 0) break@loop
                            val trimLeft = str[pos] == '-'
                            val trimRight = str[pos2 - 1] == '-'

                            val p1 = if (trimLeft) pos + 1 else pos
                            val p2 = if (trimRight) pos2 - 1 else pos2

                            val content = str.substring(p1, p2).trim()
                            if (lastPos != startPos) emit(TLiteral(str.substring(lastPos until startPos)), startPos)

                            val token = when (c2) {
                                '{' -> TExpr(content)
                                else -> {
                                    val parts = content.split(' ', limit = 2)
                                    TTag(parts[0], parts.getOrElse(1) { "" })
                                }
                            }
                            token.trimLeft = trimLeft
                            token.trimRight = trimRight
                            emit(token, p1)
                            pos = pos2 + 2
                            lastPos = pos
                        }
                    }
                }
            }
            emit(TLiteral(str.substring(lastPos, str.length)), lastPos)

            for ((n, cur) in out.withIndex()) {
                if (cur is KorteToken.TLiteral) {
                    val trimStart = out.getOrNull(n - 1)?.trimRight ?: false
                    val trimEnd = out.getOrNull(n + 1)?.trimLeft ?: false
                    out[n] = when {
                        trimStart && trimEnd -> TLiteral(cur.content.trim())
                        trimStart -> TLiteral(cur.content.trimStart())
                        trimEnd -> TLiteral(cur.content.trimEnd())
                        else -> cur
                    }
                }
            }

            return out
        }
    }
}
