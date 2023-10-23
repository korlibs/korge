package korlibs.io.concurrent.atomic

import kotlin.reflect.*

expect fun <T> korAtomic(initial: T): KorAtomicRef<T>
expect fun korAtomic(initial: Boolean): KorAtomicBoolean
expect fun korAtomic(initial: Int): KorAtomicInt
expect fun korAtomic(initial: Long): KorAtomicLong

fun <T> KorAtomicRef(initial: T): KorAtomicRef<T> = korAtomic(initial)
fun KorAtomicBoolean(initial: Boolean): KorAtomicBoolean = korAtomic(initial)
fun KorAtomicInt(initial: Int): KorAtomicInt = korAtomic(initial)
fun KorAtomicLong(initial: Long): KorAtomicLong = korAtomic(initial)

//fun <T> korAtomic(initial: T): KorAtomicRef<T> = KorAtomicRef(initial)
//fun korAtomic(initial: Boolean): KorAtomicBoolean = KorAtomicBoolean(initial)
//fun korAtomic(initial: Int): KorAtomicInt = KorAtomicInt(initial)
//fun korAtomic(initial: Long): KorAtomicLong = KorAtomicLong (initial)

interface KorAtomicBase<T> {
	var value: T
	fun compareAndSet(expect: T, update: T): Boolean
}

inline fun <T> KorAtomicBase<T>.update(transform: (T) -> T): T {
    while (true) {
        val value = this.value
        val next = transform(value)
        if (compareAndSet(value, next)) return next
    }
}

interface KorAtomicNumber<T : Number> : KorAtomicBase<T> {
	fun addAndGet(delta: T): T
}

open class KorAtomicRef<T> internal constructor(initial: T, dummy: Boolean) : KorAtomicBase<T> {
	override var value: T = initial

	override fun compareAndSet(expect: T, update: T): Boolean {
		return if (value == expect) {
			value = update
			true
		} else {
			false
		}
	}

    override fun toString(): String = "$value"
}

open class KorAtomicBoolean internal constructor(initial: Boolean, dummy: Boolean) : KorAtomicBase<Boolean> {
	override var value: Boolean = initial

	override fun compareAndSet(expect: Boolean, update: Boolean): Boolean {
		return if (value == expect) {
			value = update
			true
		} else {
			false
		}
	}

    override fun toString(): String = "$value"
}

open class KorAtomicInt internal constructor(initial: Int, dummy: Boolean) : KorAtomicNumber<Int> {
	override var value: Int = initial

	override fun compareAndSet(expect: Int, update: Int): Boolean {
		return if (value == expect) {
			value = update
			true
		} else {
			false
		}
	}

	override fun addAndGet(delta: Int): Int = update { it + delta }
    fun addAndGetMod(delta: Int, modulo: Int): Int = update { (it + delta) % modulo }

    override fun toString(): String = "$value"
}

class KorAtomicFloat(initial: Float) : KorAtomicNumber<Float> {
    private val atomic = KorAtomicInt(initial.toRawBits())
    override var value: Float
        get() = Float.fromBits(atomic.value)
        set(value) {
            atomic.value = value.toRawBits()
        }

    override fun compareAndSet(expect: Float, update: Float): Boolean {
        return if (value == expect) {
            value = update
            true
        } else {
            false
        }
    }

    override fun addAndGet(delta: Float): Float = update { it + delta }
    fun addAndGetMod(delta: Float, modulo: Float): Float = update { (it + delta) % modulo }
    override fun toString(): String = "$value"
}

open class KorAtomicLong internal constructor(initial: Long, dummy: Boolean) : KorAtomicNumber<Long> {
	override var value: Long = initial

	override fun compareAndSet(expect: Long, update: Long): Boolean {
		return if (value == expect) {
			value = update
			true
		} else {
			false
		}
	}

	override fun addAndGet(delta: Long): Long {
		this.value += delta
		return this.value
	}

    override fun toString(): String = "$value"
}

fun KorAtomicInt.getAndAdd(delta: Int): Int = addAndGet(delta) - delta
fun KorAtomicLong.getAndAdd(delta: Long): Long = addAndGet(delta) - delta

fun KorAtomicInt.incrementAndGet() = addAndGet(1)
fun KorAtomicLong.incrementAndGet() = addAndGet(1)

fun KorAtomicInt.getAndIncrement() = getAndAdd(1)
fun KorAtomicLong.getAndIncrement() = getAndAdd(1)


inline operator fun <T> KorAtomicRef<T>.getValue(obj: Any, prop: KProperty<Any?>): T = this.value
inline operator fun <T> KorAtomicRef<T>.setValue(obj: Any, prop: KProperty<Any?>, v: T) { this.value = v }

inline operator fun KorAtomicBoolean.getValue(obj: Any, prop: KProperty<Any?>): Boolean = this.value
inline operator fun KorAtomicBoolean.setValue(obj: Any, prop: KProperty<Any?>, v: Boolean) { this.value = v }

inline operator fun KorAtomicInt.getValue(obj: Any, prop: KProperty<Any?>): Int = this.value
inline operator fun KorAtomicInt.setValue(obj: Any, prop: KProperty<Any?>, v: Int) { this.value = v }

inline operator fun KorAtomicLong.getValue(obj: Any, prop: KProperty<Any?>): Long = this.value
inline operator fun KorAtomicLong.setValue(obj: Any, prop: KProperty<Any?>, v: Long) { this.value = v }
