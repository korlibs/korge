@file:OptIn(ExperimentalNativeApi::class)

package korlibs.datastructure.internal

import kotlin.native.identityHashCode as kotlinIdentityHashCode
import kotlin.experimental.ExperimentalNativeApi

internal actual fun anyIdentityHashCode(obj: Any?): Int =
    obj.kotlinIdentityHashCode()
