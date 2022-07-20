package com.soywiz.korge.animate.serialization

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.clamp
import com.soywiz.kmem.insert
import com.soywiz.korge.animate.AnEventAction
import com.soywiz.korge.animate.AnLibrary
import com.soywiz.korge.animate.AnPlaySoundAction
import com.soywiz.korge.animate.AnSymbolBitmap
import com.soywiz.korge.animate.AnSymbolEmpty
import com.soywiz.korge.animate.AnSymbolMorphShape
import com.soywiz.korge.animate.AnSymbolMovieClip
import com.soywiz.korge.animate.AnSymbolShape
import com.soywiz.korge.animate.AnSymbolSound
import com.soywiz.korge.animate.AnTextFieldSymbol
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.ordinal
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.color.ColorTransform
import com.soywiz.korim.format.ImageEncodingProps
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.writeBitmap
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.stream.MemorySyncStreamToByteArray
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.write16LE
import com.soywiz.korio.stream.write8
import com.soywiz.korio.stream.writeF32LE
import com.soywiz.korio.stream.writeS_VL
import com.soywiz.korio.stream.writeStringVL
import com.soywiz.korio.stream.writeStringz
import com.soywiz.korio.stream.writeU_VL
import com.soywiz.korma.geom.IRectangleInt
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle

suspend fun AnLibrary.writeTo(file: VfsFile, config: AnLibrarySerializer.Config = AnLibrarySerializer.Config()) {
	//println("writeTo")
	val format = PNG
	val props = ImageEncodingProps(quality = config.compression)
	file.write(AnLibrarySerializer.gen(this, config = config, externalWriters = AnLibrarySerializer.ExternalWriters(
		writeAtlas = { index, atlas ->
			//showImageAndWait(atlas)
			file.withExtension("ani.$index.png").writeBitmap(atlas, format, props)
		},
		writeSound = { index, soundData ->
			file.withExtension("ani.$index.mp3").write(soundData)
		}
	)))
}

object AnLibrarySerializer {
	class ExternalWriters(
		val writeAtlas: suspend (index: Int, bitmap: Bitmap) -> Unit,
		val writeSound: suspend (index: Int, soundData: ByteArray) -> Unit
	)

	class Config(
		val compression: Double = 1.0,
		val keepPaths: Boolean = false,
		val mipmaps: Boolean = true,
		val smoothInterpolation: Boolean = true
	)

	suspend fun gen(library: AnLibrary, config: Config = Config(), externalWriters: ExternalWriters): ByteArray =
		MemorySyncStreamToByteArray { write(this, library, config, externalWriters) }

	suspend fun write(s: SyncStream, library: AnLibrary, config: Config = Config(), externalWriters: ExternalWriters) =
		s.writeLibrary(library, config, externalWriters)

	private fun SyncStream.writeRect(r: Rectangle) {
		writeS_VL((r.x * 20).toInt())
		writeS_VL((r.y * 20).toInt())
		writeS_VL((r.width * 20).toInt())
		writeS_VL((r.height * 20).toInt())
	}

	private fun SyncStream.writeIRect(r: IRectangleInt) {
		writeS_VL(r.x)
		writeS_VL(r.y)
		writeS_VL(r.width)
		writeS_VL(r.height)
	}

	suspend private fun SyncStream.writeLibrary(lib: AnLibrary, config: Config, externalWriters: ExternalWriters) {
		writeStringz(AniFile.MAGIC, 8)
		writeU_VL(AniFile.VERSION)
		writeU_VL(lib.msPerFrame)
		writeU_VL(lib.width)
		writeU_VL(lib.height)
		writeU_VL(
			0
				.insert(config.mipmaps, 0)
				.insert(!config.smoothInterpolation, 1)
		)
		// Allocate Strings
		val strings = OptimizedStringAllocator()
		lib.symbolsById.fastForEach { symbol ->
			strings.add(symbol.name)
			when (symbol) {
				is AnSymbolMovieClip -> {
					for (ss in symbol.states) {
						strings.add(ss.key)
						//strings.add(ss.value.state.name)
						strings.add(ss.value.subTimeline.nextState)
						for (timeline in ss.value.subTimeline.timelines) {
							for (entry in timeline.entries) {
								strings.add(entry.second.name)
							}
						}
						for (action in ss.value.subTimeline.actions.objects) {
							when (action) {
								is AnEventAction -> {
									strings.add(action.event)
								}
							}
						}
					}
				}
				is AnTextFieldSymbol -> {
					strings.add(symbol.initialHtml)
				}
			}
		}
		strings.finalize()

		// String pool
		writeU_VL(strings.strings.size)
		for (str in strings.strings.drop(1)) writeStringVL(str!!)

		// Atlases
		val atlasBitmaps = listOf(
			lib.symbolsById.filterIsInstance<AnSymbolShape>().map { it.textureWithBitmap?.bitmapSlice?.bmpBase },
			lib.symbolsById.filterIsInstance<AnSymbolMorphShape>().flatMap { it.texturesWithBitmap.entries.map { it.second.bitmapSlice.bmpBase } }
		).flatMap { it }.filterNotNull().distinct()

		val atlasBitmapsToId = atlasBitmaps.withIndex().map { it.value to it.index }.toMap()

		writeU_VL(atlasBitmaps.size)
		for ((atlas, index) in atlasBitmapsToId) {
			externalWriters.writeAtlas(index, atlas)
		}

		val soundsToId =
			lib.symbolsById.filterIsInstance<AnSymbolSound>().withIndex().map { it.value to it.index }.toMap()

		writeU_VL(soundsToId.size)
		for ((sound, index) in soundsToId) {
			externalWriters.writeSound(index, sound.dataBytes ?: byteArrayOf())
		}

		// Symbols
		var morphShapeCount = 0
		var shapeCount = 0
		var movieClipCount = 0
		var totalFrameCount = 0
		var totalTimelines = 0

		writeU_VL(lib.symbolsById.size)
		for (symbol in lib.symbolsById) {
			writeU_VL(symbol.id)
			writeU_VL(strings[symbol.name])
			when (symbol) {
				is AnSymbolEmpty -> {
					writeU_VL(AniFile.SYMBOL_TYPE_EMPTY)
				}
				is AnSymbolSound -> {
					writeU_VL(AniFile.SYMBOL_TYPE_SOUND)
					writeU_VL(soundsToId[symbol]!!)
				}
				is AnTextFieldSymbol -> {
					writeU_VL(AniFile.SYMBOL_TYPE_TEXT)
					writeU_VL(strings[symbol.initialHtml])
					writeRect(symbol.bounds)
				}
				is AnSymbolShape -> {
					shapeCount++
					writeU_VL(AniFile.SYMBOL_TYPE_SHAPE)
					writeF32LE(symbol.textureWithBitmap!!.scale.toFloat())
					writeU_VL(atlasBitmapsToId[symbol.textureWithBitmap!!.bitmapSlice.bmpBase]!!)
					writeIRect(symbol.textureWithBitmap!!.bitmapSlice.bounds)
					writeRect(symbol.bounds)
					val path = symbol.path
					if (config.keepPaths && path != null) {
						writeU_VL(1)
						writeU_VL(path.commands.size)
						for (cmd in path.commands) write8(cmd)
						writeU_VL(path.data.size)
						for (v in path.data) writeF32LE(v.toFloat())
					} else {
						writeU_VL(0)
					}
				}
				is AnSymbolMorphShape -> {
					morphShapeCount++
					writeU_VL(AniFile.SYMBOL_TYPE_MORPH_SHAPE)
					val entries = symbol.texturesWithBitmap.entries
					writeU_VL(entries.size)
					for ((ratio1000, textureWithBitmap) in entries) {
						writeU_VL(ratio1000)
						writeF32LE(textureWithBitmap.scale.toFloat())
						writeU_VL(atlasBitmapsToId[textureWithBitmap.bitmapSlice.bmpBase]!!)
						writeRect(textureWithBitmap.bounds)
						writeIRect(textureWithBitmap.bitmapSlice.bounds)
					}
				}
				is AnSymbolBitmap -> {
					writeU_VL(AniFile.SYMBOL_TYPE_BITMAP)
				}
				is AnSymbolMovieClip -> {
					movieClipCount++
					// val totalDepths: Int, val totalFrames: Int, val totalUids: Int, val totalTime: Int
					writeU_VL(AniFile.SYMBOL_TYPE_MOVIE_CLIP)

					val hasNinePatchRect = (symbol.ninePatch != null)

					write8(
						0
							.insert(hasNinePatchRect, 0)
					)

					val limits = symbol.limits
					writeU_VL(limits.totalDepths)
					writeU_VL(limits.totalFrames)
					writeU_VL(limits.totalTime.millisecondsInt)

					// uids
					writeU_VL(limits.totalUids)
					for (uidInfo in symbol.uidInfo) {
						writeU_VL(uidInfo.characterId)
						writeStringVL(if (uidInfo.extraProps.isNotEmpty()) Json.stringify(uidInfo.extraProps) else "")
					}

					val symbolStates = symbol.states.map { it.value.subTimeline }.toList().distinct()
					val symbolStateToIndex = symbolStates.withIndex().map { it.value to it.index }.toMap()

					if (hasNinePatchRect) {
						writeRect(symbol.ninePatch!!)
					}

					// states
					writeU_VL(symbolStates.size)
					for (ss in symbolStates) {
						//writeU_VL(strings[ss.name])
						writeU_VL(ss.totalTime.microseconds.toInt())
						write8(0.insert(ss.nextStatePlay, 0))
						writeU_VL(strings[ss.nextState])

						var lastFrameTimeMs = 0
						//val actionsPerTime = ss.actions.entries.filter { it.second !is AnEventAction }.groupBy { it.first }
						val actionsPerTime = ss.actions.entries.groupBy { it.first }
						writeU_VL(actionsPerTime.size)
						for ((timeInMicro, actions) in actionsPerTime) {
							val timeInMs = timeInMicro / 1000
							writeU_VL(timeInMs - lastFrameTimeMs) // @TODO: Use time deltas and/or frame indices
							lastFrameTimeMs = timeInMs
							writeU_VL(actions.size)
							for (actionInfo in actions) {
								val action = actionInfo.second
								when (action) {
									is AnPlaySoundAction -> {
										write8(0)
										writeU_VL(action.soundId)
									}
									is AnEventAction -> {
										write8(1)
										writeU_VL(strings[action.event])
									}
									else -> TODO()
								}
							}
						}

						for (timeline in ss.timelines) {
							totalTimelines++
							val frames = timeline.entries
							var lastUid = -1
							var lastName: String? = null
							var lastColorTransform: ColorTransform = ColorTransform()
							var lastMatrix: Matrix = Matrix()
							var lastClipDepth = -1
							var lastRatio = 0.0
							var lastBlendMode = BlendMode.INHERIT
							writeU_VL(frames.size)
							var lastFrameTime = 0
							for ((frameTime, frame) in frames) {
								val storeFrameTime = frameTime / 1000
								totalFrameCount++
								//println(frameTime)
								writeU_VL(storeFrameTime - lastFrameTime) // @TODO: Use time deltas and/or frame indices
								lastFrameTime = storeFrameTime

								val ct = frame.colorTransform
								val m = frame.transform
								val hasUid = frame.uid != lastUid
								val hasName = frame.name != lastName
								val hasColorTransform = ct != lastColorTransform
								val hasBlendMode = frame.blendMode != lastBlendMode
								val hasAlpha = (
										(ct.mR == lastColorTransform.mR) &&
												(ct.mG == lastColorTransform.mG) &&
												(ct.mB == lastColorTransform.mB) &&
												(ct.mA != lastColorTransform.mA) &&
												(ct.aR == lastColorTransform.aR) &&
												(ct.aG == lastColorTransform.aG) &&
												(ct.aB == lastColorTransform.aB) &&
												(ct.aA == lastColorTransform.aA)
										)


								val hasClipDepth = frame.clipDepth != lastClipDepth
								val hasRatio = frame.ratio != lastRatio

								val hasMatrix = m != lastMatrix

								write8(
									0
										.insert(hasUid, 0)
										.insert(hasName, 1)
										.insert(hasColorTransform, 2)
										.insert(hasMatrix, 3)
										.insert(hasClipDepth, 4)
										.insert(hasRatio, 5)
										.insert(hasAlpha, 6)
										.insert(hasBlendMode, 7)
								)
								if (hasUid) writeU_VL(frame.uid)
								if (hasClipDepth) write16LE(frame.clipDepth)
								if (hasName) writeU_VL(strings[frame.name])

								if (hasAlpha) {
									write8((ct.mA * 255.0).toInt().clamp(0x00, 0xFF))
								} else if (hasColorTransform) {
									val hasMR = ct.mR != lastColorTransform.mR
									val hasMG = ct.mG != lastColorTransform.mG
									val hasMB = ct.mB != lastColorTransform.mB
									val hasMA = ct.mA != lastColorTransform.mA

									val hasAR = ct.aR != lastColorTransform.aR
									val hasAG = ct.aG != lastColorTransform.aG
									val hasAB = ct.aB != lastColorTransform.aB
									val hasAA = ct.aA != lastColorTransform.aA

									write8(
										0
											.insert(hasMR, 0)
											.insert(hasMG, 1)
											.insert(hasMB, 2)
											.insert(hasMA, 3)
											.insert(hasAR, 4)
											.insert(hasAG, 5)
											.insert(hasAB, 6)
											.insert(hasAA, 7)
									)

									if (hasMR) write8((ct.mR.clamp(0.0, 1.0) * 255.0).toInt())
									if (hasMG) write8((ct.mG.clamp(0.0, 1.0) * 255.0).toInt())
									if (hasMB) write8((ct.mB.clamp(0.0, 1.0) * 255.0).toInt())
									if (hasMA) write8((ct.mA.clamp(0.0, 1.0) * 255.0).toInt())
									if (hasAR) write8(ct.aR.clamp(-255, +255) / 2)
									if (hasAG) write8(ct.aG.clamp(-255, +255) / 2)
									if (hasAB) write8(ct.aB.clamp(-255, +255) / 2)
									if (hasAA) write8(ct.aA.clamp(-255, +255) / 2)
								}

								if (hasMatrix) {
									val hasMatrixA = m.a != lastMatrix.a
									val hasMatrixB = m.b != lastMatrix.b
									val hasMatrixC = m.c != lastMatrix.c
									val hasMatrixD = m.d != lastMatrix.d
									val hasMatrixTX = m.tx != lastMatrix.tx
									val hasMatrixTY = m.ty != lastMatrix.ty

									write8(
										0
											.insert(hasMatrixA, 0)
											.insert(hasMatrixB, 1)
											.insert(hasMatrixC, 2)
											.insert(hasMatrixD, 3)
											.insert(hasMatrixTX, 4)
											.insert(hasMatrixTY, 5)
									)

									if (hasMatrixA) writeS_VL((m.a * 16384).toInt())
									if (hasMatrixB) writeS_VL((m.b * 16384).toInt())
									if (hasMatrixC) writeS_VL((m.c * 16384).toInt())
									if (hasMatrixD) writeS_VL((m.d * 16384).toInt())
									if (hasMatrixTX) writeS_VL((m.tx * 20).toInt())
									if (hasMatrixTY) writeS_VL((m.ty * 20).toInt())
								}
								if (hasRatio) write8((frame.ratio * 255).toInt().clamp(0, 255))

								if (hasBlendMode) {
									write8(frame.blendMode.ordinal)
								}

								lastUid = frame.uid
								lastName = frame.name
								lastColorTransform = frame.colorTransform
								lastMatrix = m
								lastClipDepth = frame.clipDepth
								lastRatio = frame.ratio
								lastBlendMode = frame.blendMode
							}
						}
					}

					// namedStates
					writeU_VL(symbol.states.size)
					for ((name, ssi) in symbol.states) {
						val stateIndex = symbolStateToIndex[ssi.subTimeline] ?: 0
						writeU_VL(strings[name])
						writeU_VL(ssi.startTime.millisecondsInt)
						writeU_VL(stateIndex)
					}
				}
			}
		}

		if (true) {
			//println("totalTimelines: $totalTimelines")
			//println("totalFrameCount: $totalFrameCount")
			//println("shapeCount: $shapeCount")
			//println("morphShapeCount: $morphShapeCount")
			//println("movieClipCount: $movieClipCount")
		}

		// End of symbols
	}
}
