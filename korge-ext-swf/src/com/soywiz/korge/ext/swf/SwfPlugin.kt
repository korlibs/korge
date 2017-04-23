package com.soywiz.korge.ext.swf

import com.soywiz.korge.animate.animateLibraryLoaders
import com.soywiz.korge.plugin.KorgePlugin
import com.soywiz.korge.resources.Mipmaps
import com.soywiz.korge.view.KorgeFileLoader
import com.soywiz.korge.view.KorgeFileLoaderTester
import com.soywiz.korge.view.Views
import com.soywiz.korio.stream.readString

class SwfPlugin : KorgePlugin() {
	override suspend fun register(views: Views) {
		views.animateLibraryLoaders += KorgeFileLoaderTester("swf") { s, injector ->
			val mipmaps = injector.getOrNull(Mipmaps::class.java)?.mipmaps ?: false
			val MAGIC = s.readString(3)
			when (MAGIC) {
				"FWS", "CWS", "ZWS" -> KorgeFileLoader("swf") { views -> this.readSWF(views, mipmaps = mipmaps) }
				else -> null
			}
		}
	}
}
