package com.soywiz.kds.internal

internal actual fun anyIdentityHashCode(obj: Any?): Int =
    System.identityHashCode(obj)
