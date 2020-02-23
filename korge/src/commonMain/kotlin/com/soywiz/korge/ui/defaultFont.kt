package com.soywiz.korge.ui

import com.soywiz.korge.html.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*

val DefaultUIFont by lazy {
    Html.FontFace.Bitmap(debugBmpFont)
}

var View.defaultUIFont: Html.FontFace by defaultElement(DefaultUIFont)
