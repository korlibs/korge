package com.soywiz.korge.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.*
import java.awt.Desktop
import java.net.URI

class KorgeSponsorAction : AnAction(), DumbAware {
	override fun actionPerformed(p0: AnActionEvent) {
		Desktop.getDesktop().browse(URI.create("https://github.com/sponsors/soywiz"))
	}
}
