package com.soywiz.korge

import com.soywiz.korau.sound.*
import com.soywiz.korge.view.*

internal actual fun completeViews(views: Views) {
    HtmlSimpleSound.unlock // Tries to unlock audio as soon as possible
}
