package com.soywiz.korge.ext.lipsync

import com.soywiz.korge.plugin.KorgePlugin
import com.soywiz.korge.view.Views

class LipsyncPlugin : KorgePlugin() {
	suspend override fun register(views: Views) {
		println("LipsyncPlugin.register()")
		views.registerPropertyTrigger("lipsync") { view, key, value ->
			view.addComponent(LipSyncComponent(view))
		}
	}
}
