package com.soywiz.korge.ui

import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.util.encoding.*

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
    val paddingLeft: Double = 8.0,
    val paddingRight: Double = 8.0,
    val paddingTop: Double = 8.0,
    val paddingBottom: Double = 8.0
)

val DEFAULT_UI_SKIN_IMG by lazy {
    PNG.decode(
        "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".fromBase64()
    ).toBMP32()
}

val DefaultUISkin by lazy {
    UISkin(
        normal = DEFAULT_UI_SKIN_IMG.sliceWithSize(0, 0, 64, 64),
        over = DEFAULT_UI_SKIN_IMG.sliceWithSize(64, 0, 64, 64),
        down = DEFAULT_UI_SKIN_IMG.sliceWithSize(128, 0, 64, 64),
        disabled = DEFAULT_UI_SKIN_IMG.sliceWithSize(192, 0, 64, 64)
    )
}

val DefaultCheckSkin by lazy {
    IconSkin(
        normal = DEFAULT_UI_SKIN_IMG.sliceWithSize(0, 64, 44, 33),
        disabled = DEFAULT_UI_SKIN_IMG.sliceWithSize(44, 64, 44, 33),
        paddingLeft = 7.0,
        paddingRight = 7.0
    )
}

var View.defaultUISkin: UISkin by defaultElement(DefaultUISkin)
