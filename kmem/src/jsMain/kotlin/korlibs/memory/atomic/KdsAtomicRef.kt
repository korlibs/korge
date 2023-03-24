package korlibs.memory.atomic

actual class KmemAtomicRef<T> actual constructor(initial: T) {
    actual var value: T = initial
}
