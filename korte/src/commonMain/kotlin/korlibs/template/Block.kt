package korlibs.template

import korlibs.template.dynamic.DynamicContext
import korlibs.template.internal.Yaml
import korlibs.template.util.ListReader

interface Block : DynamicContext {
    suspend fun eval(context: Template.EvalContext)

    companion object {
        fun group(children: List<Block>): Block =
            if (children.size == 1) children[0] else DefaultBlocks.BlockGroup(children)

        class Parse(val tokens: List<Token>, val parseContext: Template.ParseContext) {
            val tr = ListReader(tokens, tokens.lastOrNull())

            suspend fun handle(tag: Tag, token: Token.TTag): Block {
                val parts = arrayListOf<Tag.Part>()
                var currentToken = token
                val children = arrayListOf<Block>()

                fun emitPart() {
                    parts += Tag.Part(currentToken, group(children.toList()))
                }

                loop@ while (!tr.eof) {
                    val it = tr.read()
                    when (it) {
                        is Token.TLiteral -> {
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
                                        val yaml = Yaml.read(yamlText)
                                        if (yaml is Map<*, *>) {
                                            parseContext.template.frontMatter = yaml as Map<String, Any?>
                                        }
                                        text = outside.joinToString("\n")
                                    }
                                }
                            }
                            children += DefaultBlocks.BlockText(parseContext.template.templateContent.chunkProcessor(text))
                        }
                        is Token.TExpr -> {
                            children += DefaultBlocks.BlockExpr(ExprNode.parse(it.content, it.posContext))
                        }
                        is Token.TTag -> {
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
                                            Tag.BuildContext(
                                                parseContext,
                                                listOf(Tag.Part(it, DefaultBlocks.BlockText("")))
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

                return tag.buildNode(Tag.BuildContext(parseContext, parts))
            }
        }

        suspend fun parse(tokens: List<Token>, parseContext: Template.ParseContext): Block {
            return Parse(tokens, parseContext).handle(DefaultTags.Empty, Token.TTag("", ""))
        }
    }
}
