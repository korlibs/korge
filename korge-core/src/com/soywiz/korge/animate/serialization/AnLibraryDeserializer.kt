package com.soywiz.korge.animate.serialization

import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.readNativeSoundOptimized
import com.soywiz.korge.animate.*
import com.soywiz.korge.render.Texture
import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.texture
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.color.ColorTransform
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

suspend fun VfsFile.readAni(views: Views, content: FastByteArrayInputStream? = null): AnLibrary {
	val file = this
	return AnLibraryDeserializer.read(content ?: FastByteArrayInputStream(this.readBytes()), views, externalReaders = AnLibraryDeserializer.ExternalReaders(
		atlasReader = { index ->
			file.withExtension("ani.$index.png").readBitmapOptimized()
		},
		readSound = { index ->
			file.withExtension("ani.$index.mp3").readNativeSoundOptimized()
		}
	))
}

object AnLibraryDeserializer {
	class ExternalReaders(
		val atlasReader: suspend (index: Int) -> Bitmap,
		val readSound: suspend (index: Int) -> NativeSound
	)

	suspend fun read(s: ByteArray, views: Views, externalReaders: ExternalReaders): AnLibrary = FastByteArrayInputStream(s).readLibrary(views, externalReaders)
	suspend fun read(s: SyncStream, views: Views, externalReaders: ExternalReaders): AnLibrary = FastByteArrayInputStream(s.readAll()).readLibrary(views, externalReaders)
	suspend fun read(s: FastByteArrayInputStream, views: Views, externalReaders: ExternalReaders): AnLibrary = s.readLibrary(views, externalReaders)

	suspend private fun FastByteArrayInputStream.readLibrary(views: Views, externalReaders: ExternalReaders): AnLibrary {
		val magic = readStringz(8)
		//AnLibrary(views)
		if (magic != AniFile.MAGIC) invalidOp("Not a ${AniFile.MAGIC} file")
		if (readU_VL() != AniFile.VERSION) invalidOp("Just supported ${AniFile.MAGIC} version ${AniFile.VERSION}")
		val msPerFrame = readU_VL()
		val width = readU_VL()
		val height = readU_VL()
		val fileFlags = readU_VL()
		val mipmaps = fileFlags.extract(0)
		val smoothInterpolation = !fileFlags.extract(1)

		val library = AnLibrary(views, width, height, 1000.0 / msPerFrame).apply {
			this.defaultSmoothing = smoothInterpolation
		}

		val strings = arrayOf<String?>(null) + (1 until readU_VL()).map { readStringVL() }

		val atlases = (0 until readU_VL()).map { index ->
			//val format = readU_VL()
			//val width = readU_VL()
			//val height = readU_VL()
			//val size = readU_VL()
			//val data = readBytes(size)
			val bmp = externalReaders.atlasReader(index)
			bmp to views.texture(bmp, mipmaps = mipmaps)
		}

		val sounds = (0 until readU_VL()).map { index ->
			externalReaders.readSound(index)
		}

		val symbols = (0 until readU_VL()).map {
			readSymbol(strings, atlases, sounds)
		}

		for (symbol in symbols) library.addSymbol(symbol)
		library.processSymbolNames()

		return library
	}

	private fun FastByteArrayInputStream.readSymbol(strings: Array<String?>, atlases: List<Pair<Bitmap, Texture>>, sounds: List<NativeSound>): AnSymbol {
		val symbolId = readU_VL()
		val symbolName = strings[readU_VL()]
		val type = readU_VL()
		val symbol: AnSymbol = when (type) {
			AniFile.SYMBOL_TYPE_EMPTY -> AnSymbolEmpty
			AniFile.SYMBOL_TYPE_SOUND -> {
				val soundId = readU_VL()
				AnSymbolSound(symbolId, symbolName, sounds[soundId], null)
			}
			AniFile.SYMBOL_TYPE_TEXT -> {
				val initialText = strings[readU_VL()]
				val bounds = readRect()
				AnTextFieldSymbol(symbolId, symbolName, initialText ?: "", bounds)
			}
			AniFile.SYMBOL_TYPE_SHAPE -> {
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
			AniFile.SYMBOL_TYPE_MORPH_SHAPE -> {
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
			AniFile.SYMBOL_TYPE_BITMAP -> {
				AnSymbolBitmap(symbolId, symbolName, Bitmap32(1, 1))
			}
			AniFile.SYMBOL_TYPE_MOVIE_CLIP -> {
				readMovieClip(symbolId, symbolName, strings)
			}
			else -> TODO("Type: $type")
		}
		return symbol
	}

	private fun FastByteArrayInputStream.readMovieClip(symbolId: Int, symbolName: String?, strings: Array<String?>): AnSymbolMovieClip {
		val mcFlags = readU8()

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

		if (mcFlags.extract(0)) {
			mc.ninePatch = readRect()
		}

		val symbolStates = (0 until readU_VL()).map {
			val ss = AnSymbolMovieClipSubTimeline(totalDepths)
			//ss.name = strings[readU_VL()] ?: ""
			ss.totalTime = readU_VL()
			val stateFlags = readU8()
			ss.nextStatePlay = stateFlags.extract(0)
			ss.nextState = strings[readU_VL()]

			val numberOfActionFrames = readU_VL()
			var lastFrameTimeInMs = 0
			for (n in 0 until numberOfActionFrames) {
				val deltaTime = readU_VL()
				val timeInMs = lastFrameTimeInMs + deltaTime
				lastFrameTimeInMs = timeInMs
				val actions = (0 until readU_VL()).map {
					val action = readU8()
					when (action) {
						0 -> {
							AnPlaySoundAction(readU_VL())
						}
						else -> TODO()
					}
				}
				ss.actions.add(timeInMs * 1000, AnActions(actions))
			}

			for (depth in 0 until totalDepths) {
				val timeline = ss.timelines[depth]
				var lastUid = -1
				var lastName: String? = null
				var lastColorTransform = ColorTransform()
				var lastMatrix = Matrix2d()
				var lastClipDepth = -1
				var lastRatio = 0.0
				var lastFrameTime = 0
				var lastBlendMode = BlendMode.INHERIT
				for (frameIndex in 0 until readU_VL()) {
					val deltaFrameTime = readU_VL()
					val frameTime = lastFrameTime + deltaFrameTime
					lastFrameTime = frameTime
					val flags = readU_VL()
					val hasUid = flags.extract(0)
					val hasName = flags.extract(1)
					val hasColorTransform = flags.extract(2)
					val hasMatrix = flags.extract(3)
					val hasClipDepth = flags.extract(4)
					val hasRatio = flags.extract(5)
					val hasAlpha = flags.extract(6)
					val hasBlendMode = flags.extract(7)

					if (hasUid) lastUid = readU_VL()
					if (hasClipDepth) lastClipDepth = readS16_le()
					if (hasName) lastName = strings[readU_VL()]
					if (hasAlpha) {
						val ct = lastColorTransform.copy()
						ct.mA = readU8().toDouble() / 255.0
						lastColorTransform = ct
					} else if (hasColorTransform) {
						val ct = lastColorTransform.copy()
						val ctFlags = readU8()
						if (ctFlags.extract(0)) ct.mR = readU8().toDouble() / 255.0
						if (ctFlags.extract(1)) ct.mG = readU8().toDouble() / 255.0
						if (ctFlags.extract(2)) ct.mB = readU8().toDouble() / 255.0
						if (ctFlags.extract(3)) ct.mA = readU8().toDouble() / 255.0
						if (ctFlags.extract(4)) ct.aR = readS8() * 2
						if (ctFlags.extract(5)) ct.aG = readS8() * 2
						if (ctFlags.extract(6)) ct.aB = readS8() * 2
						if (ctFlags.extract(7)) ct.aR = readS8() * 2
						//println(ct)
						lastColorTransform = ct
					}
					if (hasMatrix) {
						val lm = lastMatrix.copy()
						val matrixFlags = readU8()
						if (matrixFlags.extract(0)) lm.a = readS_VL().toDouble() / 16384.0
						if (matrixFlags.extract(1)) lm.b = readS_VL().toDouble() / 16384.0
						if (matrixFlags.extract(2)) lm.c = readS_VL().toDouble() / 16384.0
						if (matrixFlags.extract(3)) lm.d = readS_VL().toDouble() / 16384.0
						if (matrixFlags.extract(4)) lm.tx = readS_VL().toDouble() / 20.0
						if (matrixFlags.extract(5)) lm.ty = readS_VL().toDouble() / 20.0
						lastMatrix = lm
					}
					if (hasRatio) lastRatio = readU8().toDouble() / 255.0
					if (hasBlendMode) {
						lastBlendMode = BlendMode.BY_ORDINAL[readU8()] ?: BlendMode.INHERIT
					}
					timeline.add(frameTime * 1000, AnSymbolTimelineFrame(
						depth = depth,
						uid = lastUid,
						transform = lastMatrix,
						name = lastName,
						colorTransform = lastColorTransform,
						blendMode = lastBlendMode,
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
			name to AnSymbolMovieClipState(name, symbolStates[stateIndex], startTime = startTime)
		}.toMap()

		return mc
	}

	fun FastByteArrayInputStream.readRect() = Rectangle(x = readS_VL() / 20.0, y = readS_VL() / 20.0, width = readS_VL() / 20.0, height = readS_VL() / 20.0)
	fun FastByteArrayInputStream.readIRect() = RectangleInt(x = readS_VL(), y = readS_VL(), width = readS_VL(), height = readS_VL())
}
