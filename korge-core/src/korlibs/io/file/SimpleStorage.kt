package korlibs.io.file

interface SimpleStorage {
	suspend fun get(key: String): String?
	suspend fun set(key: String, value: String)
	suspend fun remove(key: String)
}
