package com.soywiz.korge.build

import com.soywiz.korau.format.*
import com.soywiz.korge.build.atlas.*
import com.soywiz.korge.build.lipsync.*
import com.soywiz.korge.build.swf.*
import com.soywiz.korge.ext.lipsync.*
import com.soywiz.korge.ext.swf.*
import com.soywiz.korge.ui.*
import com.soywiz.korim.format.*

// Workaround since META-INF/services are not loaded
object KorgeManualServiceRegistration {
	fun register() {
		println("KorgeManualServiceRegistration.register")

		// ResourceProcessor
		defaultResourceProcessors.register(
			SwfResourceProcessor, LipsyncResourceProcessor, AtlasResourceProcessor
		)

		// Audio formats
		defaultAudioFormats.register(
			WAV,
			MP3Decoder,
			OGGDecoder
		)

		// KorgePlugins
		defaultKorgePlugins.register(
			SwfPlugin,
			LipsyncPlugin,
			UIPlugin
		)
	}
}
