@file:OptIn(ExperimentalNativeApi::class)

package korlibs.datastructure.internal

import kotlin.experimental.*
import kotlin.native.identityHashCode as kotlinIdentityHashCode

internal actual fun anyIdentityHashCode(obj: Any?): Int =
    obj.kotlinIdentityHashCode()
