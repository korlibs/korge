package com.soywiz.korma.geom.bezier

import com.soywiz.kds.*
import com.soywiz.korma.geom.convex.*
import kotlin.native.concurrent.*

@ThreadLocal
val Curves.isConvex: Boolean by extraPropertyThis { this.assumeConvex || Convex.isConvex(this) }
