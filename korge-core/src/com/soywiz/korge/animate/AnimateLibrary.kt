package com.soywiz.korge.animate

import com.soywiz.korau.format.AudioData
import com.soywiz.korge.animate.serialization.AnimateDeserializer
import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.Views
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.util.Extra
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.VectorPath

open class AnSymbol(
	val id: Int = -1,
	var name: String? = null
) : Extra by Extra.Mixin() {
	open fun create(library: AnLibrary): AnElement = TODO("$this")
}

object AnSymbolEmpty : AnSymbol(-1, "")

class AnSymbolSound(id: Int, name: String?, val data: AudioData?) : AnSymbol(id, name)

class AnTextFieldSymbol(id: Int, name: String?, val initialHtml: String, val bounds: Rectangle) : AnSymbol(id, name) {
	override fun create(library: AnLibrary): AnElement = AnTextField(library, this)
}

class AnSymbolShape(id: Int, name: String?, val bounds: Rectangle, var textureWithBitmap: TextureWithBitmapSlice?, val path: VectorPath? = null) : AnSymbol(id, name) {
	override fun create(library: AnLibrary): AnElement = AnShape(library, this)
}

class AnSymbolBitmap(id: Int, name: String?, val bmp: Bitmap) : AnSymbol(id, name) {
	//override fun create(library: AnLibrary): AnElement = AnShape(library, this)
}

class AnSymbolTimelineFrame(
	val uid: Int,
	val transform: Matrix2d.Computed,
	val name: String?,
	val alpha: Double,
	val blendMode: BlendMode
)

interface AnAction
data class AnPlaySoundAction(val soundId: Int) : AnAction

data class AnActions(val actions: List<AnAction>)

class AnDepthTimeline(val depth: Int) : Timed<AnSymbolTimelineFrame>()

class AnSymbolLimits(val totalDepths: Int, val totalFrames: Int, val totalUids: Int, val totalTime: Int)

class AnSymbolUidDef(val characterId: Int, val extraProps: MutableMap<String, String> = LinkedHashMap())

class AnSymbolMovieClipState(totalDepths: Int) {
	var name: String = "default"
	var totalTime: Int = 0
	val timelines: Array<AnDepthTimeline> = Array<AnDepthTimeline>(totalDepths) { AnDepthTimeline(it) }
	val actions = Timed<AnActions>()
	var loopStartTime: Int = 0

	fun calcEffectiveTime(time: Int): Int = if (time > totalTime) {
		val loopTime = (totalTime - loopStartTime)
		if (loopTime > 0) {
			loopStartTime + (time - totalTime) % loopTime
		} else {
			totalTime
		}
	} else {
		time
	}
}

class AnSymbolMovieClipStateWithStartTime(val state: AnSymbolMovieClipState, val startTime: Int)

class AnSymbolMovieClip(id: Int, name: String?, val limits: AnSymbolLimits) : AnSymbol(id, name) {
	val states = hashMapOf<String, AnSymbolMovieClipStateWithStartTime>()
	val uidInfo = Array(limits.totalUids) { AnSymbolUidDef(-1, hashMapOf()) }

	override fun create(library: AnLibrary): AnElement = AnMovieClip(library, this)
}

@AsyncFactoryClass(AnLibrary.Factory::class)
class AnLibrary(val views: Views, val fps: Double) {
	val msPerFrameDouble: Double = (1000 / fps)
	val msPerFrame: Int = msPerFrameDouble.toInt()
	var bgcolor: Int = 0xFFFFFFFF.toInt()
	val symbolsById = arrayListOf<AnSymbol>()
	val symbolsByName = hashMapOf<String, AnSymbol>()
	var defaultSmoothing = true
	//var defaultSmoothing = false

	fun addSymbol(symbol: AnSymbol) {
		while (symbolsById.size <= symbol.id) symbolsById += AnSymbolEmpty
		if (symbol.id >= 0) symbolsById[symbol.id] = symbol
	}

	fun processSymbolNames() {
		for (symbol in symbolsById) if (symbol.name != null) symbolsByName[symbol.name!!] = symbol
	}

	fun create(id: Int) = if (id < 0) TODO() else symbolsById[id].create(this)
	fun createShape(id: Int) = create(id) as AnShape
	fun createMovieClip(id: Int) = create(id) as AnMovieClip

	fun create(name: String) = symbolsByName[name]?.create(this) ?: invalidOp("Can't find symbol with name '$name'")
	fun createShape(name: String) = create(name) as AnShape
	fun createMovieClip(name: String) = create(name) as AnMovieClip

	fun getBitmap(id: Int) = (symbolsById[id] as AnSymbolBitmap).bmp
	fun getBitmap(name: String) = (symbolsByName[name] as AnSymbolBitmap).bmp

	fun createMainTimeLine() = createMovieClip(0)

	class Factory(
		val path: Path,
		val views: Views,
		val resourcesRoot: ResourcesRoot
	) : AsyncFactory<AnLibrary> {
		suspend override fun create(): AnLibrary = resourcesRoot[path].withExtension("ani").readAni(views)
	}
}

suspend fun VfsFile.readAni(views: Views) = AnimateDeserializer.read(this.read(), views)
