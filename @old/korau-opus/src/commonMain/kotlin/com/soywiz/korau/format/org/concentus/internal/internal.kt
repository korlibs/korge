package com.soywiz.korau.format.org.concentus.internal

import kotlin.math.*

internal fun rint(v: Double): Double = if (v >= floor(v) + 0.5) ceil(v) else round(v)  // @TODO: Is this right?
internal fun rint(v: Float): Float = if (v >= floor(v) + 0.5) ceil(v) else round(v)  // @TODO: Is this right?

internal infix fun Byte.and(mask: Long): Long = this.toLong() and mask
internal infix fun Byte.and(mask: Int): Int = this.toInt() and mask
internal infix fun Short.and(mask: Int): Int = this.toInt() and mask

internal infix fun Byte.or(mask: Int): Int = this.toInt() or mask
internal infix fun Short.or(mask: Int): Int = this.toInt() or mask
internal infix fun Short.or(mask: Short): Int = this.toInt() or mask.toInt()

internal infix fun Byte.xor(mask: Int): Int = this.toInt() xor mask
internal infix fun Short.xor(mask: Int): Int = this.toInt() xor mask
internal infix fun Short.xor(mask: Short): Int = this.toInt() xor mask.toInt()

internal infix fun Byte.shl(that: Int): Int = this.toInt() shl that
internal infix fun Short.shl(that: Int): Int = this.toInt() shl that

internal infix fun Byte.shr(that: Int): Int = this.toInt() shr that
internal infix fun Short.shr(that: Int): Int = this.toInt() shr that

internal infix fun Byte.ushr(that: Int): Int = this.toInt() ushr that
internal infix fun Short.ushr(that: Int): Int = this.toInt() ushr that
