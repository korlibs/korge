package korlibs.datastructure.atomic

actual class KdsAtomicRef<T> actual constructor(initial: T) {
    actual var value: T = initial
}