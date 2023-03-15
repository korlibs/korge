package com.soywiz.korim.format.cg

import com.soywiz.korma.geom.*
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake

fun MRectangle.toCG(): CValue<CGRect> = CGRectMake(x.cg, y.cg, width.cg, height.cg)
fun Rectangle.toCG(): CValue<CGRect> = CGRectMake(x.cg, y.cg, width.cg, height.cg)
