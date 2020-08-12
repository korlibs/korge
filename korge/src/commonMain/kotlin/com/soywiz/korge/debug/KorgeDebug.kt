package com.soywiz.korge.debug

import com.soywiz.korge.view.*
import com.soywiz.korui.*

interface KorgeDebugNode {
    fun UiContainer.buildDebugComponent(views: Views)
}

interface KorgeDebugNodeLeaf : KorgeDebugNode, ViewLeaf {
}
