package com.soywiz.korge.intellij

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class KorgeBuildResourcesAction : AnAction() {
	override fun actionPerformed(anActionEvent: AnActionEvent) {
		println("KorgeBuildResourcesAction.actionPerformed")
	}
}
