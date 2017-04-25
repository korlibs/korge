package com.soywiz.korge.animate.serialization

import com.soywiz.korge.animate.*
import com.soywiz.korge.view.ColorTransform
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.ImageEncodingProps
import com.soywiz.korim.format.PNG
import com.soywiz.korim.format.writeBitmap
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.clamp
import com.soywiz.korio.util.insert
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.IRectangleInt
import com.soywiz.korma.geom.Rectangle

suspend fun AnLibrary.writeTo(file: VfsFile, compression: Double = 1.0) {
	//println("writeTo")
	val format = PNG()
	val props = ImageEncodingProps(compression)
	file.write(AnLibrarySerializer.gen(this, compression) { index, atlas ->
		//showImageAndWait(atlas)
		file.withExtension("ani.$index.png").writeBitmap(atlas, format, props)
	})
}

object AnLibrarySerializer {
	suspend fun gen(library: AnLibrary, compression: Double = 1.0, atlasWriter: suspend (index: Int, bitmap: Bitmap) -> Unit): ByteArray = MemorySyncStreamToByteArray { write(this, library, compression, atlasWriter) }

	suspend fun write(s: SyncStream, library: AnLibrary, compression: Double = 1.0, atlasWriter: suspend (index: Int, bitmap: Bitmap) -> Unit) = s.writeLibrary(library, compression, atlasWriter)

	private fun SyncStream.writeRect(r: Rectangle) {
		writeF32_le(r.x.toFloat())
		writeF32_le(r.y.toFloat())
		writeF32_le(r.width.toFloat())
		writeF32_le(r.height.toFloat())
	}

	private fun SyncStream.writeIRect(r: IRectangleInt) {
		writeF32_le(r.x.toFloat())
		writeF32_le(r.y.toFloat())
		writeF32_le(r.width.toFloat())
		writeF32_le(r.height.toFloat())
	}

	suspend private fun SyncStream.writeLibrary(lib: AnLibrary, compression: Double = 1.0, atlasWriter: suspend (index: Int, bitmap: Bitmap) -> Unit) {
		writeStringz(AnLibraryFile.MAGIC, 8)
		writeU_VL(AnLibraryFile.VERSION)
		writeU_VL(lib.msPerFrame)

		// Allocate Strings
		val strings = OptimizedStringAllocator()
		for (symbol in lib.symbolsById) {
			strings.add(symbol.name)
			when (symbol) {
				is AnSymbolMovieClip -> {
					for (ss in symbol.states) {
						strings.add(ss.key)
						//strings.add(ss.value.state.name)
						for (timeline in ss.value.state.timelines) {
							for (entry in timeline.entries) {
								strings.add(entry.second.name)
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
			lib.symbolsById.filterIsInstance<AnSymbolShape>().map { it.textureWithBitmap?.bitmapSlice?.bmp },
			lib.symbolsById.filterIsInstance<AnSymbolMorphShape>().flatMap { it.texturesWithBitmap.entries.map { it.second.bitmapSlice.bmp } }
		).flatMap { it }.filterNotNull().distinct()
		val atlasBitmapsToId = atlasBitmaps.withIndex().map { it.value to it.index }.toMap()

		writeU_VL(atlasBitmaps.size)
		for ((index, atlas) in atlasBitmaps.withIndex()) {
			atlasWriter(index, atlas)
			//val atlasBytes = ImageFormats.encode(atlas, "atlas.png", props = ImageEncodingProps(quality = compression))
			//writeU_VL(1) // 1=RGBA, 2=RGB(JPG)+ALPHA(ZLIB)
			//writeU_VL(atlas.width)
			//writeU_VL(atlas.height)
			//writeU_VL(atlasBytes.size)
			//writeBytes(atlasBytes)
		}

		// Sounds
		writeU_VL(0)

		// Fonts
		writeU_VL(0)

		// Symbols
		writeU_VL(lib.symbolsById.size)
		for (symbol in lib.symbolsById) {
			writeU_VL(symbol.id)
			writeU_VL(strings[symbol.name])
			when (symbol) {
				is AnSymbolEmpty -> {
					writeU_VL(AnLibraryFile.SYMBOL_TYPE_EMPTY)
				}
				is AnSymbolSound -> {
					writeU_VL(AnLibraryFile.SYMBOL_TYPE_SOUND)
				}
				is AnTextFieldSymbol -> {
					writeU_VL(AnLibraryFile.SYMBOL_TYPE_TEXT)
					writeU_VL(strings[symbol.initialHtml])
					writeRect(symbol.bounds)
				}
				is AnSymbolShape -> {
					writeU_VL(AnLibraryFile.SYMBOL_TYPE_SHAPE)
					writeF32_le(symbol.textureWithBitmap!!.scale.toFloat())
					writeU_VL(atlasBitmapsToId[symbol.textureWithBitmap!!.bitmapSlice.bmp]!!)
					writeIRect(symbol.textureWithBitmap!!.bitmapSlice.bounds)
					writeRect(symbol.bounds)
					val path = symbol.path
					if (path != null) {
						writeU_VL(1)
						writeU_VL(path.commands.size)
						for (cmd in path.commands) write8(cmd)
						writeU_VL(path.data.size)
						for (v in path.data) writeF32_le(v.toFloat())
					} else {
						writeU_VL(0)
					}
				}
				is AnSymbolMorphShape -> {
					writeU_VL(AnLibraryFile.SYMBOL_TYPE_MORPH_SHAPE)
					val entries = symbol.texturesWithBitmap.entries
					writeU_VL(entries.size)
					for ((ratio1000, textureWithBitmap) in entries) {
						writeU_VL(ratio1000)
						writeF32_le(textureWithBitmap.scale.toFloat())
						writeU_VL(atlasBitmapsToId[textureWithBitmap.bitmapSlice.bmp]!!)
						writeRect(textureWithBitmap.bounds)
						writeIRect(textureWithBitmap.bitmapSlice.bounds)
					}
				}
				is AnSymbolBitmap -> {
					writeU_VL(AnLibraryFile.SYMBOL_TYPE_BITMAP)
				}
				is AnSymbolMovieClip -> {
					// val totalDepths: Int, val totalFrames: Int, val totalUids: Int, val totalTime: Int
					writeU_VL(AnLibraryFile.SYMBOL_TYPE_MOVIE_CLIP)

					val limits = symbol.limits
					writeU_VL(limits.totalDepths)
					writeU_VL(limits.totalFrames)
					writeU_VL(limits.totalTime)

					// uids
					writeU_VL(limits.totalUids)
					for (uidInfo in symbol.uidInfo) {
						writeU_VL(uidInfo.characterId)
						writeStringVL(if (uidInfo.extraProps.isNotEmpty()) Json.encode(uidInfo.extraProps) else "")
					}

					val symbolStates = symbol.states.map { it.value.state }.toList().distinct()
					val symbolStateToIndex = symbolStates.withIndex().map { it.value to it.index }.toMap()

					// states
					writeU_VL(symbolStates.size)
					for (ss in symbolStates) {
						//writeU_VL(strings[ss.name])
						writeU_VL(ss.totalTime)
						writeU_VL(ss.loopStartTime)
						for (timeline in ss.timelines) {
							val frames = timeline.entries
							var lastUid = -1
							var lastName: String? = null
							var lastColorTransform: ColorTransform = ColorTransform()
							var lastMatrix: Matrix2d = Matrix2d()
							var lastClipDepth = -1
							var lastRatio = 0.0
							writeU_VL(frames.size)
							for ((frameTime, frame) in frames) {
								writeU_VL(frameTime)

								val ct = frame.colorTransform
								val m = frame.transform
								val hasUid = frame.uid != lastUid
								val hasName = frame.name != lastName
								val hasColorTransform = frame.colorTransform != lastColorTransform
								val hasClipDepth = frame.clipDepth != lastClipDepth
								val hasRatio = frame.ratio != lastRatio

								val hasMatrix = m != lastMatrix

								write8(0
									.insert(hasUid, 0)
									.insert(hasName, 1)
									.insert(hasColorTransform, 2)
									.insert(hasMatrix, 3)
									.insert(hasClipDepth, 4)
									.insert(hasRatio, 5)
								)
								if (hasUid) writeU_VL(frame.uid)
								if (hasClipDepth) write16_le(frame.clipDepth)
								if (hasName) writeU_VL(strings[frame.name])
								if (hasColorTransform) {
									val hasMR = ct.mR != lastColorTransform.mR
									val hasMG = ct.mG != lastColorTransform.mG
									val hasMB = ct.mB != lastColorTransform.mB
									val hasMA = ct.mA != lastColorTransform.mA

									val hasAR = ct.aR != lastColorTransform.aR
									val hasAG = ct.aG != lastColorTransform.aG
									val hasAB = ct.aB != lastColorTransform.aB
									val hasAA = ct.aA != lastColorTransform.aA

									write8(0
										.insert(hasMR, 0)
										.insert(hasMG, 1)
										.insert(hasMB, 2)
										.insert(hasMA, 3)
										.insert(hasAR, 4)
										.insert(hasAG, 5)
										.insert(hasAB, 6)
										.insert(hasAA, 7)
									)

									if (hasMR) writeF32_le(ct.mR.toFloat())
									if (hasMG) writeF32_le(ct.mG.toFloat())
									if (hasMB) writeF32_le(ct.mB.toFloat())
									if (hasMA) writeF32_le(ct.mA.toFloat())

									if (hasAR) write16_le(ct.aR)
									if (hasAG) write16_le(ct.aG)
									if (hasAB) write16_le(ct.aB)
									if (hasAA) write16_le(ct.aA)
								}
								if (hasMatrix) {
									val hasMatrixA = m.a != lastMatrix.a
									val hasMatrixB = m.b != lastMatrix.b
									val hasMatrixC = m.c != lastMatrix.c
									val hasMatrixD = m.d != lastMatrix.d
									val hasMatrixTX = m.tx != lastMatrix.tx
									val hasMatrixTY = m.ty != lastMatrix.ty

									write8(0
										.insert(hasMatrixA, 0)
										.insert(hasMatrixB, 1)
										.insert(hasMatrixC, 2)
										.insert(hasMatrixD, 3)
										.insert(hasMatrixTX, 4)
										.insert(hasMatrixTY, 5))

									if (hasMatrixA) writeF32_le(m.a.toFloat())
									if (hasMatrixB) writeF32_le(m.b.toFloat())
									if (hasMatrixC) writeF32_le(m.c.toFloat())
									if (hasMatrixD) writeF32_le(m.d.toFloat())
									if (hasMatrixTX) writeF32_le(m.tx.toFloat())
									if (hasMatrixTY) writeF32_le(m.ty.toFloat())
								}
								if (hasRatio) writeF32_le(frame.ratio.toFloat())

								lastUid = frame.uid
								lastName = frame.name
								lastColorTransform = frame.colorTransform
								lastMatrix = m
								lastClipDepth = frame.clipDepth
								lastRatio = frame.ratio
							}
						}
					}

					// namedStates
					writeU_VL(symbol.states.size)
					for ((name, ssi) in symbol.states) {
						val stateIndex = symbolStateToIndex[ssi.state] ?: 0
						writeU_VL(strings[name])
						writeU_VL(ssi.startTime)
						writeU_VL(stateIndex)
					}
				}
			}
		}

		// End of symbols
	}
}
