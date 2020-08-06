package com.soywiz.korge.intellij.korui

import com.intellij.openapi.ui.*
import com.soywiz.korui.native.*
import javax.swing.*

open class IdeaNativeUiFactory : BaseAwtUiFactory() {
    override fun createJPopupMenu(): JPopupMenu = JBPopupMenu()
}

