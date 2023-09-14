package korlibs.datastructure.sync

import korlibs.datastructure.lock.NonRecursiveLock

open class SynchronizedMutableIterator<T>(
    protected val iterator: MutableIterator<T>,
    protected val lock: NonRecursiveLock = NonRecursiveLock()
) : MutableIterator<T> {
    override fun hasNext(): Boolean = lock { iterator.hasNext() }
    override fun next(): T = lock { iterator.next() }
    override fun remove()  = lock { iterator.remove() }
}
