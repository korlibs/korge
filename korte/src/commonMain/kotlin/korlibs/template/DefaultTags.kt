package korlibs.template

@Suppress("unused")
object DefaultTags {
	val BlockTag = Tag("block", setOf(), setOf("end", "endblock")) {
		val part = chunks.first()
		val tr = part.tag.tokens
		val name = ExprNode.parseId(tr)
		if (name.isEmpty()) throw IllegalArgumentException("block without name")
        val contentType = if (tr.hasMore) ExprNode.parseId(tr) else null
		tr.expectEnd()
		context.template.addBlock(name, part.body)
		DefaultBlocks.BlockBlock(name, contentType ?: this.context.template.templateContent.contentType)
	}

    val Capture = Tag("capture", setOf(), setOf("end", "endcapture")) {
        val main = chunks[0]
		val tr = main.tag.tokens
		val varname = ExprNode.parseId(tr)
        val contentType = if (tr.hasMore) ExprNode.parseId(tr) else null
		tr.expectEnd()
		DefaultBlocks.BlockCapture(varname, main.body, contentType)
	}

	val Debug = Tag("debug", setOf(), null) {
		DefaultBlocks.BlockDebug(chunks[0].tag.expr)
	}

	val Empty = Tag("", setOf(""), null) {
		Block.group(chunks.map { it.body })
	}

	val Extends = Tag("extends", setOf(), null) {
		val part = chunks.first()
		val parent = ExprNode.parseExpr(part.tag.tokens)
		DefaultBlocks.BlockExtends(parent)
	}

	val For = Tag("for", setOf("else"), setOf("end", "endfor")) {
		val main = chunks[0]
		val elseTag = chunks.getOrNull(1)?.body
		val tr = main.tag.tokens
		val varnames = arrayListOf<String>()
		do {
			varnames += ExprNode.parseId(tr)
		} while (tr.tryRead(",") != null)
		ExprNode.expect(tr, "in")
		val expr = ExprNode.parseExpr(tr)
		tr.expectEnd()
		DefaultBlocks.BlockFor(varnames, expr, main.body, elseTag)
	}

    fun Tag.BuildContext.BuildIf(isIf: Boolean): Block {
        class Branch(val part: Tag.Part) {
            val expr get() = part.tag.expr
            val body get() = part.body
            val realExpr get() = if (part.tag.name.contains("unless")) {
                ExprNode.UNOP(expr, "!")
            } else {
                expr
            }
        }

        val branches = arrayListOf<Branch>()
        var elseBranch: Block? = null

        for (part in chunks) {
            when (part.tag.name) {
                "if", "elseif", "unless", "elseunless" -> branches += Branch(part)
                "else" -> elseBranch = part.body
            }
        }


        val branchesRev = branches.reversed()
        val firstBranch = branchesRev.first()

        var node: Block = DefaultBlocks.BlockIf(firstBranch.realExpr, firstBranch.body, elseBranch)
        for (branch in branchesRev.takeLast(branchesRev.size - 1)) {
            node = DefaultBlocks.BlockIf(branch.realExpr, branch.body, node)
        }

        return node
    }

	val If = Tag("if", setOf("else", "elseif", "elseunless"), setOf("end", "endif")) { BuildIf(isIf = true) }
    val Unless = Tag("unless", setOf("else", "elseif", "elseunless"), setOf("end", "endunless")) { BuildIf(isIf = true) }

	val Import = Tag("import", setOf(), null) {
		val part = chunks.first()
		val s = part.tag.tokens
		val file = s.parseExpr()
		s.expect("as")
		val name = s.read().text
		s.expectEnd()
		DefaultBlocks.BlockImport(file, name)
	}

	val Include = Tag("include", setOf(), null) {
		val main = chunks.first()
        val tr = main.tag.tokens
        val expr = ExprNode.parseExpr(tr)
        val params = linkedMapOf<String, ExprNode>()
        while (tr.hasMore) {
            val id = ExprNode.parseId(tr)
            tr.expect("=")
            val expr = ExprNode.parseExpr(tr)
            params[id] = expr
        }
        tr.expectEnd()
		DefaultBlocks.BlockInclude(expr, params, main.tag.posContext, main.tag.content)
	}

	val Macro = Tag("macro", setOf(), setOf("end", "endmacro")) {
		val part = chunks[0]
		val s = part.tag.tokens
		val funcname = s.parseId()
		s.expect("(")
		val params = s.parseIdList()
		s.expect(")")
		s.expectEnd()
		DefaultBlocks.BlockMacro(funcname, params, part.body)
	}

	val Set = Tag("set", setOf(), null) {
		val main = chunks[0]
		val tr = main.tag.tokens
		val varname = ExprNode.parseId(tr)
		ExprNode.expect(tr, "=")
		val expr = ExprNode.parseExpr(tr)
		tr.expectEnd()
		DefaultBlocks.BlockSet(varname, expr)
	}

    val Assign = Tag("assign", setOf(), null) {
        Set.buildNode(this)
    }

	val Switch = Tag("switch", setOf("case", "default"), setOf("end", "endswitch")) {
		var subject: ExprNode? = null
		val cases = arrayListOf<Pair<ExprNode, Block>>()
		var defaultCase: Block? = null

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
	}

	val ALL = listOf(
		BlockTag,
		Capture, Debug,
		Empty, Extends, For, If, Unless, Switch, Import, Include, Macro, Set,
        // Liquid
        Assign
	)
}
