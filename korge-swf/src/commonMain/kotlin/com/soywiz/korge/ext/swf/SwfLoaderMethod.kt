package com.soywiz.korge.ext.swf

import com.codeazur.as3swf.*
import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korfl.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.BlendMode
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.error.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.*

data class MinMaxDouble(
	var count: Int = 0,
	var min: Double = 0.0,
	var max: Double = 0.0
) {
	val isEmpty: Boolean get() = count == 0
	val isNotEmpty: Boolean get() = count > 0

	fun register(value: Double) {
		if (isEmpty) {
			min = value
			max = value
		} else {
			min = min(min, value)
			max = max(max, value)
		}
		count++
	}

	fun register(value: MinMaxDouble) {
		if (value.isNotEmpty) {
			register(value.min)
			register(value.max)
		}
	}
}

class SymbolAnalyzeInfo(val characterId: Int) {
	var hasNinePatch = false
	val parents = LinkedHashSet<SymbolAnalyzeInfo>()
	val scaleBounds = MinMaxDouble()

	val globalScaleBounds: MinMaxDouble by lazy {
		val out = MinMaxDouble()
		if (parents.isEmpty()) {
			if (scaleBounds.isNotEmpty) {
				out.register(scaleBounds)
			} else {
				out.register(1.0)
			}
		} else {
			for (parent in parents) {
				if (parent.hasNinePatch) continue // Do not count ninePatches
				out.register(scaleBounds.min * parent.globalScaleBounds.min)
				out.register(scaleBounds.max * parent.globalScaleBounds.max)
			}
		}
		out
	}

	fun registerParent(characterId: SymbolAnalyzeInfo) {
		parents += characterId
	}

	fun registerScale(scaleX: Double, scaleY: Double) {
		scaleBounds.register(max(scaleX, scaleY))
	}

	fun registerMatrix(matrix: Matrix) {
		registerScale(abs(matrix.a), abs(matrix.d))
	}
}

class SWFShapeRasterizerRequest(
	val swf: SWF,
	val charId: Int,
	val shapeBounds: Rectangle,
	val export: (ShapeExporter) -> Unit,
	val config: SWFExportConfig
) {
	val path = GraphicsPath()
	fun getRasterizer(maxScale: Double): SWFShapeRasterizer {
		val adaptiveScaling = if (config.adaptiveScaling) maxScale else 1.0
		//val maxScale = if (maxScale != 0.0) 1.0 / maxScale else 1.0
		//println("SWFShapeRasterizerRequest: $charId: $adaptiveScaling : $config")
		return SWFShapeRasterizer(
			swf,
			config.debug,
			shapeBounds,
			export,
			rasterizerMethod = config.rasterizerMethod,
			antialiasing = config.antialiasing,
			//requestScale = config.exportScale / maxScale.clamp(0.0001, 4.0),
			requestScale = config.exportScale * adaptiveScaling,
			minSide = config.minShapeSide,
			maxSide = config.maxShapeSide,
			path = path
		)
	}
}

class SwfLoaderMethod(val views: Views, val config: SWFExportConfig) {
	lateinit var swf: SWF
	lateinit var lib: AnLibrary
	val classNameToTypes = hashMapOf<String, ABC.TypeInfo>()
	val classNameToTagId = hashMapOf<String, Int>()
	val shapesToPopulate = LinkedHashMap<AnSymbolShape, SWFShapeRasterizerRequest>()
	val morphShapesToPopulate = arrayListOf<AnSymbolMorphShape>()
	val morphShapeRatios = hashMapOf<Int, HashSet<Double>>()

	private val analyzerInfos = hashMapOf<Int, SymbolAnalyzeInfo>()

	fun analyzerInfo(id: Int): SymbolAnalyzeInfo {
		return analyzerInfos.getOrPut(id) { SymbolAnalyzeInfo(id) }
	}

	suspend fun load(data: ByteArray): AnLibrary {
		swf = SWF().loadBytes(data)
		val bounds = swf.frameSize.rect
		lib = AnLibrary(views, bounds.width.toInt(), bounds.height.toInt(), swf.frameRate)
		parseMovieClip(swf.tags, AnSymbolMovieClip(0, "MainTimeLine", findLimits(swf.tags)))
		for (symbol in symbols) lib.addSymbol(symbol)
		processAs3Actions()
		generateActualTimelines()
		lib.processSymbolNames()
		generateTextures()
		finalProcessing()
		return lib
	}

	private fun finalProcessing() {
		if (true) {
			//println("totalPlaceObject: $totalPlaceObject")
			//println("totalShowFrame: $totalShowFrame")
		}
	}

	fun getFrameTime(index0: Int) = (index0 * lib.msPerFrameDouble).toInt() * 1000

	suspend private fun generateActualTimelines() {
		for (symbol in lib.symbolsById.filterIsInstance<AnSymbolMovieClip>()) {
			val swfTimeline = symbol.swfTimeline
			var justAfterStopOrStart = true
			var stateStartFrame = 0
			//println(swfTimeline.frames)
			//println("## Symbol: ${symbol.name} : $symbol : ${swfTimeline.frames.size})")

			data class Subtimeline(
				val index: Int,
				var totalFrames: Int = 0,
				var nextState: String? = null,
				var nextStatePlay: Boolean = true
			) {
				//val totalTime get() = getFrameTime(totalFrames - 1)
				//val totalTime get() = getFrameTime(totalFrames)
				val totalTime get() = getFrameTime(totalFrames)
			}

			data class FrameInfo(
				val subtimeline: Subtimeline,
				val frameInSubTimeline: Int,
				val stateName: String,
				val startSubtimeline: Boolean,
				val startNamedState: Boolean
			) {
				val timeInSubTimeline = getFrameTime(frameInSubTimeline)
			}

			val frameInfos = ArrayList<FrameInfo>(swfTimeline.frames.size)

			// Identify referenced frames
			val referencedFrames = hashSetOf<Int>()
			for (frame in swfTimeline.frames) {
				if (frame.hasGoto) {
					val goto = frame.actions.filterIsInstance<MySwfFrame.Action.Goto>().first()
					referencedFrames += goto.frame0
				}
			}

			// Create FrameInfo
			var flow = true
			var stateName = "default"
			var frameIndex = 0
			var subtimelineIndex = -1


			val subtimelines = arrayListOf<Subtimeline>()

			for (frame in swfTimeline.frames) {
				var startNamedState = false
				var startSubtimeline = false
				if (flow) {
					stateName = when {
						frame.isFirst -> "default"
						frame.name != null -> frame.name!!
						else -> "frame${frame.index0}"
					}
					frameIndex = 0
					subtimelineIndex++
					subtimelines += Subtimeline(subtimelineIndex)
					startNamedState = true
					startSubtimeline = true
				}

				if (frame.name != null) {
					stateName = frame.name!!
					startNamedState = true
				} else if (frame.index0 in referencedFrames) {
					stateName = "frame${frame.index0}"
					startNamedState = true
				}

				val subtimeline = subtimelines[subtimelineIndex]
				subtimeline.totalFrames++
				flow = frame.hasFlow

				frameInfos += FrameInfo(subtimeline, frameIndex, stateName, startSubtimeline, startNamedState)

				frameIndex++
			}

			// Compute flow
			for (frame in swfTimeline.frames) {
				val info = frameInfos[frame.index0]
				val isLast = frame.index0 == swfTimeline.frames.last().index0

				if (frame.hasFlow) {
					for (action in frame.actions) {
						when (action) {
							is MySwfFrame.Action.Goto -> {
								info.subtimeline.nextState = frameInfos[action.frame0].stateName
							}
							is MySwfFrame.Action.Stop -> {
								info.subtimeline.nextStatePlay = false
							}
							is MySwfFrame.Action.Play -> {
								info.subtimeline.nextStatePlay = true
							}
						}
					}
				} else {
					if (isLast) {
						info.subtimeline.nextState = "default"
						info.subtimeline.nextStatePlay = true
					}
				}
			}

			val totalDepths = symbol.limits.totalDepths
			var currentSubTimeline = AnSymbolMovieClipSubTimeline(totalDepths)

			val lastDepths = arrayOfNulls<AnSymbolTimelineFrame?>(totalDepths)

			//println("-------------")
			for (frame in swfTimeline.frames) {
				val info = frameInfos[frame.index0]
				val currentTime = info.timeInSubTimeline
				//val isLast = frame.index0 == swfTimeline.frames.last().index0

				// Subtimelines
				if (info.startSubtimeline) {
					currentSubTimeline = AnSymbolMovieClipSubTimeline(totalDepths)
					lastDepths.fill(null)
					val subtimeline = info.subtimeline
					currentSubTimeline.totalTime = subtimeline.totalTime
					currentSubTimeline.nextState = subtimeline.nextState
					currentSubTimeline.nextStatePlay = subtimeline.nextStatePlay

					//println("$currentSubTimeline : $subtimeline")

					//println("currentSubTimeline.totalTime = info.subtimeline.totalTime <- ${info.subtimeline.totalTime}")
					if (frame.isFirst) {
						symbol.states["default"] = AnSymbolMovieClipState("default", currentSubTimeline, 0)
						symbol.states["frame0"] = AnSymbolMovieClipState("frame0", currentSubTimeline, 0)
					}
				}
				// States
				if (info.startNamedState) {
					currentSubTimeline.actions.add(info.timeInSubTimeline, AnEventAction(info.stateName))
					symbol.states[info.stateName] =
							AnSymbolMovieClipState(info.stateName, currentSubTimeline, info.timeInSubTimeline)
				}

				// Compute frame
				//println("$info: $frame")
				for (depth in frame.depths) {
					val n = depth.depth
					val lastDepth = lastDepths[n]
					if (depth != lastDepth) {
						//println(" - [$n]: $depth")
						currentSubTimeline.timelines[depth.depth].add(info.timeInSubTimeline, depth)
						lastDepths[n] = depth
					}
				}

				// Compute actions
				for (it in frame.actions) {
					when (it) {
						is MySwfFrame.Action.PlaySound -> {
							currentSubTimeline.actions.add(currentTime, AnPlaySoundAction(it.soundId))
						}
					}
				}
			}

			// Append first frame of next animation to the end of each animation for smooth transition
			//val frameTime = lib.msPerFrame * 1000
//
			//for (currSubtimeline in symbol.states.map { it.value.subTimeline }.distinct()) {
			//	val nextStateName = currSubtimeline.nextState
			//	if (nextStateName != null) {
			//		val nextState = symbol.states[nextStateName]!!
			//		for ((index, nextTimeline) in nextState.subTimeline.timelines.withIndex()) {
			//			val newFrame = AnSymbolTimelineFrame()
			//			val result = nextTimeline.find(nextState.startTime)
			//			val l = result.left
			//			val r = result.right
			//			val ratio = result.ratio
			//			if (l != null && r != null) {
			//				newFrame.setToInterpolated(l, r, ratio)
			//			} else if (l != null) {
			//				newFrame.copyFrom(l)
			//			} else if (r != null) {
			//				newFrame.copyFrom(r)
			//			}
//
			//			val currPropTimeline = currSubtimeline.timelines[index]
			//			//currSubtimeline.totalTime += frameTime
			//			currPropTimeline.add(currSubtimeline.totalTime, newFrame)
			//			//println(newFrame)
			//		}
//
			//	}
			//}
		}
	}

	suspend private fun processAs3Actions() {
		for ((className, tagId) in classNameToTagId) {
			lib.symbolsById[tagId].name = className
			val type = classNameToTypes[className] ?: continue
			val symbol = (lib.symbolsById[tagId] as? AnSymbolMovieClip?) ?: continue
			val abc = type.abc
			val labelsToFrame0 = symbol.labelsToFrame0

			//println("$tagId :: $className :: $symbol :: $type")
			for (trait in type.instanceTraits) {
				val simpleName = trait.name.simpleName
				//println(" - " + trait.name.simpleName)
				if (simpleName.startsWith("frame")) {
					val frame = ignoreErrors { simpleName.substr(5).toInt() } ?: continue
					val frame0 = frame - 1
					val traitMethod = (trait as ABC.TraitMethod?) ?: continue
					val methodDesc = abc.methodsDesc[traitMethod.methodIndex]
					val body = methodDesc.body ?: continue
					//println("FRAME: $frame0")
					//println(body.ops)

					var lastValue: Any? = null
					for (op in body.ops) {
						when (op.opcode) {
							AbcOpcode.PushByte -> lastValue = (op as AbcIntOperation).value
							AbcOpcode.PushShort -> lastValue = (op as AbcIntOperation).value
							AbcOpcode.PushInt -> lastValue = (op as AbcIntOperation).value
							AbcOpcode.PushUInt -> lastValue = (op as AbcIntOperation).value
							AbcOpcode.PushString -> lastValue = (op as AbcStringOperation).value
							AbcOpcode.CallPropVoid -> {
								val call = (op as AbcMultinameIntOperation)
								val callMethodName = call.multiname.simpleName
								val frameData = symbol.swfTimeline.frames[frame0]
								when (callMethodName) {
									"gotoAndPlay", "gotoAndStop" -> {
										val gotoFrame0 = when (lastValue) {
											is String -> labelsToFrame0[lastValue] ?: 0
											is Int -> lastValue - 1
											else -> 0
										}
										if (callMethodName == "gotoAndStop") {
											frameData.gotoAndStop(gotoFrame0)
										} else {
											frameData.gotoAndPlay(gotoFrame0)
										}
									}
									"play" -> frameData.play()
									"stop" -> frameData.stop()

									else -> {
										//println("method: $callMethodName")
									}
								}
								lastValue = null
							}
							else -> Unit
						}
					}
				}
			}
		}
	}


	suspend private fun generateTextures() {
		val itemsInAtlas = LinkedHashMap<(TextureWithBitmapSlice) -> Unit, BitmapWithScale>()

		for ((shape, rasterizerRequest) in shapesToPopulate) {
			val info = analyzerInfo(rasterizerRequest.charId)
			val rasterizer = rasterizerRequest.getRasterizer(info.globalScaleBounds.max)
			itemsInAtlas.put({ texture -> shape.textureWithBitmap = texture }, rasterizer.imageWithScale)
		}

		for (morph in morphShapesToPopulate) {
			val tag = morph.tagDefineMorphShape!!
			val ratios = (morphShapeRatios[tag.characterId] ?: setOf<Double>()).sorted()
			val MAX_RATIOS = 24
			val aratios =
				if (ratios.size > MAX_RATIOS) (0 until MAX_RATIOS).map { it.toDouble() / (MAX_RATIOS - 1).toDouble() } else ratios
			for (ratio in aratios) {
				val bb = ShapeExporterBoundsBuilder()
				try {
					tag.export(bb, ratio)
				} catch (e: Throwable) {
					e.printStackTrace()
				}
				val bounds = bb.bb.getBounds()
				//bb.bb.add()
				val rasterizer = SWFShapeRasterizer(
					swf, config.debug, bounds,
					{
						try {
							tag.export(it, ratio)
						} catch (e: Throwable) {
							e.printStackTrace()
						}
					},
					config.rasterizerMethod,
					antialiasing = config.antialiasing,
					requestScale = config.exportScale,
					minSide = config.minMorphShapeSide,
					maxSide = config.maxMorphShapeSide
				)
				itemsInAtlas.put(
					{ texture -> morph.texturesWithBitmap.add((ratio * 1000).toInt(), texture) },
					rasterizer.imageWithScale
				)
			}
		}

		for ((processor, texture) in itemsInAtlas.toAtlas(views, config.maxTextureSide, config.mipmaps)) processor(
			texture
		)
	}

	fun findLimits(tags: Iterable<ITag>): AnSymbolLimits {
		var maxDepth = -1
		var totalFrames = 0
		val items = hashSetOf<Pair<Int, Int>>()
		// Find limits
		for (it in tags) {
			when (it) {
				is TagPlaceObject -> {
					if (it.hasCharacter) {
						items += it.depth0 to it.characterId
					}

					maxDepth = max(maxDepth, it.depth0)
					if (it.hasClipDepth) {
						maxDepth = max(maxDepth, it.clipDepth0 + 1)
					}
					//if (it.hasClipDepth) maxDepth = max(maxDepth, it.clipDepth0)
				}
				is TagShowFrame -> {
					totalFrames++
				}
			}
		}
		return AnSymbolLimits(maxDepth + 1, totalFrames, items.size, (totalFrames * lib.msPerFrameDouble).toInt())
	}

	val symbols = arrayListOf<AnSymbol>()

	fun registerBitmap(charId: Int, bmp: Bitmap, name: String? = null) {
		swf.bitmaps[charId] = bmp
		symbols += AnSymbolBitmap(charId, name, bmp)
		//showImageAndWait(bmp)
	}

	var totalPlaceObject = 0
	var globalTotalShowFrame = 0

	var spritesById = hashMapOf<Int, AnSymbolMovieClip>()

	suspend fun parseMovieClip(tags: Iterable<ITag>, mc: AnSymbolMovieClip) {
		symbols += mc

		val swfTimeline = mc.swfTimeline
		val labelsToFrame0 = mc.labelsToFrame0
		val uniqueIds = hashMapOf<Pair<Int, Int>, Int>()

		data class DepthInfo(
			val depth: Int,
			var uid: Int = -1,
			var charId: Int = -1,
			var clipDepth: Int = -1,
			var name: String? = null,
			var colorTransform: ColorTransform = ColorTransform.identity,
			var ratio: Double = 0.0,
			var matrix: Matrix = Matrix(),
			var blendMode: BlendMode = BlendMode.INHERIT
		) {
			fun reset() {
				uid = -1
				ratio = 0.0
				charId = -1
				clipDepth = -1
				colorTransform = ColorTransform.identity
				name = null
				matrix = Matrix()
				blendMode = BlendMode.INHERIT
			}

			//var frameElement = AnSymbolTimelineFrame(); private set

			fun createFrameElement() {
				//frameElement = toFrameElement()
			}

			fun toFrameElement() = AnSymbolTimelineFrame(
				depth = depth,
				clipDepth = clipDepth,
				uid = uid,
				ratio = ratio,
				name = name,
				transform = matrix,
				colorTransform = colorTransform,
				blendMode = blendMode
			)
		}

		val depths = Array(mc.limits.totalDepths) { DepthInfo(it) }

		fun getUid(depth: Int): Int {
			val charId = depths[depth].charId
			return uniqueIds.getOrPut(depth to charId) {
				val uid = uniqueIds.size
				mc.uidInfo[uid] = AnSymbolUidDef(charId)
				uid
			}
		}

		// Add frames and read labels information
		var totalShowFramesInMc = 0
		for (it in tags) {
			val currentFrame = swfTimeline.frames.size
			when (it) {
				is TagDefineSceneAndFrameLabelData -> {
					mc.labelsToFrame0 += it.frameLabels.map { it.name to it.frameNumber - 1 }
				}
				is TagFrameLabel -> {
					mc.labelsToFrame0[it.frameName] = currentFrame
				}
				is TagShowFrame -> {
					swfTimeline.frames += MySwfFrame(currentFrame, mc.limits.totalDepths)
					totalShowFramesInMc++
				}
			}
		}

		//if (totalShowFramesInMc > 0) swfTimeline.frames += MySwfFrame(swfTimeline.frames.size, mc.limits.totalDepths)

		// Populate frame names
		for ((name, index) in mc.labelsToFrame0) swfTimeline.frames[index].name = name

		val depthsChanged = BitSet(depths.size)
		var currentFrame = 0
		for (it in tags) {
			//println("Tag: $it")
			val currentTime = getFrameTime(currentFrame)
			val swfCurrentFrame by lazy { mc.swfTimeline.frames[currentFrame] }
			when (it) {
				is TagDefineSceneAndFrameLabelData -> Unit
				is TagFrameLabel -> Unit
				is TagFileAttributes -> Unit
				is TagSetBackgroundColor -> {
					lib.bgcolor = decodeSWFColor(it.color).rgba
				}
				is TagProtect -> Unit // ignore
				is TagDefineFont -> {
				}
				is TagDefineFontName -> {
				}
				is TagDefineFontAlignZones -> {
				}
				is TagDefineEditText -> {
					symbols += AnTextFieldSymbol(it.characterId, null, it.initialText ?: "", it.bounds.rect)
				}
				is TagCSMTextSettings -> {
				}
				is TagDoAction -> {
					for (action in it.actions) {
						when (action) {
							is ActionStop -> swfCurrentFrame.stop()
							is ActionPlay -> swfCurrentFrame.play()
							is ActionGotoFrame -> swfCurrentFrame.goto(action.frame)
						}
					}
				}
				is TagSoundStreamHead -> {
				}
				is TagDefineSound -> {
					val soundBytes = it.soundData.cloneToNewByteArray()
					symbols += AnSymbolSound(it.characterId, null, null, soundBytes)
				}
				is TagStartSound -> {
					swfCurrentFrame.playSound(it.soundId)
				}
				is TagJPEGTables -> {
					println("Unhandled tag: $it")
				}
				is TagDefineBits, is TagDefineBitsLossless -> {
					var fbmp: Bitmap = Bitmap32(1, 1)
					it as IDefinitionTag

					when (it) {
						is TagDefineBitsJPEG2 -> {
							val bitsData = it.bitmapData.cloneToNewByteArray()
							val nativeBitmap = try {
								bitsData.openAsync().readBitmap(views.imageFormats)
							} catch (e: Throwable) {
								e.printStackTrace()
								Bitmap32(1, 1)
							}
							//println(nativeBitmap)
							val bmp = nativeBitmap.toBMP32()
							fbmp = bmp

							if (it is TagDefineBitsJPEG3) {
								val fmaskinfo = it.bitmapAlphaData.cloneToNewFlashByteArray()
								fmaskinfo.uncompressInWorker("zlib")
								val maskinfo = fmaskinfo.cloneToNewByteArray()
								//val bmpAlpha = nativeImageFormatProvider.decode(maskinfo)
								//showImageAndWait(bmpAlpha)
								bmp.writeChannel(BitmapChannel.ALPHA, Bitmap8(bmp.width, bmp.height, maskinfo))
							}

							//showImageAndWait(bmp)

							//println(bmp)
							//for (y in 0 until bmp.height) {
							//	for (x in 0 until bmp.width) System.out.printf("%08X,", bmp[x, y])
							//	println()
							//}
						}
						is TagDefineBitsLossless -> {
							//val isRgba = it.hasAlpha
							val funcompressedData = it.zlibBitmapData.cloneToNewFlashByteArray()
							funcompressedData.uncompressInWorker("zlib")
							val uncompressedData = funcompressedData.cloneToNewByteArray()
							when (it.bitmapFormat) {
								BitmapFormat.BIT_8 -> {
									val ncolors = it.bitmapColorTableSize
									val s = FastByteArrayInputStream(uncompressedData)
									val clut = if (it.hasAlpha) {
										(0 until ncolors).map { s.readS32LE() }.toIntArray()
									} else {
										(0 until ncolors).map { 0x00FFFFFF.inv() or s.readU24LE() }.toIntArray()
									}
									val pixels = s.readBytes(it.actualWidth * it.actualHeight)

									val bmp = Bitmap8(it.actualWidth, it.actualHeight, pixels, RgbaArray(clut))
									fbmp = bmp
								}
								BitmapFormat.BIT_15 -> {
									fbmp = Bitmap32(it.actualWidth, it.actualHeight, BGRA_5551.decode(uncompressedData))
								}
								BitmapFormat.BIT_24_32 -> {
									val components = uncompressedData.size / (it.bitmapWidth * it.bitmapHeight)
									val colorFormat = BGRA
									//fbmp = Bitmap32(it.bitmapWidth, it.bitmapHeight, colorFormat.decode(uncompressedData, littleEndian = false))
									fbmp = Bitmap32(
										it.bitmapWidth,
										it.bitmapHeight,
										colorFormat.decode(uncompressedData, littleEndian = false)
									)
									if (!it.hasAlpha) {
										for (n in 0 until fbmp.data.size) fbmp.data[n] = RGBA(fbmp.data[n].rgb, 0xFF)
									}
								}
								else -> Unit
							}

						}
					}

					registerBitmap(it.characterId, fbmp, null)
				}
				is TagDefineShape -> {
					val tag = it
					val rasterizerRequest = SWFShapeRasterizerRequest(
						swf,
						tag.characterId,
						tag.shapeBounds.rect,
						{ tag.export(it) },
						config
					)
					//val rasterizer = LoggerShapeExporter(SWFShapeRasterizer(swf, debug, it))
					val symbol =
						AnSymbolShape(it.characterId, null, rasterizerRequest.shapeBounds, null, rasterizerRequest.path)
					symbol.tagDefineShape = it
					symbols += symbol
					shapesToPopulate += symbol to rasterizerRequest
				}
				is TagDefineMorphShape -> {
					val startBounds = it.startBounds.rect
					val endBounds = it.endBounds.rect
					val bounds = BoundsBuilder()
						.add(startBounds)
						.add(endBounds)
						.add(it.startEdgeBounds.rect)
						.add(it.endEdgeBounds.rect)
						.getBounds()

					val bounds2 = bounds.copy(width = bounds.width + 100, height = bounds.height + 100)

					//println("${startBounds.toStringBounds()}, ${endBounds.toStringBounds()} -> ${bounds.toStringBounds()}")

					val symbol = AnSymbolMorphShape(it.characterId, null, bounds2)
					symbol.tagDefineMorphShape = it
					symbols += symbol
					morphShapesToPopulate += symbol
				}
				is TagDoABC -> {
					classNameToTypes += it.abc.typesInfo.map { it.name.toString() to it }.toMap()
				}
				is TagSymbolClass -> {
					classNameToTagId += it.symbols.filter { it.name != null }.map { it.name!! to it.tagId }.toMap()
				}
				is TagDefineSprite -> {
					val childMc = AnSymbolMovieClip(it.characterId, null, findLimits(it.tags))
					spritesById[it.characterId] = childMc
					parseMovieClip(it.tags, childMc)
				}
				is TagDefineScalingGrid -> {
					val childMc = spritesById[it.characterId]
					if (childMc != null) {
						childMc.ninePatch = it.splitter.rect
					}
					analyzerInfo(it.characterId).hasNinePatch = true
				}
				is TagPlaceObject -> {
					totalPlaceObject++
					//val depthId = if (it.hasClipDepth) it.clipDepth0 else it.depth0
					//val clipDepthId = if (it.hasClipDepth) it.depth0 else -1

					val depthId = it.depth0
					val clipDepthId = if (it.hasClipDepth) it.clipDepth0 - 1 else -1

					val depth = depths[depthId]

					if (it.hasCharacter) depth.charId = it.characterId
					if (it.hasClipDepth) depth.clipDepth = clipDepthId
					if (it.hasName) depth.name = it.instanceName
					//if (it.hasBlendMode) depth.blendMode = it.blendMode
					if (it.hasColorTransform) {
						val ct = it.colorTransform!!.toColorTransform()
						depth.colorTransform = ct
						//allColorTransforms += ct
						//println(depth.colorTransform)
					}
					if (it.hasMatrix) {
						val m = it.matrix!!.matrix
						depth.matrix = m
						//allMatrices += m
					}
					if (it.hasBlendMode) depth.blendMode = when (it.blendMode) {
						com.codeazur.as3swf.BlendMode.NORMAL_0 -> BlendMode.NORMAL
						com.codeazur.as3swf.BlendMode.NORMAL_1 -> BlendMode.NORMAL
						com.codeazur.as3swf.BlendMode.LAYER -> BlendMode.INHERIT
						com.codeazur.as3swf.BlendMode.MULTIPLY -> BlendMode.MULTIPLY
						com.codeazur.as3swf.BlendMode.SCREEN -> BlendMode.SCREEN
						com.codeazur.as3swf.BlendMode.LIGHTEN -> BlendMode.LIGHTEN
						com.codeazur.as3swf.BlendMode.DARKEN -> BlendMode.DARKEN
						com.codeazur.as3swf.BlendMode.DIFFERENCE -> BlendMode.DIFFERENCE
						com.codeazur.as3swf.BlendMode.ADD -> BlendMode.ADD
						com.codeazur.as3swf.BlendMode.SUBTRACT -> BlendMode.SUBTRACT
						com.codeazur.as3swf.BlendMode.INVERT -> BlendMode.INVERT
						com.codeazur.as3swf.BlendMode.ALPHA -> BlendMode.ALPHA
						com.codeazur.as3swf.BlendMode.ERASE -> BlendMode.ERASE
					//com.codeazur.as3swf.BlendMode.OVERLAY ->  BlendMode.OVERLAY
						com.codeazur.as3swf.BlendMode.OVERLAY -> BlendMode.INHERIT
						com.codeazur.as3swf.BlendMode.HARDLIGHT -> BlendMode.HARDLIGHT
						else -> BlendMode.INHERIT
					}
					val uid = getUid(depthId)
					val metaData = it.metaData
					if (metaData != null && metaData is Map<*, *> && "props" in metaData) {
						val uidInfo = mc.uidInfo[uid]
						val eprops = ignoreErrors { Json.decode(metaData["props"].toString()) as Map<String, String> }
						if (eprops != null) uidInfo.extraProps += eprops
						//println(depth.extraProps)
					}

					if (it.hasRatio) {
						depth.ratio = it.ratiod
						val ratios = morphShapeRatios.getOrPut(depth.charId) { hashSetOf() }
						ratios += it.ratiod
					}

					analyzerInfo(depth.charId).registerParent(analyzerInfo(mc.id))
					analyzerInfo(depth.charId).registerMatrix(depth.matrix)

					depth.uid = uid
					//println("+$depthId")
					depthsChanged[depthId] = true
					//println("-$depthId")
					//depth.createFrameElement()
				}
				is TagRemoveObject -> {
					depths[it.depth0].reset()
					depthsChanged[it.depth0] = true
					//depths[it.depth0].createFrameElement()
				}
				is TagShowFrame -> {
					globalTotalShowFrame++
					for (depth in depths) {
						//if (depthsChanged[depth.depth]) swfCurrentFrame.depths += depth.toFrameElement()
						//swfCurrentFrame.depths += depth.frameElement
						swfCurrentFrame.depths += depth.toFrameElement()
					}
					depthsChanged.clear()
					currentFrame++
				}
				is TagEnd -> {
				}
				else -> {
					println("Unhandled tag $it")
				}
			}
		}

		//if (totalShowFramesInMc > 0) {
		//	val lastFrame = mc.swfTimeline.frames.last()
		//	for (depth in depths) lastFrame.depths += depth.toFrameElement()
		//}

	}
}

