package korlibs.template

data class TeFunction(val name: String, val eval: suspend Template.EvalContext.(args: List<Any?>) -> Any?) {
	suspend fun eval(args: List<Any?>, context: Template.EvalContext) = eval.invoke(context, args)
}
