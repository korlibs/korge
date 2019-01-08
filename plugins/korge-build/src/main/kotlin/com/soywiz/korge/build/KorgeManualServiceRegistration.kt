package com.soywiz.korge.build

import com.soywiz.korau.format.*
import com.soywiz.korge.build.atlas.*
import com.soywiz.korge.build.lipsync.*
import com.soywiz.korge.build.swf.*
import com.soywiz.korio.serialization.*
import com.soywiz.korio.util.*

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
			WAV
			//,MP3Decoder
			//,OGGDecoder
		)

		Mapper.jvmFallback()

		//defaultAudioFormats.registerStandard().registerMp3Decoder().registerOggVorbisDecoder()


		// KorgePlugins
		//defaultKorgePlugins.register(
		//	SwfPlugin,
		//	LipsyncPlugin,
		//	UIPlugin
		//)
	}
}
