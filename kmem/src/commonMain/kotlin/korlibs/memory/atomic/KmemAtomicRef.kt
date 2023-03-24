package korlibs.memory.atomic

expect class KmemAtomicRef<T>(initial: T) {
    var value: T
}