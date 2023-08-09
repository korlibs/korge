package korlibs.template

open class KorteException(val msg: String, val context: FilePosContext) : RuntimeException() {

	override val message: String get() = "$msg at $context"

	//override fun toString(): String = message
}

fun korteException(msg: String, context: FilePosContext): Nothing = throw KorteException(msg, context)
