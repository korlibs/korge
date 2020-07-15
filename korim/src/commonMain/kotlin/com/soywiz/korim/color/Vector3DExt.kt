package com.soywiz.korim.color

import com.soywiz.korma.geom.*

fun Vector3D.setToColorPremultiplied(col: RGBA): Vector3D = this.apply { col.toPremultipliedVector3D(this) }
fun Vector3D.setToColor(col: RGBA): Vector3D = this.apply { col.toPremultipliedVector3D(this) }
fun RGBA.toPremultipliedVector3D(out: Vector3D = Vector3D()): Vector3D = out.setTo(rf * af, gf * af, bf * af, 1f)
fun RGBA.toVector3D(out: Vector3D = Vector3D()): Vector3D = out.setTo(rf, gf, bf, af)
