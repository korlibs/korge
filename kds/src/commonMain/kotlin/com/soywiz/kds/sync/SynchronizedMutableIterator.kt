package com.soywiz.kds.sync

import com.soywiz.kds.lock.Lock

open class SynchronizedMutableIterator<T>(
    protected val iterator: MutableIterator<T>,
    protected val lock: Lock = Lock()
) : MutableIterator<T> {
    override fun hasNext(): Boolean = lock { iterator.hasNext() }
    override fun next(): T = lock { iterator.next() }
    override fun remove()  = lock { iterator.remove() }
}
