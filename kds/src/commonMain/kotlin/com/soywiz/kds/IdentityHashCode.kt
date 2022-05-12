package com.soywiz.kds

import com.soywiz.kds.internal.anyIdentityHashCode

fun Any?.identityHashCode(): Int = anyIdentityHashCode(this)
