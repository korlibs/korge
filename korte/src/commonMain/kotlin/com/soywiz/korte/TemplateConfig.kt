package com.soywiz.korte

import com.soywiz.korte.dynamic.Dynamic2
import com.soywiz.korte.internal.htmlspecialchars

open class TemplateConfig(
    extraTags: List<Tag> = listOf(),
    extraFilters: List<Filter> = listOf(),
    extraFunctions: List<TeFunction> = listOf(),
    // Here we can convert markdown into html if required. This is available at the template level + content + named blocks
    val contentTypeProcessor: (content: String, contentType: String?) -> String = { content, _ -> content }
) {
    val extra = LinkedHashMap<String, Any>()

    val integratedFunctions = DefaultFunctions.ALL
    val integratedFilters = DefaultFilters.ALL
    val integratedTags = DefaultTags.ALL

    private val allFunctions = integratedFunctions + extraFunctions
    private val allTags = integratedTags + extraTags
    private val allFilters = integratedFilters + extraFilters

    val tags = hashMapOf<String, Tag>().apply {
        for (tag in allTags) {
            this[tag.name] = tag
            for (alias in tag.aliases) this[alias] = tag
        }
    }

    val filters = hashMapOf<String, Filter>().apply {
        for (filter in allFilters) this[filter.name] = filter
    }

    val functions = hashMapOf<String, TeFunction>().apply {
        for (func in allFunctions) this[func.name] = func
    }

    fun register(vararg its: Tag) = this.apply { for (it in its) tags[it.name] = it }
    fun register(vararg its: Filter) = this.apply { for (it in its) filters[it.name] = it }
    fun register(vararg its: TeFunction) = this.apply { for (it in its) functions[it.name] = it }

    var variableProcessor: VariableProcessor = { name ->
        scope.get(name)
    }

    fun replaceVariablePocessor(func: suspend Template.EvalContext.(name: String, previous: VariableProcessor) -> Any?) {
        val previous = variableProcessor
        variableProcessor = { eval ->
            this.func(eval, previous)
        }
    }

    var writeBlockExpressionResult: WriteBlockExpressionResultFunction = { value ->
        this.write(when (value) {
            is RawString -> contentTypeProcessor(value.str, value.contentType)
            else -> contentTypeProcessor(Dynamic2.toString(value), null).htmlspecialchars()
        })
    }

    fun replaceWriteBlockExpressionResult(func: suspend Template.EvalContext.(value: Any?, previous: WriteBlockExpressionResultFunction) -> Unit) {
        val previous = writeBlockExpressionResult
        writeBlockExpressionResult = { eval ->
            this.func(eval, previous)
        }
    }
}

typealias WriteBlockExpressionResultFunction = suspend Template.EvalContext.(value: Any?) -> Unit
typealias VariableProcessor = suspend Template.EvalContext.(name: String) -> Any?

open class TemplateConfigWithTemplates(
    extraTags: List<Tag> = listOf(),
    extraFilters: List<Filter> = listOf(),
    extraFunctions: List<TeFunction> = listOf()
) : TemplateConfig(extraTags, extraFilters, extraFunctions) {
    var templates = Templates(TemplateProvider(mapOf()), config = this)
    fun cache(value: Boolean) = this.apply { templates.cache = value }
    fun root(root: NewTemplateProvider, includes: NewTemplateProvider = root, layouts: NewTemplateProvider = root) =
        this.apply {
            templates.root = root
            templates.includes = includes
            templates.layouts = layouts
        }
}
