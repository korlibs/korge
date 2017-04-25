package com.soywiz.korge.animate.serialization

import com.soywiz.korge.animate.*
import com.soywiz.korge.render.Texture
import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.ColorTransform
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.texture
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.format.readBitmapOptimized
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.stream.FastByteArrayInputStream
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.util.extract
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.ds.DoubleArrayList
import com.soywiz.korma.ds.IntArrayList
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.VectorPath

suspend fun VfsFile.readAni(views: Views, mipmaps: Boolean = false): AnLibrary {
	return this.readAni(FastByteArrayInputStream(this.readBytes()), views, mipmaps)
}

suspend fun VfsFile.readAni(content: FastByteArrayInputStream, views: Views, mipmaps: Boolean = false): AnLibrary {
	val file = this
	return AnLibraryDeserializer.read(content, views, mipmaps) { index ->
		file.withExtension("ani.$index.png").readBitmapOptimized()
	}
}

object AnLibraryDeserializer {
	suspend fun read(s: ByteArray, views: Views, mipmaps: Boolean = false, atlasReader: suspend (index: Int) -> Bitmap): AnLibrary = FastByteArrayInputStream(s).readLibrary(views, mipmaps, atlasReader)
	suspend fun read(s: SyncStream, views: Views, mipmaps: Boolean = false, atlasReader: suspend (index: Int) -> Bitmap): AnLibrary = FastByteArrayInputStream(s.readAll()).readLibrary(views, mipmaps, atlasReader)
	suspend fun read(s: FastByteArrayInputStream, views: Views, mipmaps: Boolean = false, atlasReader: suspend (index: Int) -> Bitmap): AnLibrary = s.readLibrary(views, mipmaps, atlasReader)


	suspend private fun FastByteArrayInputStream.readLibrary(views: Views, mipmaps: Boolean, atlasReader: suspend (index: Int) -> Bitmap): AnLibrary {
		val magic = readStringz(8)
		//AnLibrary(views)
		if (magic != AnLibraryFile.MAGIC) invalidOp("Not a ${AnLibraryFile.MAGIC} file")
		if (readU_VL() != AnLibraryFile.VERSION) invalidOp("Just supported ${AnLibraryFile.MAGIC} version ${AnLibraryFile.VERSION}")
		val msPerFrame = readU_VL()
		val library = AnLibrary(views, 1000.0 / msPerFrame)

		val strings = arrayOf<String?>(null) + (1 until readU_VL()).map { readStringVL() }

		val atlases = (0 until readU_VL()).map { index ->
			//val format = readU_VL()
			//val width = readU_VL()
			//val height = readU_VL()
			//val size = readU_VL()
			//val data = readBytes(size)
			val bmp = atlasReader(index)
			bmp to views.texture(bmp, mipmaps = mipmaps)
		}

		val sounds = (0 until readU_VL()).map {
			Unit
		}

		val fonts = (0 until readU_VL()).map {
			Unit
		}

		val symbols = (0 until readU_VL()).map {
			readSymbol(strings, atlases)
		}

		for (symbol in symbols) library.addSymbol(symbol)
		library.processSymbolNames()

		return library
	}

	private fun FastByteArrayInputStream.readSymbol(strings: Array<String?>, atlases: List<Pair<Bitmap, Texture>>): AnSymbol {
		val symbolId = readU_VL()
		val symbolName = strings[readU_VL()]
		val type = readU_VL()
		val symbol: AnSymbol = when (type) {
			AnLibraryFile.SYMBOL_TYPE_EMPTY -> AnSymbolEmpty
			AnLibraryFile.SYMBOL_TYPE_SOUND -> {
				AnSymbolSound(symbolId, symbolName, null)
			}
			AnLibraryFile.SYMBOL_TYPE_TEXT -> {
				val initialText = strings[readU_VL()]
				val bounds = readRect()
				AnTextFieldSymbol(symbolId, symbolName, initialText ?: "", bounds)
			}
			AnLibraryFile.SYMBOL_TYPE_SHAPE -> {
				val scale = readF32_le().toDouble()
				val bitmapId = readU_VL()
				val atlas = atlases[bitmapId]
				val textureBounds = readIRect()
				val bounds = readRect()
				val bitmap = atlas.first
				val texture = atlas.second

				val path: VectorPath? = when (readU_VL()) {
					0 -> null
					1 -> {
						val cmds = IntArray(readU_VL())
						for (n in 0 until cmds.size) cmds[n] = readU8()

						val data = DoubleArray(readU_VL())
						for (n in 0 until data.size) data[n] = readF32_le().toDouble()

						//val cmds = (0 until readU_VL()).map { readU8() }.toIntArray()
						//val data = (0 until readU_VL()).map { readF32_le().toDouble() }.toDoubleArray()
						VectorPath(IntArrayList(cmds), DoubleArrayList(data))
					}
					else -> null
				}
				AnSymbolShape(
					id = symbolId,
					name = symbolName,
					bounds = bounds,
					textureWithBitmap = TextureWithBitmapSlice(
						texture = texture.slice(textureBounds.toDouble()),
						bitmapSlice = bitmap.slice(textureBounds),
						scale = scale,
						bounds = bounds
					),
					path = path
				)
			}
			AnLibraryFile.SYMBOL_TYPE_MORPH_SHAPE -> {
				val nframes = readU_VL()
				val texturesWithBitmap = Timed<TextureWithBitmapSlice>(nframes)
				for (n in 0 until nframes) {
					val ratio1000 = readU_VL()
					val scale = readF32_le().toDouble()
					val bitmapId = readU_VL()
					val bounds = readRect()
					val textureBounds = readIRect()
					val atlas = atlases[bitmapId]
					val bitmap = atlas.first
					val texture = atlas.second

					texturesWithBitmap.add(ratio1000, TextureWithBitmapSlice(
						texture = texture.slice(textureBounds.toDouble()),
						bitmapSlice = bitmap.slice(textureBounds),
						scale = scale,
						bounds = bounds
					))
				}
				AnSymbolMorphShape(
					id = symbolId,
					name = symbolName,
					bounds = Rectangle(),
					texturesWithBitmap = texturesWithBitmap,
					path = null
				)
			}
			AnLibraryFile.SYMBOL_TYPE_BITMAP -> {
				AnSymbolBitmap(symbolId, symbolName, Bitmap32(1, 1))
			}
			AnLibraryFile.SYMBOL_TYPE_MOVIE_CLIP -> {
				readMovieClip(symbolId, symbolName, strings)
			}
			else -> TODO("Type: $type")
		}
		return symbol
	}

	private fun FastByteArrayInputStream.readMovieClip(symbolId: Int, symbolName: String?, strings: Array<String?>): AnSymbolMovieClip {
		val totalDepths = readU_VL()
		val totalFrames = readU_VL()
		val totalTime = readU_VL()
		val totalUids = readU_VL()
		val uidsToCharacterIds = (0 until totalUids).map {
			val charId = readU_VL()
			val extraPropsString = readStringVL()
			val extraProps = if (extraPropsString.isEmpty()) LinkedHashMap<String, String>() else Json.decode(extraPropsString) as MutableMap<String, String>
			//val extraProps = LinkedHashMap<String, String>()
			AnSymbolUidDef(charId, extraProps)
		}.toTypedArray()
		val mc = AnSymbolMovieClip(symbolId, symbolName, AnSymbolLimits(totalDepths, totalFrames, totalUids, totalTime))

		val symbolStates = (0 until readU_VL()).map {
			val ss = AnSymbolMovieClipState(totalDepths)
			//ss.name = strings[readU_VL()] ?: ""
			ss.totalTime = readU_VL()
			ss.loopStartTime = readU_VL()
			for (depth in 0 until totalDepths) {
				val timeline = ss.timelines[depth]
				var lastUid = -1
				var lastName: String? = null
				var lastColorTransform: ColorTransform = ColorTransform()
				var lastMatrix: Matrix2d = Matrix2d()
				var lastClipDepth = -1
				var lastRatio = 0.0
				for (frameIndex in 0 until readU_VL()) {
					val frameTime = readU_VL()
					val flags = readU_VL()
					val hasUid = flags.extract(0)
					val hasName = flags.extract(1)
					val hasColorTransform = flags.extract(2)
					val hasMatrix = flags.extract(3)
					val hasClipDepth = flags.extract(4)
					val hasRatio = flags.extract(5)

					if (hasUid) lastUid = readU_VL()
					if (hasClipDepth) lastClipDepth = readS16_le()
					if (hasName) lastName = strings[readU_VL()]
					if (hasColorTransform) {
						val ct = lastColorTransform.copy()
						val ctFlags = readU8()
						if (ctFlags.extract(0)) ct.mR = readF32_le().toDouble()
						if (ctFlags.extract(1)) ct.mG = readF32_le().toDouble()
						if (ctFlags.extract(2)) ct.mB = readF32_le().toDouble()
						if (ctFlags.extract(3)) ct.mA = readF32_le().toDouble()
						if (ctFlags.extract(4)) ct.aR = readS16_le()
						if (ctFlags.extract(5)) ct.aG = readS16_le()
						if (ctFlags.extract(6)) ct.aB = readS16_le()
						if (ctFlags.extract(7)) ct.aR = readS16_le()
						lastColorTransform = ct
					}
					if (hasMatrix) {
						val lm = lastMatrix.copy()
						val matrixFlags = readU8()
						if (matrixFlags.extract(0)) lm.a = readF32_le().toDouble()
						if (matrixFlags.extract(1)) lm.b = readF32_le().toDouble()
						if (matrixFlags.extract(2)) lm.c = readF32_le().toDouble()
						if (matrixFlags.extract(3)) lm.d = readF32_le().toDouble()
						if (matrixFlags.extract(4)) lm.tx = readF32_le().toDouble()
						if (matrixFlags.extract(5)) lm.ty = readF32_le().toDouble()
						lastMatrix = lm
					}
					if (hasRatio) lastRatio = readF32_le().toDouble()
					timeline.add(frameTime, AnSymbolTimelineFrame(
						depth = depth,
						uid = lastUid,
						transform = lastMatrix,
						name = lastName,
						colorTransform = lastColorTransform,
						blendMode = BlendMode.INHERIT,
						ratio = lastRatio,
						clipDepth = lastClipDepth
					))
				}
			}
			ss
		}

		for (n in 0 until uidsToCharacterIds.size) mc.uidInfo[n] = uidsToCharacterIds[n]
		mc.states += (0 until readU_VL()).map {
			val name = strings[readU_VL()] ?: ""
			val startTime = readU_VL()
			val stateIndex = readU_VL()
			name to AnSymbolMovieClipStateWithStartTime(name, symbolStates[stateIndex], startTime = startTime)
		}.toMap()

		return mc
	}

	fun FastByteArrayInputStream.readRect() = Rectangle(x = readF32_le(), y = readF32_le(), width = readF32_le(), height = readF32_le())
	fun FastByteArrayInputStream.readIRect() = RectangleInt(x = readF32_le().toInt(), y = readF32_le().toInt(), width = readF32_le().toInt(), height = readF32_le().toInt())
}
