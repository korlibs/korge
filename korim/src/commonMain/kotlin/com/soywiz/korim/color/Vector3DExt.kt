package com.soywiz.korim.color

import com.soywiz.korma.geom.MVector4

fun MVector4.setToColorPremultiplied(col: RGBA): MVector4 = this.apply { col.toPremultipliedVector3D(this) }
fun MVector4.setToColor(col: RGBA): MVector4 = this.apply { col.toPremultipliedVector3D(this) }
fun RGBA.toPremultipliedVector3D(out: MVector4 = MVector4()): MVector4 = out.setTo(rf * af, gf * af, bf * af, 1f)
fun RGBA.toVector3D(out: MVector4 = MVector4()): MVector4 = out.setTo(rf, gf, bf, af)
