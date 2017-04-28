package com.soywiz.korge.animate

import com.soywiz.korau.format.AudioData
import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.nativeSoundProvider
import com.soywiz.korge.animate.serialization.AnLibraryFile
import com.soywiz.korge.animate.serialization.readAni
import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.tween.interpolate
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.stream.FastByteArrayInputStream
import com.soywiz.korio.util.AsyncCache
import com.soywiz.korio.util.AsyncCacheItem
import com.soywiz.korio.util.Extra
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.VectorPath
import kotlin.collections.set

open class AnSymbol(
	val id: Int = -1,
	var name: String? = null
) : Extra by Extra.Mixin() {
	open fun create(library: AnLibrary): AnElement = AnEmptyView(library)
}

object AnSymbolEmpty : AnSymbol(-1, "")

class AnSymbolSound(id: Int, name: String?, private var inputSound: NativeSound?, val dataBytes: ByteArray?) : AnSymbol(id, name) {
	private val nativeSoundCache = AsyncCacheItem<NativeSound>()
	suspend fun getNativeSound(): NativeSound = nativeSoundCache {
		if (inputSound == null) {
			try {
				inputSound = nativeSoundProvider.createSound(dataBytes ?: byteArrayOf())
			} catch (e: Throwable) {
				inputSound = nativeSoundProvider.createSound(AudioData(44100, 2, shortArrayOf()))
			}
		}
		inputSound!!
	}
}


class AnTextFieldSymbol(id: Int, name: String?, val initialHtml: String, val bounds: Rectangle) : AnSymbol(id, name) {
	override fun create(library: AnLibrary): AnElement = AnTextField(library, this)
}

open class AnSymbolBaseShape(id: Int, name: String?, var bounds: Rectangle, val path: VectorPath? = null) : AnSymbol(id, name) {
}

class AnSymbolShape(id: Int, name: String?, bounds: Rectangle, var textureWithBitmap: TextureWithBitmapSlice?, path: VectorPath? = null) : AnSymbolBaseShape(id, name, bounds, path) {
	override fun create(library: AnLibrary): AnElement = AnShape(library, this)
}

class AnSymbolMorphShape(id: Int, name: String?, bounds: Rectangle, var texturesWithBitmap: Timed<TextureWithBitmapSlice> = Timed(), path: VectorPath? = null) : AnSymbolBaseShape(id, name, bounds, path) {
	override fun create(library: AnLibrary): AnElement = AnMorphShape(library, this)
}

class AnSymbolBitmap(id: Int, name: String?, val bmp: Bitmap) : AnSymbol(id, name) {
	//override fun create(library: AnLibrary): AnElement = AnShape(library, this)
}

class AnConstantPool(
	val stringPool: Array<String>
)

// @TODO: Matrix and ColorTransform pools? Maybe smaller size, reusability and possibility of compute values fast
data class AnSymbolTimelineFrame(
	var depth: Int = -1,
	var uid: Int = -1,
	var clipDepth: Int = -1,
	var ratio: Double = 0.0,
	var transform: Matrix2d = Matrix2d(),
	var name: String? = null,
	var colorTransform: ColorTransform = ColorTransform(),
	var blendMode: BlendMode = BlendMode.INHERIT
) {
	companion object {
		fun setToViewInterpolated(view: View, l: AnSymbolTimelineFrame, r: AnSymbolTimelineFrame, ratio: Double) {
			view.setMatrixInterpolated(ratio, l.transform, r.transform)
			view.colorTransform = view.colorTransform.setToInterpolated(l.colorTransform, r.colorTransform, ratio)
			view.ratio = interpolate(l.ratio, r.ratio, ratio)
			view.name = l.name
			view.blendMode = l.blendMode
		}
	}

	fun setToView(view: View) {
		view.ratio = ratio
		view.setMatrix(transform)
		view.name = name
		view.colorTransform = colorTransform
		view.blendMode = blendMode
	}

	fun copyFrom(other: AnSymbolTimelineFrame) {
		this.depth = other.depth
		this.uid = other.uid
		this.clipDepth = other.clipDepth
		this.ratio = other.ratio
		this.transform.copyFrom(other.transform)
		this.name = other.name
		this.colorTransform.copyFrom(other.colorTransform)
		this.blendMode = other.blendMode
	}

	//fun setToInterpolated(l: AnSymbolTimelineFrame, r: AnSymbolTimelineFrame, ratio: Double) {
	//	this.depth = l.depth
	//	this.uid = l.uid
	//	this.clipDepth = l.clipDepth
	//	this.ratio = ratio.interpolate(l.ratio, r.ratio)
	//	this.transform.setToInterpolated(l.transform, r.transform, ratio)
	//	this.name = l.name
	//	this.colorTransform.setToInterpolated(l.colorTransform, r.colorTransform, ratio)
	//	this.blendMode = l.blendMode
	//}
//
	//fun writeCompressedPack(s: SyncStream, cp: AnConstantPool) {
	//	val hasUid = uid >= 0
	//	val hasClipDepth = clipDepth >= 0
	//	val hasRatio = clipDepth >= 0
	//	val hasTransform = transform.getType() != Matrix2d.Type.IDENTITY
	//	val hasName = name != null
	//	val hasColorTransform = !colorTransform.isIdentity()
	//	val hasBlendMode = blendMode != BlendMode.INHERIT
	//	s.writeU_VL(0
	//		.insert(hasUid, 0)
	//		.insert(hasClipDepth, 1)
	//		.insert(hasRatio, 2)
	//		.insert(hasTransform, 3)
	//		.insert(hasName, 4)
	//		.insert(hasColorTransform, 5)
	//		.insert(hasBlendMode, 6)
	//	)
//
	//	if (hasUid) s.writeU_VL(uid)
	//	if (hasClipDepth) s.writeU_VL(clipDepth)
	//	if (hasRatio) s.write8((ratio * 255).toInt())
	//	// @TODO: optimized
	//	if (hasTransform) {
	//		val t = transform
	//		s.writeF32_le(t.a.toFloat())
	//		s.writeF32_le(t.b.toFloat())
	//		s.writeF32_le(t.c.toFloat())
	//		s.writeF32_le(t.d.toFloat())
	//		s.writeF32_le(t.tx.toFloat())
	//		s.writeF32_le(t.ty.toFloat())
	//	}
	//	// @TODO: Use constantpool to store just integer
	//	if (hasName) {
	//		s.writeStringVL(name!!)
	//	}
	//	// @TODO: optimized
	//	if (hasColorTransform) {
	//		val ct = colorTransform
	//		s.writeF32_le(ct.mRf)
	//		s.writeF32_le(ct.mGf)
	//		s.writeF32_le(ct.mBf)
	//		s.writeF32_le(ct.mAf)
	//		s.write32_le(ct.aR)
	//		s.write32_le(ct.aG)
	//		s.write32_le(ct.aB)
	//		s.write32_le(ct.aA)
	//	}
	//	if (hasBlendMode) {
	//		s.write8(blendMode.ordinal)
	//	}
	//}
//
	//fun readCompressedPack(s: FastByteArrayInputStream, cp: AnConstantPool) {
	//	val flags = s.readU_VL()
	//	val t = transform
	//	val ct = colorTransform
	//	uid = if (flags.extract(0)) s.readU_VL() else -1
	//	clipDepth = if (flags.extract(1)) s.readU_VL() else -1
	//	ratio = if (flags.extract(2)) s.readU8().toDouble() / 255.0 else 0.0
	//	if (flags.extract(3)) {
	//		t.setTo(
	//			s.readF32_le().toDouble(),
	//			s.readF32_le().toDouble(),
	//			s.readF32_le().toDouble(),
	//			s.readF32_le().toDouble(),
	//			s.readF32_le().toDouble(),
	//			s.readF32_le().toDouble()
	//		)
	//	} else {
	//		t.setToIdentity()
	//	}
	//	if (flags.extract(4)) {
	//		name = s.readStringVL()
	//	} else {
	//		name = null
	//	}
	//	if (flags.extract(5)) {
	//		name = s.readStringVL()
	//	} else {
	//		name = null
	//	}
	//	if (flags.extract(6)) {
	//		ct.setTo(
	//			s.readF32_le().toDouble(),
	//			s.readF32_le().toDouble(),
	//			s.readF32_le().toDouble(),
	//			s.readF32_le().toDouble(),
	//			s.readS32_le(),
	//			s.readS32_le(),
	//			s.readS32_le(),
	//			s.readS32_le()
	//		)
	//	} else {
	//		ct.setToIdentity()
	//	}
	//}
}

interface AnAction
data class AnPlaySoundAction(val soundId: Int) : AnAction

data class AnActions(val actions: List<AnAction>)

class AnDepthTimeline(val depth: Int) : Timed<AnSymbolTimelineFrame>()

class AnSymbolLimits(val totalDepths: Int, val totalFrames: Int, val totalUids: Int, val totalTime: Int)

class AnSymbolUidDef(val characterId: Int, val extraProps: MutableMap<String, String> = LinkedHashMap())

class AnSymbolMovieClipSubTimeline(totalDepths: Int) {
	//var name: String = "default"
	var totalTime: Int = 0

	//val totalTimeSeconds: Double get() = totalTime / 1_000_000.0
	//val totalTimeSeconds: Double get() = 100.0

	val timelines: Array<AnDepthTimeline> = Array<AnDepthTimeline>(totalDepths) { AnDepthTimeline(it) }
	val actions = Timed<AnActions>()

	var nextState: String? = null
	var nextStatePlay: Boolean = false
}

class AnSymbolMovieClipState(val name: String, val subTimeline: AnSymbolMovieClipSubTimeline, val startTime: Int)

class AnSymbolMovieClip(id: Int, name: String?, val limits: AnSymbolLimits) : AnSymbol(id, name) {
	var ninePatch: Rectangle? = null
	val states = hashMapOf<String, AnSymbolMovieClipState>()
	val uidInfo = Array(limits.totalUids) { AnSymbolUidDef(-1, hashMapOf()) }

	override fun create(library: AnLibrary): AnElement = AnMovieClip(library, this)
}

val Views.animateLibraryLoaders by Extra.Property {
	arrayListOf<KorgeFileLoaderTester<AnLibrary>>(
		KorgeFileLoaderTester("core/ani") { s, injector ->
			when {
				(s.readString(8) == AnLibraryFile.MAGIC) -> KorgeFileLoader("ani") { content, views -> this.readAni(views, content = content) }
				else -> null
			}
		}
	)
}

@AsyncFactoryClass(AnLibrary.Factory::class)
class AnLibrary(val views: Views, val width: Int, val height: Int, val fps: Double) : Extra by Extra.Mixin() {
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

	fun create(id: Int) = if (id < 0) TODO() else symbolsById.getOrElse(id) { AnSymbolEmpty }.create(this)
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
		val injector: AsyncInjector,
		val resourcesRoot: ResourcesRoot
	) : AsyncFactory<AnLibrary> {
		suspend override fun create(): AnLibrary {
			val file = resourcesRoot[path]
			val content = file.readAll()

			for (loader in views.animateLibraryLoaders) {
				val aloader = loader(FastByteArrayInputStream(content), injector) ?: continue

				return aloader.loader(file, FastByteArrayInputStream(content), views)
			}

			throw IllegalArgumentException("Don't know how to load an AnLibrary for file $file using loaders: ${views.animateLibraryLoaders}")
		}
	}
}

