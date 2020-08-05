package com.soywiz.korge.debug

import com.soywiz.korge.view.*

interface KorgeDebugNode {
    fun getDebugProperties(views: Views): EditableNode?
}
