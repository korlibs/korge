package com.soywiz.korge.ext.swf

import com.soywiz.korge.animate.animateLibraryLoaders
import com.soywiz.korge.plugin.KorgePlugin
import com.soywiz.korge.view.KorgeFileLoader
import com.soywiz.korge.view.KorgeFileLoaderTester
import com.soywiz.korge.view.Views
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readString

class SwfPlugin : KorgePlugin() {
	override suspend fun register(views: Views) {
		views.animateLibraryLoaders += KorgeFileLoaderTester("swf") { s: SyncStream ->
			val MAGIC = s.readString(3)
			when (MAGIC) {
				"FWS", "CWS", "ZWS" -> KorgeFileLoader("swf") { views -> this.readSWF(views) }
				else -> null
			}
		}
	}
}
