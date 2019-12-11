package com.soywiz.korge.newui

import com.soywiz.kds.*
import com.soywiz.korge.html.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*

@PublishedApi
internal var View.internalDefaultUISkin: UISkin? by extraProperty("defaultUiSkin") { null }
var View.defaultUISkin: UISkin
	set(value) = run { internalDefaultUISkin = value }
	get() = internalDefaultUISkin ?: parent?.defaultUISkin ?: DefaultUISkin

fun Container.uiSkin(skin: UISkin, block: Container.() -> Unit) {
	defaultUISkin = skin
	block()
}

data class UISkin(
	val normal: BmpSlice,
	val hover: BmpSlice,
	val down: BmpSlice,
	val backColor: RGBA = Colors.DARKGREY,
	val font: Html.FontFace = Html.FontFace.Named("Arial")
)
