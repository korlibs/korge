package com.soywiz.kds.internal

import kotlin.native.identityHashCode as kotlinIdentityHashCode

internal actual fun anyIdentityHashCode(obj: Any?): Int =
    obj.kotlinIdentityHashCode()
