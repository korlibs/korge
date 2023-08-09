package korlibs.template

import korlibs.template.dynamic.Dynamic2
import korlibs.template.dynamic.DynamicContext
import korlibs.template.dynamic.Mapper2
import korlibs.template.dynamic.ObjectMapper2
import korlibs.template.internal.Pool
import korlibs.template.util.AsyncTextWriterContainer
import kotlin.collections.set

open class TemplateContent(
    val text: String,
    val contentType: String? = null,
    val chunkProcessor: ((String) -> String) = { it }
)

class Template internal constructor(
    val name: String,
    val templates: Templates,
    val templateContent: TemplateContent,
    val config: TemplateConfig = TemplateConfig()
) {
    val template get() = templateContent.text
    // @TODO: Move to parse plugin + extra
    var frontMatter: Map<String, Any?>? = null

    val blocks = hashMapOf<String, Block>()
    val parseContext = ParseContext(this, config)
    val templateTokens = Token.tokenize(template, FilePosContext(FileContext(name, template), 0))
    lateinit var rootNode: Block; private set

    suspend fun init(): Template {
        rootNode = Block.parse(templateTokens, parseContext)
        // @TODO: Move to parse plugin + extra
        if (frontMatter != null) {
            val layout = frontMatter?.get("layout")
            if (layout != null) {
                rootNode = DefaultBlocks.BlockGroup(
                    listOf(
                        DefaultBlocks.BlockCapture("content", rootNode, templateContent.contentType),
                        DefaultBlocks.BlockExtends(ExprNode.LIT(layout))
                    )
                )
            }
        }
        return this
    }

    class ParseContext(val template: Template, val config: TemplateConfig) {
        val templates: Templates get() = template.templates
    }

    class Scope(val map: Any?, val mapper: ObjectMapper2, val parent: Template.Scope? = null) : DynamicContext {
        // operator
        suspend fun get(key: Any?): Any? = Dynamic2.accessAny(map, key, mapper) ?: parent?.get(key)

        // operator
        suspend fun set(key: Any?, value: Any?) {
            Dynamic2.setAny(map, key, value, mapper)
        }
    }

    data class ExecResult(val context: Template.EvalContext, val str: String)

    interface DynamicInvokable {
        suspend fun invoke(ctx: Template.EvalContext, args: List<Any?>): Any?
    }

    class Macro(val name: String, val argNames: List<String>, val code: Block) : DynamicInvokable {
        override suspend fun invoke(ctx: Template.EvalContext, args: List<Any?>): Any? {
            return ctx.createScope {
                for ((key, value) in this.argNames.zip(args)) {
                    ctx.scope.set(key, value)
                }
                RawString(ctx.capture {
                    code.eval(ctx)
                })
            }
        }
    }

    data class BlockInTemplateEval(val name: String, val block: Block, val template: TemplateEvalContext) {
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

    class TemplateEvalContext(val template: Template) {
        val name: String = template.name
        val templates: Templates get() = template.templates

        var parent: TemplateEvalContext? = null
        val root: TemplateEvalContext get() = parent?.root ?: this

        fun getBlockOrNull(name: String): BlockInTemplateEval? =
            template.blocks[name]?.let { BlockInTemplateEval(name, it, this@TemplateEvalContext) }
                ?: parent?.getBlockOrNull(name)

        fun getBlock(name: String): BlockInTemplateEval =
            getBlockOrNull(name) ?: BlockInTemplateEval(name, DefaultBlocks.BlockText(""), this)

        class WithArgs(val context: TemplateEvalContext, val args: Any?, val mapper: ObjectMapper2, val parentScope: Template.Scope? = null) :
            AsyncTextWriterContainer {
            override suspend fun write(writer: suspend (String) -> Unit) {
                context.exec2(args, mapper, parentScope, writer)
            }
        }

        fun withArgs(args: Any?, mapper: ObjectMapper2 = Mapper2, parentScope: Template.Scope? = null) = WithArgs(this, args, mapper, parentScope)

        suspend fun exec2(args: Any?, mapper: ObjectMapper2, parentScope: Template.Scope? = null, writer: suspend (String) -> Unit): Template.EvalContext {
            val scope = Scope(args, mapper, parentScope)
            if (template.frontMatter != null) for ((k, v) in template.frontMatter!!) scope.set(k, v)
            val context = Template.EvalContext(this, scope, template.config, mapper = mapper, write = writer)
            eval(context)
            return context
        }

        suspend fun exec(args: Any?, mapper: ObjectMapper2 = Mapper2, parentScope: Template.Scope? = null): ExecResult {
            val str = StringBuilder()
            val scope = Scope(args, mapper, parentScope)
            if (template.frontMatter != null) for ((k, v) in template.frontMatter!!) scope.set(k, v)
            val context = Template.EvalContext(this, scope, template.config, mapper, write = { str.append(it) })
            eval(context)
            return ExecResult(context, str.toString())
        }

        suspend fun exec(vararg args: Pair<String, Any?>, mapper: ObjectMapper2 = Mapper2, parentScope: Template.Scope? = null): ExecResult =
            exec(hashMapOf(*args), mapper, parentScope)

        operator suspend fun invoke(args: Any?, mapper: ObjectMapper2 = Mapper2, parentScope: Template.Scope? = null): String = exec(args, mapper, parentScope).str
        operator suspend fun invoke(vararg args: Pair<String, Any?>, mapper: ObjectMapper2 = Mapper2, parentScope: Template.Scope? = null): String =
            exec(hashMapOf(*args), mapper, parentScope).str

        suspend fun eval(context: Template.EvalContext) {
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
        var scope: Template.Scope,
        val config: TemplateConfig,
        val mapper: ObjectMapper2,
        var write: suspend (str: String) -> Unit
    ) : DynamicContext {
        val leafTemplate: TemplateEvalContext = currentTemplate
        val templates = currentTemplate.templates
        val macros = hashMapOf<String, Macro>()
        var currentBlock: BlockInTemplateEval? = null

        internal val filterCtxPool = Pool { Filter.Ctx() }

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

        inline fun captureRaw(callback: () -> Unit): RawString = RawString(capture(callback))

        inline fun <T> createScope(content: MutableMap<*, *> = LinkedHashMap<Any?, Any?>(), callback: () -> T): T {
            val old = this.scope
            try {
                this.scope = Template.Scope(content, mapper, old)
                return callback()
            } finally {
                this.scope = old
            }
        }
    }

    fun addBlock(name: String, body: Block) {
        blocks[name] = body
    }

    //suspend operator fun invoke(hashMap: Any?, mapper: ObjectMapper2 = Mapper2): String = Template.TemplateEvalContext(this).invoke(hashMap, mapper = mapper)
    //suspend operator fun invoke(vararg args: Pair<String, Any?>, mapper: ObjectMapper2 = Mapper2): String = Template.TemplateEvalContext(this).invoke(*args, mapper = mapper)

    suspend fun createEvalContext() = Template.TemplateEvalContext(this)
    suspend operator fun invoke(hashMap: Any?, mapper: ObjectMapper2 = Mapper2): String =
        createEvalContext().invoke(hashMap, mapper = mapper)

    suspend operator fun invoke(vararg args: Pair<String, Any?>, mapper: ObjectMapper2 = Mapper2): String =
        createEvalContext().invoke(*args, mapper = mapper)

    suspend fun prender(vararg args: Pair<String, Any?>, mapper: ObjectMapper2 = Mapper2): AsyncTextWriterContainer {
        return createEvalContext().withArgs(HashMap(args.toMap()), mapper)
    }

    suspend fun prender(args: Any?, mapper: ObjectMapper2 = Mapper2): AsyncTextWriterContainer {
        return createEvalContext().withArgs(args, mapper)
    }
}

suspend fun Template(
    template: String,
    templates: Templates,
    includes: NewTemplateProvider = templates.includes,
    layouts: NewTemplateProvider = templates.layouts,
    config: TemplateConfig = templates.config,
    cache: Boolean = templates.cache,
): Template {
    val root = TemplateProvider(mapOf("template" to template))
    return Templates(
        root = root,
        includes = includes,
        layouts = layouts,
        config = config,
        cache = cache,
    ).get("template")
}

suspend fun Template(template: String, config: TemplateConfig = TemplateConfig()): Template = Templates(
    TemplateProvider(mapOf("template" to template)),
    config = config
).get("template")
