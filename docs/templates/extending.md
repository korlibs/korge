---
permalink: /templates/extending/
group: templates
layout: default
title: Template Extending
title_short: Extending
fa-icon: fa-puzzle-piece
priority: 70
---

It is possible to extend KorTE with new tags, functions and filters.



{% raw %}

## Extending

By registering new extensions via `TemplateConfig.register` you can have more functionality in your templates.

```kotlin
val config = TemplateConfig()
config.register(Filter("length") { subject.dynamicLength() })

val template = Template("mytemplate", config)
```

## Filters

Filters have an injected this with the execution `context`, the `subject`,and optionally the `args` of the filter call.

```kotlin
config.register(Filter("length") { subject.dynamicLength() })
```

```kotlin
config.register(Filter("chunked") {
    subject.toDynamicList().chunked(args[0].toDynamicInt())
})
```

## Functions

Functions have an args argument that receives a list of parameters already evaluated:

```kotlin
config.register(TeFunction("cycle") { args ->
    val list = args.getOrNull(0).toDynamicList()
    val index = args.getOrNull(1).toDynamicInt()
    list[index umod list.size]
})
```

## Tags

A tag has an associated name, several internal tag names, and a tag that ends this tag or null if it is not a block.

```kotlin
config.register(Tag("capture", setOf(), null) {
    data class BlockCapture(val varname: String, val content: Block) : Block {
		override suspend fun eval(context: Template.EvalContext) {
			val result = context.capture {
				content.eval(context)
			}
			context.scope.set(varname, RawString(result))
		}
	}

    val main = chunks[0]
    val tr = ExprNode.Token.tokenize(main.tag.content)
    val varname = ExprNode.parseId(tr)
    DefaultBlocks.BlockCapture(varname, main.body)
})
```

```kotlin
config.register(Tag("switch", setOf("case", "default"), setOf("endswitch")) {
    var subject: ExprNode? = null
    val cases = arrayListOf<Pair<ExprNode, Block>>()
    var defaultCase: Block? = null

    for (part in this.chunks) {
        val tagContent = part.tag.content
        val body = part.body
        when (part.tag.name) {
            "switch" -> {
                subject = ExprNode.parse(tagContent)
            }
            "case" -> {
                cases += ExprNode.parse(tagContent) to body
            }
            "default" -> {
                defaultCase = body
            }
        }
    }
    if (subject == null) error("No subject set in switch")
    //println(this.chunks)
    object : Block {
        override suspend fun eval(context: Template.EvalContext) {
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
})
```

{% endraw %}
