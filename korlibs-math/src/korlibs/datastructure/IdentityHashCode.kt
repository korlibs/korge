package korlibs.datastructure

import korlibs.datastructure.internal.anyIdentityHashCode

fun Any?.identityHashCode(): Int = anyIdentityHashCode(this)
