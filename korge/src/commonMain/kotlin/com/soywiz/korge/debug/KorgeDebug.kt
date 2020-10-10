package com.soywiz.korge.debug

import com.soywiz.korge.view.*
import com.soywiz.korui.*

interface KorgeDebugNode {
    fun buildDebugComponent(views: Views, container: UiContainer)
}
