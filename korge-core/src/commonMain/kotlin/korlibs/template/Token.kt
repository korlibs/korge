package korlibs.template

data class FileContext(val fileName: String, val fileContent: String) {
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
        val DUMMY = FileContext("unknown", "")
    }
}

data class FilePosContext(val file: FileContext, val pos: Int) {
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

interface TokenContext {
    var file: FileContext
    var pos: Int
    val posContext: FilePosContext get() = FilePosContext(file, pos)

    fun exception(msg: String): Nothing = posContext.exception(msg)

    class Mixin : TokenContext {
        override var file: FileContext = FileContext.DUMMY
        override var pos: Int = -1
    }
}

sealed class Token : TokenContext {
    var trimLeft = false
    var trimRight = false

    data class TLiteral(val content: String) : Token(), TokenContext by TokenContext.Mixin()
    data class TExpr(val content: String) : Token(), TokenContext by TokenContext.Mixin()
    data class TTag(val name: String, val content: String) : Token(), TokenContext by TokenContext.Mixin() {
        val tokens by lazy { ExprNode.Token.tokenize(content, posContext) }
        val expr by lazy { ExprNode.parse(this) }
    }

    companion object {
        // @TODO: Use StrReader
        fun tokenize(str: String, context: FilePosContext): List<Token> {
            val out = arrayListOf<Token>()
            var lastPos = 0

            fun emit(token: Token, pos: Int) {
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
                if (cur is Token.TLiteral) {
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
