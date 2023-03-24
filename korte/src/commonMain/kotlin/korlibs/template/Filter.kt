package korlibs.template

import korlibs.template.dynamic.DynamicContext

data class Filter(val name: String, val eval: suspend Ctx.() -> Any?) {
    class Ctx : DynamicContext {
        lateinit var context: Template.EvalContext
        lateinit var tok: ExprNode.Token
        lateinit var name: String
        val mapper get() = context.mapper
        var subject: Any? = null
        var args: List<Any?> = listOf()
    }
}
