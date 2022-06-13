package com.soywiz.korma.geom.bezier

import com.soywiz.kds.extraPropertyThis
import com.soywiz.korma.geom.convex.Convex

val Curves.isConvex: Boolean by extraPropertyThis { this.assumeConvex || Convex.isConvex(this) }
