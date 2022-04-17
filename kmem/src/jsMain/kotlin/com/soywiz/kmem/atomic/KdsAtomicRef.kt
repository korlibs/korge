package com.soywiz.kmem.atomic

actual class KmemAtomicRef<T> actual constructor(initial: T) {
    actual var value: T = initial
}
