package korlibs.datastructure.atomic

expect class KdsAtomicRef<T>(initial: T) {
    var value: T
}