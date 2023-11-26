package korlibs.io.lang

fun String.textCase(): TextCase = TextCase(this)

class TextCase(val words: List<String>) {
    companion object {
        operator fun invoke(str: String): TextCase {
            // @TODO:
            //   - hello_World
            //   - SNAKE_CASE
            //   - TEST-DEMO
            //   - helloWorld
            //   - HelloWorld
            return TextCase(str.replace('_', '-').split(Regex("\\W+")))
            //val out = arrayListOf<String>()
            //val sr = StrReader(str)
            //var lastLowerCase: Boolean? = null
            //while (sr.hasMore) {
            //    val lowerCase = sr.peek().isLowerCase()
            //    val changedCase = (lowerCase != lastLowerCase)
            //    if (changedCase) {
            //    }
            //    lastLowerCase = lowerCase
            //}
            //return TextCase(out)
        }
    }

    fun spaceCase(): String = words.joinToString(" ") { it.lowercase() }
    fun snakeCase(): String = words.joinToString("_") { it.lowercase() }
    fun kebabCase(): String = words.joinToString("_") { it.lowercase() }
    fun screamingSnakeCase(): String = words.joinToString("_") { it.uppercase() }
    fun pascalCase(): String = words.joinToString("") { it.lowercase().replaceFirstChar { it.uppercaseChar() } }
    fun camelCase(): String {
        var first = true
        return words.joinToString("") {
            if (first) {
                first = false
                it.lowercase()
            } else {
                it.lowercase().replaceFirstChar { it.uppercaseChar() }
            }
        }
    }
}
