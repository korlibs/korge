package com.soywiz.korma.triangle

import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.triangle.*
import com.soywiz.korma.triangle.triangulate.*

val Shape2d.area: Double
    get() = when (this) {
        is Shape2d.Complex -> this.items.sumByDouble { it.area }
        is Shape2d.WithArea -> this.area
        else -> this.triangulateFlat().sumByDouble { it.area }
    }
