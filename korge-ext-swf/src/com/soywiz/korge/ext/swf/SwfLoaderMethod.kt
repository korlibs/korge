package com.soywiz.korge.ext.swf

import com.codeazur.as3swf.SWF
import com.codeazur.as3swf.data.actions.ActionGotoFrame
import com.codeazur.as3swf.data.actions.ActionPlay
import com.codeazur.as3swf.data.actions.ActionStop
import com.codeazur.as3swf.data.consts.BitmapFormat
import com.codeazur.as3swf.exporters.ShapeExporterBoundsBuilder
import com.codeazur.as3swf.tags.*
import com.soywiz.korau.format.AudioFormats
import com.soywiz.korau.format.toNativeSound
import com.soywiz.korau.sound.nativeSoundProvider
import com.soywiz.korfl.abc.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.ColorTransform
import com.soywiz.korge.view.Views
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmap8
import com.soywiz.korim.bitmap.BitmapChannel
import com.soywiz.korim.color.BGRA
import com.soywiz.korim.color.BGRA_5551
import com.soywiz.korim.color.RGB
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.error.ignoreErrors
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.util.substr
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.BoundsBuilder
import java.util.*

class SwfLoaderMethod(val views: Views, val config: SWFExportConfig) {
	lateinit var swf: SWF
	lateinit var lib: AnLibrary
	val classNameToTypes = hashMapOf<String, ABC.TypeInfo>()
	val classNameToTagId = hashMapOf<String, Int>()
	val shapesToPopulate = LinkedHashMap<AnSymbolShape, SWFShapeRasterizer>()
	val morphShapesToPopulate = arrayListOf<AnSymbolMorphShape>()
	val morphShapeRatios = hashMapOf<Int, HashSet<Double>>()

	suspend fun load(data: ByteArray): AnLibrary {
		swf = SWF().loadBytes(data)
		lib = AnLibrary(views, swf.frameRate)
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

			data class Subtimeline(val index: Int, var totalFrames: Int = 0, var nextState: String? = null, var nextStatePlay: Boolean = true) {
				//val totalTime get() = getFrameTime(totalFrames - 1)
				val totalTime get() = getFrameTime(totalFrames)
			}

			data class FrameInfo(val subtimeline: Subtimeline, val frameInSubTimeline: Int, val stateName: String, val startSubtimeline: Boolean, val startNamedState: Boolean) {
				val timeInSubTimeline = getFrameTime(frameInSubTimeline)
			}

			val frameInfos = java.util.ArrayList<FrameInfo>(swfTimeline.frames.size)

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

			val lastDepths = kotlin.arrayOfNulls<AnSymbolTimelineFrame?>(totalDepths)

			//println("-------------")
			for (frame in swfTimeline.frames) {
				val info = frameInfos[frame.index0]
				val currentTime = info.timeInSubTimeline
				//val isLast = frame.index0 == swfTimeline.frames.last().index0

				// Subtimelines
				if (info.startSubtimeline) {
					currentSubTimeline = AnSymbolMovieClipSubTimeline(totalDepths)
					Arrays.fill(lastDepths, null)
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
					symbol.states[info.stateName] = AnSymbolMovieClipState(info.stateName, currentSubTimeline, info.timeInSubTimeline)
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
				val anActions = arrayListOf<AnAction>()
				for (it in frame.actions) {
					when (it) {
						is MySwfFrame.Action.PlaySound -> {
							anActions += AnPlaySoundAction(it.soundId)
						}
					}
				}
				if (anActions.isNotEmpty()) currentSubTimeline.actions.add(currentTime, AnActions(anActions))
			}
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

		for ((shape, rasterizer) in shapesToPopulate) {
			itemsInAtlas.put({ texture -> shape.textureWithBitmap = texture }, rasterizer.imageWithScale)
		}

		for (morph in morphShapesToPopulate) {
			val tag = morph.tagDefineMorphShape!!
			val ratios = (morphShapeRatios[tag.characterId] ?: setOf<Double>()).sorted()
			val MAX_RATIOS = 24
			val aratios = if (ratios.size > MAX_RATIOS) (0 until MAX_RATIOS).map { it.toDouble() / (MAX_RATIOS - 1).toDouble() } else ratios
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
				itemsInAtlas.put({ texture -> morph.texturesWithBitmap.add((ratio * 1000).toInt(), texture) }, rasterizer.imageWithScale)
			}
		}

		for ((processor, texture) in itemsInAtlas.toAtlas(views, config.mipmaps)) processor(texture)
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
					maxDepth = Math.max(maxDepth, it.depth0)
					//if (it.hasClipDepth) maxDepth = Math.max(maxDepth, it.clipDepth0)
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
	var totalShowFrame = 0

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
			var matrix: Matrix2d = Matrix2d(),
			var blendMode: BlendMode = BlendMode.INHERIT
		) {
			fun reset() {
				uid = -1
				ratio = 0.0
				charId = -1
				clipDepth = -1
				colorTransform = ColorTransform.identity
				name = null
				matrix = Matrix2d()
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
				}
			}
		}

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
					lib.bgcolor = decodeSWFColor(it.color)
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
					val audioData = try {

						nativeSoundProvider.createSound(soundBytes)
					} catch (e: Throwable) {
						e.printStackTrace()
						null
					}
					symbols += AnSymbolSound(it.characterId, null, audioData, soundBytes)
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
								bitsData.openAsync().readBitmap()
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
							val isRgba = it.hasAlpha
							val funcompressedData = it.zlibBitmapData.cloneToNewFlashByteArray()
							funcompressedData.uncompressInWorker("zlib")
							val uncompressedData = funcompressedData.cloneToNewByteArray()
							when (it.bitmapFormat) {
								BitmapFormat.BIT_8 -> {
									val bmp = Bitmap8(it.bitmapWidth, it.bitmapHeight)
									fbmp = bmp
								}
								BitmapFormat.BIT_15 -> {
									fbmp = Bitmap32(it.bitmapWidth, it.bitmapHeight, BGRA_5551.decode(uncompressedData))
								}
								BitmapFormat.BIT_24_32 -> {
									val colorFormat = if (isRgba) BGRA else RGB
									fbmp = Bitmap32(it.bitmapWidth, it.bitmapHeight, colorFormat.decode(uncompressedData, littleEndian = false))
								}
								else -> Unit
							}

						}
					}

					registerBitmap(it.characterId, fbmp, null)
				}
				is TagDefineShape -> {
					val tag = it
					val rasterizer = SWFShapeRasterizer(
						swf,
						config.debug,
						tag.shapeBounds.rect,
						{ tag.export(it) },
						config.rasterizerMethod,
						antialiasing = config.antialiasing,
						requestScale = config.exportScale,
						minSide = config.minShapeSide,
						maxSide = config.maxShapeSide
					)
					//val rasterizer = LoggerShapeExporter(SWFShapeRasterizer(swf, debug, it))
					val symbol = AnSymbolShape(it.characterId, null, rasterizer.bounds, null, rasterizer.path)
					symbol.tagDefineShape = it
					symbols += symbol
					shapesToPopulate += symbol to rasterizer
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
						SwfBlendMode.NORMAL_0 ->  BlendMode.NORMAL
						SwfBlendMode.NORMAL_1 ->  BlendMode.NORMAL
						SwfBlendMode.LAYER ->  BlendMode.INHERIT
						SwfBlendMode.MULTIPLY ->  BlendMode.MULTIPLY
						SwfBlendMode.SCREEN ->  BlendMode.SCREEN
						SwfBlendMode.LIGHTEN ->  BlendMode.LIGHTEN
						SwfBlendMode.DARKEN ->  BlendMode.DARKEN
						SwfBlendMode.DIFFERENCE ->  BlendMode.DIFFERENCE
						SwfBlendMode.ADD ->  BlendMode.ADD
						SwfBlendMode.SUBTRACT ->  BlendMode.SUBTRACT
						SwfBlendMode.INVERT ->  BlendMode.INVERT
						SwfBlendMode.ALPHA ->  BlendMode.ALPHA
						SwfBlendMode.ERASE ->  BlendMode.ERASE
						//SwfBlendMode.OVERLAY ->  BlendMode.OVERLAY
						SwfBlendMode.OVERLAY ->  BlendMode.INHERIT
						SwfBlendMode.HARDLIGHT ->  BlendMode.HARDLIGHT
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

					depth.uid = uid
					depthsChanged[depthId] = true
					//depth.createFrameElement()
				}
				is TagRemoveObject -> {
					depths[it.depth0].reset()
					depthsChanged[it.depth0] = true
					//depths[it.depth0].createFrameElement()
				}
				is TagShowFrame -> {
					totalShowFrame++
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
	}
}

private typealias SwfBlendMode = com.codeazur.as3swf.data.consts.BlendMode

