package korlibs.template

import korlibs.template.internal.extraProperty
import kotlin.coroutines.cancellation.CancellationException
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
var TemplateConfig.debugPrintln by extraProperty({ extra }) { { v: Any? -> println(v) } }

object DefaultBlocks {
    data class BlockBlock(val name: String, val contentType: String?) : Block {
        override suspend fun eval(context: Template.EvalContext) {
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

    data class BlockCapture(val varname: String, val content: Block, val contentType: String? = null) : Block {
        override suspend fun eval(context: Template.EvalContext) {
            val result = context.capture {
                content.eval(context)
            }
            context.scope.set(varname, RawString(result, contentType))
        }
    }

    data class BlockDebug(val expr: ExprNode) : Block {
        override suspend fun eval(context: Template.EvalContext) {
            context.config.debugPrintln(expr.eval(context))
        }
    }

    data class BlockExpr(val expr: ExprNode) : Block {
        override suspend fun eval(context: Template.EvalContext) {
            context.config.writeBlockExpressionResult(context, expr.eval(context))
        }
    }

    data class BlockExtends(val expr: ExprNode) : Block {
        override suspend fun eval(context: Template.EvalContext) {
            val result = expr.eval(context)
            val parentTemplate = Template.TemplateEvalContext(context.templates.getLayout(result.toDynamicString()))
            context.currentTemplate.parent = parentTemplate
            parentTemplate.eval(context)
            throw Template.StopEvaluatingException()
            //context.template.parent
        }
    }

    data class BlockFor(val varnames: List<String>, val expr: ExprNode, val loop: Block, val elseNode: Block?) : Block {
        override suspend fun eval(context: Template.EvalContext) {
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

    data class BlockGroup(val children: List<Block>) : Block {
        override suspend fun eval(context: Template.EvalContext) {
            for (n in children) n.eval(context)
        }
    }

    data class BlockIf(val cond: ExprNode, val trueContent: Block, val falseContent: Block?) : Block {
        override suspend fun eval(context: Template.EvalContext) {
            if (cond.eval(context).toDynamicBool()) {
                trueContent.eval(context)
            } else {
                falseContent?.eval(context)
            }
        }
    }

    data class BlockImport(val fileExpr: ExprNode, val exportName: String) : Block {
        override suspend fun eval(context: Template.EvalContext) {
            val ctx =
                Template.TemplateEvalContext(context.templates.getInclude(fileExpr.eval(context).toString())).exec()
                    .context
            context.scope.set(exportName, ctx.macros)
        }
    }

    data class BlockInclude(
        val fileNameExpr: ExprNode,
        val params: LinkedHashMap<String, ExprNode>,
        val filePos: FilePosContext,
        val tagContent: String
    ) : Block {
        override suspend fun eval(context: Template.EvalContext) {
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
                Template.TemplateEvalContext(includeTemplate).eval(context)
            }
        }
    }

    data class BlockMacro(val funcname: String, val args: List<String>, val body: Block) : Block {
        override suspend fun eval(context: Template.EvalContext) {
            context.macros[funcname] = Template.Macro(funcname, args, body)
        }
    }

    data class BlockSet(val varname: String, val expr: ExprNode) : Block {
        override suspend fun eval(context: Template.EvalContext) {
            context.scope.set(varname, expr.eval(context))
        }
    }

    data class BlockText(val content: String) : Block {
        override suspend fun eval(context: Template.EvalContext) {
            context.write(content)
        }
    }
}
