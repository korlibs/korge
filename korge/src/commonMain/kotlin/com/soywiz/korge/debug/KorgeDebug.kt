package com.soywiz.korge.debug

import com.soywiz.korge.view.Views
import com.soywiz.korui.UiContainer

interface KorgeDebugNode {
    fun buildDebugComponent(views: Views, container: UiContainer)
}
