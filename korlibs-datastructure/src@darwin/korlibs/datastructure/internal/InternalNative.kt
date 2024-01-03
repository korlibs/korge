@file:OptIn(ExperimentalNativeApi::class)

package korlibs.datastructure.internal

import kotlin.experimental.*
import kotlin.native.identityHashCode as kotlinIdentityHashCode

@OptIn(ExperimentalNativeApi::class)
internal actual fun anyIdentityHashCode(obj: Any?): Int =
    obj.kotlinIdentityHashCode()
