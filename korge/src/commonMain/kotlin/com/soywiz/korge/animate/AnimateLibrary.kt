@file:OptIn(KorgeInternal::class)

package com.soywiz.korge.animate

import com.soywiz.kds.Extra
import com.soywiz.kds.extraCache
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.korau.sound.AudioData
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.nativeSoundProvider
import com.soywiz.korge.animate.serialization.AniFile
import com.soywiz.korge.animate.serialization.readAni
import com.soywiz.korge.html.Html
import com.soywiz.korge.internal.KorgeInternal
import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.GraphicsRenderer
import com.soywiz.korge.view.KorgeFileLoader
import com.soywiz.korge.view.KorgeFileLoaderTester
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.filter.filter
import com.soywiz.korge.view.filter.Filter
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.color.ColorTransform
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.ImageFormat
import com.soywiz.korim.format.RegisteredImageFormats
import com.soywiz.korim.vector.Shape
import com.soywiz.korio.lang.invalidOp
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korio.util.AsyncOnce
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.interpolation.interpolate
import kotlinx.coroutines.CancellationException
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

open class AnSymbol(
	val id: Int = -1,
	var name: String? = null
) : Extra by Extra.Mixin() {
	open fun create(library: AnLibrary): AnElement = AnEmptyView(library)

	override fun toString(): String = "Symbol(id=$id, name=$name)"
}

object AnSymbolEmpty : AnSymbol(-1, "")

class AnSymbolButton(id: Int, name: String?) : AnSymbol(id, name) {
}

class AnSymbolVideo(id: Int, name: String?) : AnSymbol(id, name) {
}

class AnSymbolSound(id: Int, name: String?, private var inputSound: Sound?, val dataBytes: ByteArray?) :
	AnSymbol(id, name) {
	private val nativeSoundCache = AsyncOnce<Sound>()
	suspend fun getNativeSound(): Sound = nativeSoundCache {
		if (inputSound == null) {
			inputSound = try {
				nativeSoundProvider.createSound(dataBytes ?: byteArrayOf())
			} catch (e: Throwable) {
                if (e is CancellationException) throw e
				nativeSoundProvider.createSound(AudioData.DUMMY)
			}
		}
		inputSound!!
	}
}


class AnTextFieldSymbol(id: Int, name: String?, val initialHtml: String, val bounds: Rectangle) : AnSymbol(id, name) {
	override fun create(library: AnLibrary): AnElement = AnTextField(library, this)
}

open class AnSymbolBaseShape(id: Int, name: String?, var bounds: Rectangle, val path: VectorPath? = null) :
	AnSymbol(id, name) {
}

class AnSymbolShape(
    id: Int,
    name: String?,
    bounds: Rectangle,
    var textureWithBitmap: TextureWithBitmapSlice?,
    path: VectorPath? = null,
    var shapeGen: (() -> Shape)? = null,
    var graphicsRenderer: GraphicsRenderer = GraphicsRenderer.GPU,
) : AnSymbolBaseShape(id, name, bounds, path) {
	override fun create(library: AnLibrary): AnElement = AnShape(library, this)
}

class AnSymbolMorphShape(
	id: Int,
	name: String?,
	bounds: Rectangle,
	var texturesWithBitmap: Timed<TextureWithBitmapSlice> = Timed(),
	path: VectorPath? = null,
    var shapeGen: ((ratio: Double) -> Shape)? = null,
    var graphicsRenderer: GraphicsRenderer = GraphicsRenderer.GPU,
) : AnSymbolBaseShape(id, name, bounds, path) {
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
	var transform: Matrix = Matrix(),
	var name: String? = null,
	var colorTransform: ColorTransform = ColorTransform(),
	var blendMode: BlendMode = BlendMode.INHERIT,
    var filter: Filter? = null
) {
	fun setToInterpolated(l: AnSymbolTimelineFrame, r: AnSymbolTimelineFrame, ratio: Double) {
		this.transform.setToInterpolated(ratio, l.transform, r.transform)
		this.colorTransform.setToInterpolated(ratio, l.colorTransform, r.colorTransform)
		this.ratio = ratio.interpolate(l.ratio, r.ratio)
		this.name = l.name
		this.blendMode = l.blendMode
        this.filter = l.filter
	}

	companion object {
		fun setToViewInterpolated(view: View, l: AnSymbolTimelineFrame, r: AnSymbolTimelineFrame, ratio: Double) {
			view.setMatrixInterpolated(ratio, l.transform, r.transform)
			view.colorTransform = view.colorTransform.setToInterpolated(ratio, l.colorTransform, r.colorTransform)
			view.ratio = ratio.interpolate(l.ratio, r.ratio)
			view.name = l.name
			view.blendMode = l.blendMode
            view.filter = l.filter
		}
	}

	fun setToView(view: View) {
		view.ratio = ratio
		view.setMatrix(transform)
		view.name = name
		view.colorTransform = colorTransform
		view.blendMode = blendMode
        view.filter = filter
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
        this.filter = other.filter
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
	//		s.writeF32LE(t.a.toFloat())
	//		s.writeF32LE(t.b.toFloat())
	//		s.writeF32LE(t.c.toFloat())
	//		s.writeF32LE(t.d.toFloat())
	//		s.writeF32LE(t.tx.toFloat())
	//		s.writeF32LE(t.ty.toFloat())
	//	}
	//	// @TODO: Use constantpool to store just integer
	//	if (hasName) {
	//		s.writeStringVL(name!!)
	//	}
	//	// @TODO: optimized
	//	if (hasColorTransform) {
	//		val ct = colorTransform
	//		s.writeF32LE(ct.mRf)
	//		s.writeF32LE(ct.mGf)
	//		s.writeF32LE(ct.mBf)
	//		s.writeF32LE(ct.mAf)
	//		s.write32LE(ct.aR)
	//		s.write32LE(ct.aG)
	//		s.write32LE(ct.aB)
	//		s.write32LE(ct.aA)
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
	//			s.readF32LE().toDouble(),
	//			s.readF32LE().toDouble(),
	//			s.readF32LE().toDouble(),
	//			s.readF32LE().toDouble(),
	//			s.readF32LE().toDouble(),
	//			s.readF32LE().toDouble()
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
	//			s.readF32LE().toDouble(),
	//			s.readF32LE().toDouble(),
	//			s.readF32LE().toDouble(),
	//			s.readF32LE().toDouble(),
	//			s.readS32LE(),
	//			s.readS32LE(),
	//			s.readS32LE(),
	//			s.readS32LE()
	//		)
	//	} else {
	//		ct.setToIdentity()
	//	}
	//}
}

interface AnAction
data class AnPlaySoundAction(val soundId: Int) : AnAction
data class AnEventAction(val event: String) : AnAction

class AnDepthTimeline(val depth: Int) : Timed<AnSymbolTimelineFrame>()

class AnSymbolLimits constructor(val totalDepths: Int, val totalFrames: Int, val totalUids: Int, val totalTime: TimeSpan)

class AnSymbolUidDef(val characterId: Int, val extraProps: MutableMap<String, String> = LinkedHashMap())

class AnSymbolMovieClipSubTimeline(totalDepths: Int) {
	//var name: String = "default"
    var totalTime = 0.milliseconds

	//val totalTimeSeconds: Double get() = totalTime / 1_000_000.0
	//val totalTimeSeconds: Double get() = 100.0

	val timelines: Array<AnDepthTimeline> = Array<AnDepthTimeline>(totalDepths) { AnDepthTimeline(it) }
	val actions = Timed<AnAction>()

	var nextState: String? = null
	var nextStatePlay: Boolean = false
}

class AnSymbolMovieClipState(val name: String, val subTimeline: AnSymbolMovieClipSubTimeline, val startTime: TimeSpan)

class AnSymbolMovieClip(id: Int, name: String?, val limits: AnSymbolLimits) : AnSymbol(id, name) {
	var ninePatch: Rectangle? = null
	val states = LinkedHashMap<String, AnSymbolMovieClipState>()
	val uidInfo = Array(limits.totalUids) { AnSymbolUidDef(-1, LinkedHashMap()) }

	override fun create(library: AnLibrary): AnElement = AnMovieClip(library, this)
}

// Do not use by extraProperty because it would be included in the output
val Views.animateLibraryLoaders get() = extraCache("animateLibraryLoaders") {
    arrayListOf(
        KorgeFileLoaderTester("core/ani") { s, injector ->
            when {
                (s.readString(8) == AniFile.MAGIC) -> KorgeFileLoader("ani") { content, views ->
                    this.readAni(
                        AnLibrary.Context(views),
                        content = content
                    )
                }
                else -> null
            }
        }
    )
}

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(AnLibrary.Factory::class)
class AnLibrary(val context: Context, val width: Int, val height: Int, val fps: Double) : Extra by Extra.Mixin() {
    var graphicsRenderer: GraphicsRenderer? = null
    val fontsCatalog = Html.FontsCatalog(null)

    data class Context(
        val coroutineContext: CoroutineContext = EmptyCoroutineContext,
        val imageFormats: ImageFormat = RegisteredImageFormats,
    ) {
        companion object {
            operator fun invoke(views: Views) = Context(views.coroutineContext, views.imageFormats)
        }
    }

	val msPerFrameDouble: Double = (1000 / fps)
	val msPerFrame: Int = msPerFrameDouble.toInt()
	var bgcolor: RGBA = Colors.WHITE
	val symbolsById = arrayListOf<AnSymbol>()
	val symbolsByName = LinkedHashMap<String, AnSymbol>()
	var defaultSmoothing = true
	//var defaultSmoothing = false

	fun addSymbol(symbol: AnSymbol) {
		while (symbolsById.size <= symbol.id) symbolsById += AnSymbolEmpty
		if (symbol.id >= 0) symbolsById[symbol.id] = symbol
	}

	fun processSymbolNames() {
		symbolsById.fastForEach { symbol ->
			if (symbol.name != null) symbolsByName[symbol.name!!] = symbol
		}
	}

	fun AnSymbol.findFirstTexture(): BmpSlice? {
		when (this) {
			is AnSymbolEmpty -> return null
			is AnSymbolSound -> return null
			is AnTextFieldSymbol -> return null
			is AnSymbolShape -> return this.textureWithBitmap?.texture
			is AnSymbolMorphShape -> return this.texturesWithBitmap.objects.firstOrNull()?.texture
			is AnSymbolBitmap -> return null
			is AnSymbolMovieClip -> {
				this.uidInfo.fastForEach { uid ->
					val res = create(uid.characterId).findFirstTexture()
					if (res != null) return res
				}
				return null
			}
			else -> throw RuntimeException("Don't know how to handle ${this::class}")
		}
	}

	fun AnElement.findFirstTexture(): BmpSlice? = this.symbol.findFirstTexture()

	fun create(id: Int): AnElement = when {
        id < 0 -> {
            printStackTrace("ERROR invalid id=$id")
            AnSymbolEmpty.create(this)
            //TODO("ERROR invalid id=$id")
        }
        else -> symbolsById.getOrElse(id) { AnSymbolEmpty }.create(this)
    }
	fun createShape(id: Int) = create(id) as AnShape
	fun createMovieClip(id: Int) = create(id) as AnMovieClip
	fun getTexture(id: Int) = create(id).findFirstTexture()

	fun create(name: String) = symbolsByName[name]?.create(this) ?: invalidOp("Can't find symbol with name '$name'")
	fun createShape(name: String) = create(name) as AnShape
	fun createMovieClip(name: String) = create(name) as AnMovieClip
	fun getTexture(name: String) = create(name).findFirstTexture()

	fun getBitmap(id: Int) = (symbolsById[id] as AnSymbolBitmap).bmp
	fun getBitmap(name: String) = (symbolsByName[name] as AnSymbolBitmap).bmp

    val mainTimeLineInfo: AnSymbolMovieClip get() = symbolsById[0] as AnSymbolMovieClip

	fun createMainTimeLine(): AnMovieClip = createMovieClip(0)
}
