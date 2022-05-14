package com.soywiz.korma.triangle

import com.soywiz.korma.geom.shape.Shape2d
import com.soywiz.korma.geom.triangle.area
import com.soywiz.korma.triangle.triangulate.triangulateFlat

val Shape2d.area: Double
    get() = when (this) {
        is Shape2d.Complex -> this.items.sumByDouble { it.area }
        is Shape2d.WithArea -> this.area
        else -> this.triangulateFlat().sumByDouble { it.area }
    }
