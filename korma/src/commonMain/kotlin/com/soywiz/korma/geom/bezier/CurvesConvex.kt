package com.soywiz.korma.geom.bezier

import com.soywiz.kds.extraPropertyThis
import com.soywiz.korma.geom.convex.Convex
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
val Curves.isConvex: Boolean by extraPropertyThis { this.assumeConvex || Convex.isConvex(this) }
