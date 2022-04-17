package com.soywiz.kmem.atomic

expect class KmemAtomicRef<T>(initial: T) {
    var value: T
}
