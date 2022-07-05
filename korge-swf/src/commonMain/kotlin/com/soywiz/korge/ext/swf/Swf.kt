package com.soywiz.korge.ext.swf

import com.soywiz.kds.Extra
import com.soywiz.korfl.as3swf.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.animate.serialization.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.dynamic.serialization.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.yaml.*
import kotlin.coroutines.*
import kotlin.native.concurrent.ThreadLocal

data class SWFExportConfig(
    val debug: Boolean = false,
    val mipmaps: Boolean = true,
    val antialiasing: Boolean = true,
    val rasterizerMethod: ShapeRasterizerMethod = ShapeRasterizerMethod.X4,
    val exportScale: Double = 1.0,
	//val exportScale: Double = 1.1,
    val minShapeSide: Int = 64,
    val maxShapeSide: Int = 512,
    val minMorphShapeSide: Int = 16,
    val maxMorphShapeSide: Int = 128,
    val maxTextureSide: Int = 4096,
    val exportPaths: Boolean = false,
    val adaptiveScaling: Boolean = true,
    val smoothInterpolation: Boolean = true,
    val atlasPacking: Boolean = true,
    val roundDecimalPlaces: Int = -1,
    val generateTextures: Boolean = false,
    //val generateTextures: Boolean = true,
    val graphicsRenderer: GraphicsRenderer = GraphicsRenderer.SYSTEM,
)

fun SWFExportConfig.toAnLibrarySerializerConfig(compression: Double = 1.0): AnLibrarySerializer.Config =
	AnLibrarySerializer.Config(
		compression = compression,
		keepPaths = this.exportPaths,
		mipmaps = this.mipmaps
	)

suspend fun VfsFile.readSWF(context: AnLibrary.Context, config: SWFExportConfig?): AnLibrary {
	return if (config != null) this.readSWF(context, config) else this.readSWF(context)
}

@ThreadLocal
var AnLibrary.swfExportConfig by Extra.Property { SWFExportConfig() }

suspend fun VfsFile.readSWF(
    views: Views,
    defaultConfig: SWFExportConfig = SWFExportConfig(),
    atlasPacking: Boolean? = null
): AnLibrary = readSWF(AnLibrary.Context(views), defaultConfig.copy(atlasPacking = atlasPacking ?: defaultConfig.atlasPacking))

suspend fun VfsFile.readSWF(
    context: AnLibrary.Context,
    content: ByteArray? = null,
    defaultConfig: SWFExportConfig = SWFExportConfig()
): AnLibrary {
	val configFile = this.appendExtension("config")
	val config = try {
		if (configFile.exists()) {
			Yaml.decodeToType(configFile.readString(), SWFExportConfig::class, Mapper)
		} else {
			defaultConfig
		}
	} catch (e: Throwable) {
		e.printStackTrace()
		SWFExportConfig()
	}
	val lib = readSWF(context.copy(coroutineContext = coroutineContext), config, content)
	lib.swfExportConfig = config
	return lib
}

suspend fun VfsFile.readSWF(context: AnLibrary.Context, config: SWFExportConfig, content: ByteArray? = null): AnLibrary =
	SwfLoaderMethod(context, config).load(content ?: this.readAll())

inline val TagPlaceObject.depth0: Int get() = this.depth - 1
inline val TagPlaceObject.clipDepth0: Int get() = this.clipDepth - 1
inline val TagRemoveObject.depth0: Int get() = this.depth - 1

@ThreadLocal
val SWF.bitmaps by Extra.Property { hashMapOf<Int, Bitmap>() }

class MySwfFrame(val index0: Int, maxDepths: Int) {
	var name: String? = null
	val depths = arrayListOf<AnSymbolTimelineFrame>()
	val actions = arrayListOf<Action>()

	interface Action {
		object Stop : Action
		object Play : Action
		class Goto(val frame0: Int) : Action
		class PlaySound(val soundId: Int) : Action
	}

	val isFirst: Boolean get() = index0 == 0
	val hasStop: Boolean get() = Action.Stop in actions
	val hasGoto: Boolean get() = actions.any { it is Action.Goto }
	val hasFlow: Boolean get() = hasStop || hasGoto

	fun stop() { actions += Action.Stop }
	fun play() { actions += Action.Play }
	fun goto(frame: Int) { actions += Action.Goto(frame) }
	fun gotoAndStop(frame: Int) { goto(frame); stop() }
	fun gotoAndPlay(frame: Int) { goto(frame); play() }
	fun playSound(soundId: Int) { actions += Action.PlaySound(soundId) }
}

class MySwfTimeline {
	val frames = arrayListOf<MySwfFrame>()
}

@ThreadLocal
internal val AnSymbolMovieClip.swfTimeline by Extra.Property { MySwfTimeline() }
@ThreadLocal
internal val AnSymbolMovieClip.labelsToFrame0 by Extra.Property { hashMapOf<String, Int>() }

@ThreadLocal
var AnSymbolMorphShape.tagDefineMorphShape by Extra.Property<TagDefineMorphShape?> { null }
@ThreadLocal
var AnSymbolShape.tagDefineShape by Extra.Property<TagDefineShape?> { null }
