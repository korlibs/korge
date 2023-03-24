package korlibs.datastructure

/**
 * [Map] with [String] keys that are treated in a insensitive manner.
 */
class CaseInsensitiveStringMap<T> private constructor(
    private val mapOrig: MutableMap<String, T>,
    private val lcToOrig: MutableMap<String, String>,
    private val mapLC: MutableMap<String, T>
) : MutableMap<String, T> by mapOrig {
    constructor() : this(LinkedHashMap(), LinkedHashMap(), LinkedHashMap())
    constructor(data: Map<String, T>) : this() { putAll(data) }
    constructor(vararg items: Pair<String, T>) : this() { putAll(items.toList()) }

    override fun containsKey(key: String): Boolean = mapLC.containsKey(key.toLowerCase())

    override fun clear() {
        mapOrig.clear()
        mapLC.clear()
        lcToOrig.clear()
    }

    override fun get(key: String): T? = mapLC[key.toLowerCase()]

    override fun put(key: String, value: T): T? {
        remove(key)
        mapOrig[key] = value
        lcToOrig[key.toLowerCase()] = key
        return mapLC.put(key.toLowerCase(), value)
    }

    override fun putAll(from: Map<out String, T>) {
        for (v in from) put(v.key, v.value)
    }

    override fun remove(key: String): T? {
        val lkey = key.toLowerCase()
        val okey = lcToOrig[lkey]
        mapOrig.remove(okey)
        val res = mapLC.remove(lkey)
        lcToOrig.remove(lkey)
        return res
    }

    override fun equals(other: Any?): Boolean = (other is CaseInsensitiveStringMap<*>) && this.mapLC == other.mapLC
    override fun hashCode(): Int = mapLC.hashCode()
}

fun <T> Map<String, T>.toCaseInsensitiveMap(): Map<String, T> =
    CaseInsensitiveStringMap<T>().also { it.putAll(this) }
