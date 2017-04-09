package com.soywiz.korge.animate.serialization

import com.soywiz.korge.animate.*
import com.soywiz.korim.format.ImageEncodingProps
import com.soywiz.korim.format.ImageFormats
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.clamp
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.IRectangle
import com.soywiz.korma.geom.Rectangle

object AnimateSerializer {
	fun gen(library: AnLibrary): ByteArray = MemorySyncStreamToByteArray { write(this, library) }

	fun write(s: SyncStream, library: AnLibrary) = s.writeLibrary(library)

	private fun SyncStream.writeRect(r: Rectangle) {
		writeF32_le(r.x.toFloat())
		writeF32_le(r.y.toFloat())
		writeF32_le(r.width.toFloat())
		writeF32_le(r.height.toFloat())
	}

	private fun SyncStream.writeIRect(r: IRectangle) {
		writeF32_le(r.x.toFloat())
		writeF32_le(r.y.toFloat())
		writeF32_le(r.width.toFloat())
		writeF32_le(r.height.toFloat())
	}

	private fun SyncStream.writeLibrary(lib: AnLibrary) {
		writeStringz(AnimateFile.MAGIC, 8)
		writeU_VL(AnimateFile.VERSION)
		writeU_VL(lib.msPerFrame)

		// Allocate Strings
		val strings = OptimizedStringAllocator()
		for (symbol in lib.symbolsById) {
			strings.add(symbol.name ?: "")
			when (symbol) {
				is AnSymbolMovieClip -> {
					for (ss in symbol.states) {
						strings.add(ss.key)
						strings.add(ss.value.state.name)
						for (timeline in ss.value.state.timelines) {
							for (entry in timeline.entries) {
								strings.add(entry.second.name ?: "")
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
		for (str in strings.strings) {
			writeStringVL(str)
		}

		// Atlases
		val atlasBitmaps = lib.symbolsById.filterIsInstance<AnSymbolShape>().map { it.textureWithBitmap?.bitmapSlice?.bmp }.filterNotNull().distinct()
		val atlasBitmapsToId = atlasBitmaps.withIndex().map { it.value to it.index }.toMap()

		writeU_VL(atlasBitmaps.size)
		for (atlas in atlasBitmaps) {
			val atlasBytes = ImageFormats.encode(atlas, "atlas.png", props = ImageEncodingProps(quality = 1.0))
			writeU_VL(1) // 1=RGBA, 2=RGB(JPG)+ALPHA(ZLIB)
			writeU_VL(atlas.width)
			writeU_VL(atlas.height)
			writeU_VL(atlasBytes.size)
			writeBytes(atlasBytes)
		}

		// Sounds
		writeU_VL(0)

		// Fonts
		writeU_VL(0)

		// Symbols
		writeU_VL(lib.symbolsById.size)
		for (symbol in lib.symbolsById) {
			writeU_VL(symbol.id)
			writeU_VL(strings[symbol.name ?: ""])
			when (symbol) {
				is AnSymbolEmpty -> {
					writeU_VL(AnimateFile.SYMBOL_TYPE_EMPTY)
				}
				is AnSymbolSound -> {
					writeU_VL(AnimateFile.SYMBOL_TYPE_SOUND)
				}
				is AnTextFieldSymbol -> {
					writeU_VL(AnimateFile.SYMBOL_TYPE_TEXT)
					writeU_VL(strings[symbol.initialHtml])
					writeRect(symbol.bounds)
				}
				is AnSymbolShape -> {
					writeU_VL(AnimateFile.SYMBOL_TYPE_SHAPE)
					writeU_VL(atlasBitmapsToId[symbol.textureWithBitmap!!.bitmapSlice.bmp]!!)
					writeIRect(symbol.textureWithBitmap!!.bitmapSlice.bounds)
					writeRect(symbol.bounds)
				}
				is AnSymbolBitmap -> {
					writeU_VL(AnimateFile.SYMBOL_TYPE_BITMAP)
				}
				is AnSymbolMovieClip -> {
					// val totalDepths: Int, val totalFrames: Int, val totalUids: Int, val totalTime: Int
					writeU_VL(AnimateFile.SYMBOL_TYPE_MOVIE_CLIP)

					val limits = symbol.limits
					writeU_VL(limits.totalDepths)
					writeU_VL(limits.totalFrames)
					writeU_VL(limits.totalTime)

					// uids
					writeU_VL(limits.totalUids)
					for (uidInfo in symbol.uidInfo) writeU_VL(uidInfo.characterId)

					val symbolStates = symbol.states.map { it.value.state }.toList().distinct()
					val symbolStateToIndex = symbolStates.withIndex().map { it.value to it.index }.toMap()

					// states
					writeU_VL(symbolStates.size)
					for (ss in symbolStates) {
						writeU_VL(strings[ss.name])
						writeU_VL(ss.totalTime)
						writeU_VL(ss.loopStartTime)
						for (timeline in ss.timelines) {
							val frames = timeline.entries
							writeU_VL(frames.size)
							var lastUid = -1
							var lastName: String? = null
							var lastAlpha: Double = 1.0
							var lastMatrix: Matrix2d = Matrix2d()
							for ((frameTime, frame) in frames) {
								writeU_VL(frameTime)

								val hasUid = frame.uid != lastUid
								val hasName = frame.name != lastName
								val hasAlpha = frame.alpha != lastAlpha
								val hasMatrix = frame.transform.matrix != lastMatrix

								var flags = 0
								if (hasUid) flags = flags or 1
								if (hasName) flags = flags or 2
								if (hasAlpha) flags = flags or 4
								if (hasMatrix) flags = flags or 8

								writeU_VL(flags)
								if (hasUid) writeU_VL(frame.uid)
								if (hasName) writeU_VL(strings[frame.name ?: ""])
								if (hasAlpha) write8((frame.alpha.clamp(0.0, 1.0) * 255.0).toInt())
								// @TODO: Compact
								if (hasMatrix) {
									val m = frame.transform.matrix
									writeF32_le(m.a.toFloat())
									writeF32_le(m.b.toFloat())
									writeF32_le(m.c.toFloat())
									writeF32_le(m.d.toFloat())
									writeF32_le(m.tx.toFloat())
									writeF32_le(m.ty.toFloat())
								}

								lastUid = frame.uid
								lastName = frame.name
								lastAlpha = frame.alpha
								lastMatrix = frame.transform.matrix
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
	}
}
