package korlibs.io.concurrent.atomic

import korlibs.math.*
import kotlin.concurrent.*

actual fun <T> korAtomic(initial: T): KorAtomicRef<T> = object : KorAtomicRef<T>(initial, true) {
    val ref = AtomicReference(initial)
    override var value: T get() = ref.value; set(value) { ref.value = value }
    override fun compareAndSet(expect: T, update: T): Boolean = ref.compareAndSet(expect, update)
}

actual fun korAtomic(initial: Boolean): KorAtomicBoolean = object : KorAtomicBoolean(initial, true) {
    val ref = AtomicInt(initial.toInt())
    override var value: Boolean get() = ref.value != 0; set(value) { ref.value = value.toInt() }

    override fun compareAndSet(expect: Boolean, update: Boolean): Boolean = ref.compareAndSet(expect.toInt(), update.toInt())
}

actual fun korAtomic(initial: Int): KorAtomicInt = object : KorAtomicInt(initial, true) {
    val ref = AtomicInt(initial)
    override var value: Int get() = ref.value; set(value) { ref.value = value }
    override fun compareAndSet(expect: Int, update: Int): Boolean = ref.compareAndSet(expect, update)
    override fun addAndGet(delta: Int): Int = ref.addAndGet(delta)
}

actual fun korAtomic(initial: Long): KorAtomicLong = object : KorAtomicLong(initial, true) {
    val ref = AtomicLong(initial)
    override var value: Long get() = ref.value; set(value) { ref.value = value }
    override fun compareAndSet(expect: Long, update: Long): Boolean = ref.compareAndSet(expect, update)
    override fun addAndGet(delta: Long): Long = ref.addAndGet(delta)
}
