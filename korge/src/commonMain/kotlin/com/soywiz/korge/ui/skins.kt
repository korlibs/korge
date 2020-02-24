package com.soywiz.korge.ui

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*

data class UISkin(
    val normal: BmpSlice,
    val over: BmpSlice = normal,
    val down: BmpSlice = normal,
    val disabled: BmpSlice = normal,
    val backColor: RGBA = Colors.DARKGREY
)

data class IconSkin(
    val normal: BmpSlice,
    val over: BmpSlice = normal,
    val down: BmpSlice = normal,
    val disabled: BmpSlice = normal,
    val indentRatioLeft: Double = 0.0,
    val indentRatioRight: Double = 0.0,
    val indentRatioTop: Double = 0.0,
    val indentRatioBottom: Double = 0.0
)

fun IconSkin.calculateWidth(parentWidth: Double): Double {
    return parentWidth * (1.0 - indentRatioLeft - indentRatioRight)
}

fun IconSkin.calculateHeight(parentHeight: Double): Double {
    return parentHeight * (1.0 - indentRatioTop - indentRatioBottom)
}

fun IconSkin.paddingLeft(width: Double) = width * indentRatioLeft
fun IconSkin.paddingRight(width: Double) = width * indentRatioRight
fun IconSkin.paddingTop(height: Double) = height * indentRatioTop
fun IconSkin.paddingBottom(height: Double) = height * indentRatioBottom
