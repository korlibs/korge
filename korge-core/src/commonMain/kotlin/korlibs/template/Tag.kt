package korlibs.template

import korlibs.template.dynamic.DynamicContext

data class Tag(val name: String, val nextList: Set<String>, val end: Set<String>?, val aliases: List<String> = listOf(), val buildNode: suspend BuildContext.() -> Block) : DynamicContext {
	data class Part(val tag: Token.TTag, val body: Block)
	data class BuildContext(val context: Template.ParseContext, val chunks: List<Tag.Part>)
}
