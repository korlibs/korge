package com.soywiz.kds

class IndexedTable<T> : Iterable<T> {
	val instances = arrayListOf<T>()
	val size get() = instances.size
	private val instanceToIndex = LinkedHashMap<T, Int>()

	fun add(str: T): Unit = run { get(str) }

	operator fun get(str: T): Int = instanceToIndex.getOrPut(str) { instances.size.also { instances += str } }
	override fun iterator(): Iterator<T> = instances.iterator()

    override fun equals(other: Any?): Boolean = (other is IndexedTable<*>) && this.instances == other.instances
    override fun hashCode(): Int = this.instances.hashCode()
}
