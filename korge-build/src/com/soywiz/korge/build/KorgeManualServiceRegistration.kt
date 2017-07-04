package com.soywiz.korge.build

import com.soywiz.korau.awt.AwtNativeSoundProvider
import com.soywiz.korau.awt.AwtNativeSoundSpecialReader
import com.soywiz.korau.format.AudioFormat
import com.soywiz.korau.format.MP3Decoder
import com.soywiz.korau.format.OGGDecoder
import com.soywiz.korau.format.WAV
import com.soywiz.korau.sound.NativeSoundProvider
import com.soywiz.korge.build.atlas.AtlasResourceProcessor
import com.soywiz.korge.build.lipsync.LipsyncResourceProcessor
import com.soywiz.korge.build.swf.SwfResourceProcessor
import com.soywiz.korge.ext.lipsync.LipsyncPlugin
import com.soywiz.korge.ext.swf.SwfPlugin
import com.soywiz.korge.plugin.KorgePlugin
import com.soywiz.korim.awt.AwtImageSpecialReader
import com.soywiz.korim.awt.AwtNativeImageFormatProvider
import com.soywiz.korim.format.*
import com.soywiz.korio.async.EventLoopFactory
import com.soywiz.korio.service.Services
import com.soywiz.korio.vfs.*
import com.soywiz.korio.vfs.jvm.LocalVfsProviderJvm
import com.soywiz.korio.vfs.jvm.ResourcesVfsProviderJvm
import com.soywiz.korui.light.LightComponentsFactory
import com.soywiz.korui.light.awt.AwtLightComponentsFactory
import com.soywiz.korui.light.awt.EventLoopFactoryAwt

// Workaround since META-INF/services are not loaded
object KorgeManualServiceRegistration {
	fun register() {
		println("KorgeManualServiceRegistration.register")

		// Providers

		//Services.register(LocalVfsProvider::class.java, LocalVfsProviderJTransc::class.java)
		//Services.register(ResourcesVfsProvider::class.java, ResourcesVfsProviderJTransc::class.java)
		Services.register(LocalVfsProvider::class.java, LocalVfsProviderJvm::class.java)
		Services.register(ResourcesVfsProvider::class.java, ResourcesVfsProviderJvm::class.java)

		Services.register(NativeImageFormatProvider::class.java, AwtNativeImageFormatProvider::class.java)
		Services.register(NativeSoundProvider::class.java, AwtNativeSoundProvider::class.java)
		Services.register(LightComponentsFactory::class.java, AwtLightComponentsFactory::class.java)

		// ResourceProcessor
		Services.register(ResourceProcessor::class.java, SwfResourceProcessor::class.java)
		Services.register(ResourceProcessor::class.java, LipsyncResourceProcessor::class.java)
		Services.register(ResourceProcessor::class.java, AtlasResourceProcessor::class.java)

		// EventLoop
		Services.register(EventLoopFactory::class.java, EventLoopFactoryAwt::class.java)

		// Special readers
		Services.register(VfsSpecialReader::class.java, AwtImageSpecialReader::class.java)
		Services.register(VfsSpecialReader::class.java, AwtNativeSoundSpecialReader::class.java)

		// Image Formats
		Services.register(ImageFormat::class.java, PNG::class.java)
		Services.register(ImageFormat::class.java, JPEG::class.java)
		Services.register(ImageFormat::class.java, SVG::class.java)

		// Audio formats
		Services.register(AudioFormat::class.java, WAV::class.java)
		Services.register(AudioFormat::class.java, MP3Decoder::class.java)
		Services.register(AudioFormat::class.java, OGGDecoder::class.java)

		// KorgePlugins
		Services.register(KorgePlugin::class.java, SwfPlugin::class.java)
		Services.register(KorgePlugin::class.java, LipsyncPlugin::class.java)
	}
}
