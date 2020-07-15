package com.soywiz.korio.util

@Suppress("UNCHECKED_CAST")
class DynamicNode(private val wrapped: Any?, private val key: String? = null) {
	fun getAny(): Any? = if (key != null) DynamicJvm.getAnySync(wrapped, key!!) else wrapped

	fun setAny(value: Any?) = DynamicJvm.setAnySync(wrapped, key, value)

	operator fun set(name: String, value: Any?) = DynamicJvm.setAnySync(getAny(), name, value)

	operator fun get(name: String): DynamicNode = DynamicNode(this.getAny(), name)

	operator fun get(index: Int): DynamicNode = DynamicNode(this.getAny(), "$index")

	fun exists() = this.getAny() != null

	fun getKeys(): Iterable<String> {
		val value = getAny()
		return when (value) {
			null -> listOf()
			is Map<*, *> -> value.keys.map { "$it" }.toList()
			is List<*> -> (0 until value.size).map { "$it" }
			else -> value.javaClass.declaredFields.filter { it.isAccessible }.map { it.name }
		}
	}

	fun getEntries(): Map<String, DynamicNode> = getKeys().map { it to this[it] }.toMap()

	fun toNumber() = getAny() as Number?
	fun toInt() = toNumber()?.toInt()
	fun toDouble() = toNumber()?.toDouble()
	fun toFloat() = toNumber()?.toFloat()
	fun toBoolean(): Boolean? = DynamicJvm.toBoolOrNull(getAny())

	fun asString() = getAny()?.toString()

	fun toNumber(default: Number): Number = toNumber() ?: default
	fun toInt(default: Int): Int = toInt() ?: default
	fun toDouble(default: Double): Double = toDouble() ?: default
	fun toFloat(default: Float): Float = toFloat() ?: default
	fun toBoolean(default: Boolean): Boolean = toBoolean() ?: default
	fun toString(default: String): String = asString() ?: default
}

fun Any?.asDynamicNode() = DynamicNode(this)