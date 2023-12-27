package korlibs.datastructure.internal

internal actual fun anyIdentityHashCode(obj: Any?): Int =
    System.identityHashCode(obj)
