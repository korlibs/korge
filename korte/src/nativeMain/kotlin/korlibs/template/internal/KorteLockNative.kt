package korlibs.template.internal

internal actual class KorteLock actual constructor() {
	actual inline operator fun <T> invoke(callback: () -> T): T = callback()
}
