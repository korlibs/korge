package korlibs.korge.service.storage

open class InmemoryStorage : IStorageWithKeys {
    val data = LinkedHashMap<String, String>()
    override operator fun set(key: String, value: String) { data[key] = value }
    override fun getOrNull(key: String): String? = data[key]
    override fun remove(key: String) { data.remove(key) }
    override fun removeAll(): Unit = data.clear()
    override fun keys(): List<String> = data.keys.toList()
}
