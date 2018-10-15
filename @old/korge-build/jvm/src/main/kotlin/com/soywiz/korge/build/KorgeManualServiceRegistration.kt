package com.soywiz.korge.build

import com.soywiz.korau.AwtNativeSoundSpecialReader
import com.soywiz.korau.format.MP3Decoder
import com.soywiz.korau.format.OGGDecoder
import com.soywiz.korau.format.WAV
import com.soywiz.korau.format.defaultAudioFormats
import com.soywiz.korge.build.atlas.AtlasResourceProcessor
import com.soywiz.korge.build.lipsync.LipsyncResourceProcessor
import com.soywiz.korge.build.swf.SwfResourceProcessor
import com.soywiz.korge.ext.lipsync.LipsyncPlugin
import com.soywiz.korge.ext.swf.SwfPlugin
import com.soywiz.korge.plugin.defaultKorgePlugins
import com.soywiz.korge.ui.UIPlugin
import com.soywiz.korim.awt.AwtImageSpecialReader
import com.soywiz.korim.format.JPEG
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.SVG
import com.soywiz.korim.format.defaultImageFormats
import com.soywiz.korio.vfs.registerVfsSpecialReader

// Workaround since META-INF/services are not loaded
object KorgeManualServiceRegistration {
	fun register() {
		println("KorgeManualServiceRegistration.register")

		// ResourceProcessor
		defaultResourceProcessors.register(
			SwfResourceProcessor, LipsyncResourceProcessor, AtlasResourceProcessor
		)

		// Special readers
		registerVfsSpecialReader(AwtImageSpecialReader())
		registerVfsSpecialReader(AwtNativeSoundSpecialReader())

		// Image Formats
		defaultImageFormats.register(
			PNG,
			JPEG,
			SVG
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
