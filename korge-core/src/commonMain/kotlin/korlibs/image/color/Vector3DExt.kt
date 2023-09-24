package korlibs.image.color

import korlibs.math.geom.*

fun MVector4.setToColorPremultiplied(col: RGBA): MVector4 = this.apply { col.toPremultipliedVector3D(this) }
fun MVector4.setToColor(col: RGBA): MVector4 = this.apply { col.toPremultipliedVector3D(this) }
fun RGBA.toPremultipliedVector3D(out: MVector4 = MVector4()): MVector4 = out.setTo(rf * af, gf * af, bf * af, 1f)
fun RGBA.toVector3D(out: MVector4 = MVector4()): MVector4 = out.setTo(rf, gf, bf, af)
fun RGBAPremultiplied.toVector3D(out: MVector4 = MVector4()): MVector4 = out.setTo(rf, gf, bf, af)

fun RGBAPremultiplied.toVector4(): Vector4F = Vector4F(rf, gf, bf, af)
