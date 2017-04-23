package com.soywiz.korge.animate.serialization

import com.soywiz.korge.animate.*
import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.texture
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.slice
import com.soywiz.korim.format.ImageFormats
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.stream.*
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.ds.DoubleArrayList
import com.soywiz.korma.ds.IntArrayList
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.VectorPath

object AnLibraryDeserializer {
	fun read(s: ByteArray, views: Views): AnLibrary = s.openSync().readLibrary(views)
	fun read(s: SyncStream, views: Views): AnLibrary = s.readLibrary(views)

	private fun SyncStream.readLibrary(views: Views): AnLibrary {
		//AnLibrary(views)
		val magic = readStringz(8)
		if (magic != AnLibraryFile.MAGIC) invalidOp("Not a ${AnLibraryFile.MAGIC} file")
		if (readU_VL() > AnLibraryFile.VERSION) invalidOp("Just supported ${AnLibraryFile.MAGIC} version ${AnLibraryFile.VERSION} or lower")
		val msPerFrame = readU_VL()
		val library = AnLibrary(views, 1000.0 / msPerFrame)

		val strings = arrayOf<String?>(null) + (1 until readU_VL()).map { readStringVL() }

		val atlases = (0 until readU_VL()).map {
			val format = readU_VL()
			val width = readU_VL()
			val height = readU_VL()
			val size = readU_VL()
			val data = readBytes(size)
			val bmp = ImageFormats.read(data)
			bmp to views.texture(bmp)
		}

		val sounds = (0 until readU_VL()).map {
			Unit
		}

		val fonts = (0 until readU_VL()).map {
			Unit
		}

		val symbols = (0 until readU_VL()).map {
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
							val cmds = (0 until readU_VL()).map { readU8() }.toIntArray()
							val data = (0 until readU_VL()).map { readF32_le().toDouble() }.toDoubleArray()
							VectorPath(IntArrayList(cmds), DoubleArrayList(data))
						}
						else -> null
					}
					AnSymbolShape(symbolId, symbolName, bounds, textureWithBitmap = TextureWithBitmapSlice(texture.slice(textureBounds.toDouble()), bitmap.slice(textureBounds), scale = scale), path = path)
				}
				AnLibraryFile.SYMBOL_TYPE_BITMAP -> {
					AnSymbolBitmap(symbolId, symbolName, Bitmap32(1, 1))
				}
				AnLibraryFile.SYMBOL_TYPE_MOVIE_CLIP -> {
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
							var lastAlpha: Double = 1.0
							var lastMatrix: Matrix2d.Computed = Matrix2d.Computed(Matrix2d())
							for (frameIndex in 0 until readU_VL()) {
								val frameTime = readU_VL()
								val flags = readU_VL()
								val hasUid = (flags and 1) != 0
								val hasName = (flags and 2) != 0
								val hasAlpha = (flags and 4) != 0
								val hasMatrix = (flags and 8) != 0

								if (hasUid) lastUid = readU_VL()
								if (hasName) lastName = strings[readU_VL()]
								if (hasAlpha) lastAlpha = readU8().toDouble() / 255.0
								if (hasMatrix) lastMatrix = Matrix2d.Computed(Matrix2d(a = readF32_le(), b = readF32_le(), c = readF32_le(), d = readF32_le(), tx = readF32_le(), ty = readF32_le()))
								timeline.add(frameTime, AnSymbolTimelineFrame(lastUid, lastMatrix, lastName, lastAlpha, BlendMode.INHERIT))
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

					mc
				}
				else -> TODO("Type: $type")
			}
			symbol
		}

		for (symbol in symbols) library.addSymbol(symbol)
		library.processSymbolNames()

		return library
	}

	fun SyncStream.readRect() = Rectangle(x = readF32_le(), y = readF32_le(), width = readF32_le(), height = readF32_le())
	fun SyncStream.readIRect() = RectangleInt(x = readF32_le().toInt(), y = readF32_le().toInt(), width = readF32_le().toInt(), height = readF32_le().toInt())
}
