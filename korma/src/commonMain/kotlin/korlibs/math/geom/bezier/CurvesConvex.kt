package korlibs.math.geom.bezier

import korlibs.datastructure.*
import korlibs.math.geom.convex.*
import kotlin.native.concurrent.*

@ThreadLocal
val Curves.isConvex: Boolean by extraPropertyThis { this.assumeConvex || Convex.isConvex(this) }