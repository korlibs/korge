package com.soywiz.korge.animate.serialization

import com.soywiz.korge.animate.AnLibrary
import com.soywiz.korge.view.Views
import com.soywiz.korio.stream.SyncStream

object AnimateDeserializer {
	fun read(s: SyncStream, views: Views): AnLibrary = s.readLibrary()

	private fun SyncStream.readLibrary(): AnLibrary {
		//AnLibrary(views)
		TODO()
	}
}
