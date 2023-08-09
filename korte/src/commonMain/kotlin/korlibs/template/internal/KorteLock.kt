package korlibs.template.internal

internal expect class KorteLock() {
	inline operator fun <T> invoke(callback: () -> T): T
}
