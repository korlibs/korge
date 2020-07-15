package com.soywiz.kds.atomic

expect class KdsAtomicRef<T>(initial: T) {
    var value: T
}
