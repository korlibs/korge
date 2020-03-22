package com.soywiz.korge.ui

import com.soywiz.korge.html.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*

data class TextFormat(
    var color: RGBA = Colors.BLACK,
    var size: Int = 16,
    var font: Html.FontFace? = null
)

data class TextSkin(
    val normal: TextFormat,
    val over: TextFormat = normal,
    val down: TextFormat = normal,
    val disabled: TextFormat = normal,
    val backColor: RGBA = Colors.TRANSPARENT_BLACK
)

data class UISkin(
    val normal: BmpSlice,
    val over: BmpSlice = normal,
    val down: BmpSlice = normal,
    val disabled: BmpSlice = normal,
    val backColor: RGBA = Colors.DARKGREY
)

data class ComboBoxSkin(
    val itemSkin: UISkin,
    val selectedSkin: UISkin = itemSkin,
    val expandSkin: UISkin = itemSkin,
    val showIcon: IconSkin,
    val hideIcon: IconSkin,
    val scrollbarSkin: ScrollBarSkin,
    val textFont: Html.FontFace
)

data class ScrollBarSkin(
    val thumbSkin: UISkin,
    val upSkin: UISkin = thumbSkin,
    val downSkin: UISkin = thumbSkin,
    val upIcon: IconSkin,
    val downIcon: IconSkin,
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
