package korlibs.image.format.cg

import kotlinx.cinterop.*
import platform.CoreGraphics.*

// @TODO: K/N .convert() doesn't work to convert integers to doubles
@OptIn(UnsafeNumber::class)
expect inline fun Double.toCgFloat(): CGFloat
@OptIn(UnsafeNumber::class)
expect inline fun Float.toCgFloat(): CGFloat

@OptIn(UnsafeNumber::class)
inline val Int.cg: CGFloat get() = this.toDouble().toCgFloat()
@OptIn(UnsafeNumber::class)
inline val Float.cg: CGFloat get() = this.toCgFloat()
@OptIn(UnsafeNumber::class)
inline val Double.cg: CGFloat get() = this.toCgFloat()

fun CGRectMakeExt(x: Int, y: Int, width: Int, height: Int): CValue<CGRect> = CGRectMake(x.cg, y.cg, width.cg, height.cg)
fun CGRectMakeExt(x: Float, y: Float, width: Float, height: Float): CValue<CGRect> = CGRectMake(x.cg, y.cg, width.cg, height.cg)
fun CGRectMakeExt(x: Double, y: Double, width: Double, height: Double): CValue<CGRect> = CGRectMake(x.cg, y.cg, width.cg, height.cg)
