package com.soywiz.korge.ext.swf

import com.soywiz.kds.BitSet
import com.soywiz.kds.memoize
import com.soywiz.korfl.as3swf.*
import com.soywiz.klock.*
import com.soywiz.korfl.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.filter.composedOrNull
import com.soywiz.korge.view.filter.BlurFilter
import com.soywiz.korge.view.filter.ComposedFilter
import com.soywiz.korge.view.filter.DropshadowFilter
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korio.stream.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.krypto.encoding.hexLower
import kotlinx.coroutines.*
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

fun TagDefineShape.getShapeExporter(swf: SWF, config: SWFExportConfig, maxScale: Double, path: VectorPath = VectorPath()): SWFShapeExporter {
    val adaptiveScaling = if (config.adaptiveScaling) maxScale else 1.0
    //val maxScale = if (maxScale != 0.0) 1.0 / maxScale else 1.0
    //println("SWFShapeRasterizerRequest: $charId: $adaptiveScaling : $config")
    return SWFShapeExporter(
        swf,
        config.debug,
        this.shapeBounds.rect,
        { this.export(it) },
        rasterizerMethod = config.rasterizerMethod,
        antialiasing = config.antialiasing,
        //requestScale = config.exportScale / maxScale.clamp(0.0001, 4.0),
        requestScale = config.exportScale * adaptiveScaling,
        minSide = config.minShapeSide,
        maxSide = config.maxShapeSide,
        path = path,
        charId = this.characterId,
        roundDecimalPlaces = config.roundDecimalPlaces,
        graphicsRenderer = config.graphicsRenderer,
    )
}

fun TagDefineMorphShape.getShapeExporter(swf: SWF, config: SWFExportConfig, maxScale: Double, path: VectorPath = VectorPath()): SWFShapeExporter {
    val adaptiveScaling = if (config.adaptiveScaling) maxScale else 1.0
    //val maxScale = if (maxScale != 0.0) 1.0 / maxScale else 1.0
    //println("SWFShapeRasterizerRequest: $charId: $adaptiveScaling : $config")
    return SWFShapeExporter(
        swf,
        config.debug,
        this.startBounds.rect,
        { this.export(it) },
        rasterizerMethod = config.rasterizerMethod,
        antialiasing = config.antialiasing,
        //requestScale = config.exportScale / maxScale.clamp(0.0001, 4.0),
        requestScale = config.exportScale * adaptiveScaling,
        minSide = config.minShapeSide,
        maxSide = config.maxShapeSide,
        path = path,
        charId = this.characterId,
        roundDecimalPlaces = config.roundDecimalPlaces,
        graphicsRenderer = config.graphicsRenderer,
    )
}

class SWFShapeRasterizerRequest constructor(
	val swf: SWF,
    val tag: TagDefineShape,
	val config: SWFExportConfig
) {
    val charId: Int get () = tag.characterId
    val shapeBounds: Rectangle get() = tag.shapeBounds.rect
	val path = VectorPath()
	fun getShapeExporter(maxScale: Double): SWFShapeExporter = tag.getShapeExporter(swf, config, maxScale, path)
}

class SwfLoaderMethod(val context: AnLibrary.Context, val config: SWFExportConfig) {
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
		lib = AnLibrary(context, bounds.width.toInt(), bounds.height.toInt(), swf.frameRate)
        parseMovieClip(swf.tags, AnSymbolMovieClip(0, "MainTimeLine", findLimits(swf.tags)))
        for (symbol in symbols) lib.addSymbol(symbol)
        try {
            processAs3Actions()
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            e.printStackTrace()
        }
		generateActualTimelines()
		lib.processSymbolNames()
        if (config.generateTextures) {
            generateTextures()
        } else {
            generateShapes()
        }
		finalProcessing()
		return lib
	}

	private fun finalProcessing() {
		if (true) {
			//println("totalPlaceObject: $totalPlaceObject")
			//println("totalShowFrame: $totalShowFrame")
		}
	}

	fun getFrameTime(index0: Int) = (index0 * lib.msPerFrameDouble).toInt().milliseconds

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
                                if (action.frame0 >= frameInfos.size) {
                                    println("ERROR: action.frame0 >= frameInfos.size :: ${action.frame0} >= ${frameInfos.size}")
                                }
								info.subtimeline.nextState = frameInfos.getOrNull(action.frame0)?.stateName
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
						symbol.states["default"] = AnSymbolMovieClipState("default", currentSubTimeline, 0.milliseconds)
						symbol.states["frame0"] = AnSymbolMovieClipState("frame0", currentSubTimeline, 0.milliseconds)
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

	private fun processAs3Actions() {
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
					val frame = runIgnoringExceptions { simpleName.substr(5).toInt() } ?: continue
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

    private fun generateShapes() {
        for (shape in shapesToPopulate.keys) {
            val tag = shape.tagDefineShape!!
            shape.shapeGen = { tag.export(swf.bitmaps) }.memoize()
            shape.graphicsRenderer = config.graphicsRenderer
            //itemsInAtlas.put({ texture -> shape.textureWithBitmap = texture }, rasterizer.imageWithScale)
        }
        for (morph in morphShapesToPopulate) {
            val tag = morph.tagDefineMorphShape!!
            morph.shapeGen = { ratio ->
                tag.export(swf.bitmaps, ratio)
            }
        }
    }

	private suspend fun generateTextures() {
		val itemsInAtlas = LinkedHashMap<(TextureWithBitmapSlice) -> Unit, BitmapWithScale>()

		for ((shape, rasterizerRequest) in shapesToPopulate) {
			val info = analyzerInfo(rasterizerRequest.charId)
			val rasterizer = rasterizerRequest.getShapeExporter(info.globalScaleBounds.max)
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
                    if (e is CancellationException) throw e
					e.printStackTrace()
				}
				val bounds = bb.bb.getBounds()
				//bb.bb.add()
				val rasterizer = SWFShapeExporter(
					swf, config.debug, bounds,
					{
						try {
							tag.export(it, ratio)
                        } catch (e: Throwable) {
                            if (e is CancellationException) throw e
							e.printStackTrace()
						}
					},
                    rasterizerMethod = config.rasterizerMethod,
					antialiasing = config.antialiasing,
					requestScale = config.exportScale,
					minSide = config.minMorphShapeSide,
					maxSide = config.maxMorphShapeSide,
                    charId = morph.id,
                    graphicsRenderer = config.graphicsRenderer,
				)
				itemsInAtlas.put(
					{ texture -> morph.texturesWithBitmap.add(ratio.seconds, texture) },
					rasterizer.imageWithScale
				)
			}
		}

		for ((processor, texture) in itemsInAtlas.toAtlas(context, config.maxTextureSide, config.mipmaps, config.atlasPacking)) processor(texture)
        //for ((processor, texture) in itemsInAtlas) processor(texture)
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
		return AnSymbolLimits(maxDepth + 1, totalFrames, items.size, (totalFrames * lib.msPerFrameDouble).toInt().milliseconds)
	}

    var pathsArePostScript = false
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
        try {
            parseMovieClipInternal(tags, mc)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

	private suspend fun parseMovieClipInternal(tags: Iterable<ITag>, mc: AnSymbolMovieClip) {
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
            var colorTransform: ColorTransform = ColorTransform(),
            var ratio: Double = 0.0,
            var matrix: Matrix = Matrix(),
            var blendMode: BlendMode = BlendMode.INHERIT,
            var filterList: List<IFilter> = emptyList(),
		) {
			fun reset() {
				uid = -1
				ratio = 0.0
				charId = -1
				clipDepth = -1
				colorTransform.setToIdentity()
				name = null
				matrix = Matrix()
				blendMode = BlendMode.INHERIT
			}

			//var frameElement = AnSymbolTimelineFrame(); private set

			fun createFrameElement() {
				//frameElement = toFrameElement()
			}

            fun computeBlur(value: Double, passes: Int): Double {
                val passesFactor = when (passes) {
                    1 -> 2.5
                    2 -> 3.0
                    3 -> 8.3290429691304455
                    else -> 1.0
                }
                //println("value=$value, passes=$passes")
                return value / passesFactor
            }

			fun toFrameElement() = AnSymbolTimelineFrame(
				depth = depth,
				clipDepth = clipDepth,
				uid = uid,
				ratio = ratio,
				name = name,
				transform = matrix,
				colorTransform = colorTransform,
				blendMode = blendMode,
                filter = filterList.mapNotNull { when (it) {
                    is FilterBlur -> {
                        // passes=1=LOW, passes=2=MED, passes=3=HIGH
                        val blurAvg = (it.blurX + it.blurY) * 0.5
                        //val blurAmount = blurAvg / 16.0
                        //val blurAmount = blurAvg / 6.0
                        //val blurAmount = blurAvg / 4.0
                        //val blurAmount = sqrt(sqrt(blurAvg)) * it.passes
                        //val blurAmount = ln(sqrt(blurAvg) * it.passes)
                        //val blurAmount = sqrt(blurAvg * ln(it.passes.toDouble()))
                        val blurAmount = computeBlur(blurAvg, it.passes)
                        //val blurAmount = log(blurAvg, 1.2)
                        //val blurAmount = blurAvg.pow(0.75)
                        //val blurAmount = ln(blurAvg)
                        //val blurAmount = log(blurAvg, 2.0)
                        //BlurFilter(log(blurAmount, 2.0))
                        //println("blurAmount=$blurAmount, blurX=${it.blurX}, blurY=${it.blurY}, passes=${it.passes}")
                        //ComposedFilter((0 until it.passes).map { BlurFilter(blurAmount, optimize = false) })
                        //ComposedFilter(BlurFilter(blurAmount, optimize = it.passes <= 2))
                        ComposedFilter(BlurFilter(blurAmount, optimize = false))
                        //ComposedFilter(BlurFilter(blurAmount, optimize = true))
                        //ComposedFilter(BlurFilter(blurAmount, optimize = true))
                        //ComposedFilter(BlurFilter(blurAmount, optimize = false))
                    }
                    is FilterDropShadow -> {
                        DropshadowFilter(it.blurX, it.blurY, decodeSWFColor(it.dropShadowColor), blurRadius = computeBlur(it.distance, it.passes))
                    }
                    else -> {
                        println("Unimplemented SWF filter: $it")
                        null
                    }
                } }.composedOrNull()
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
                is TagSoundStreamHead -> {
                    println("Unhandled tag: $it")
                }
                is TagSoundStreamBlock -> {
                }
                is TagDefineText -> {
                    println("Unhandled tag: TagDefineText")
                }
                is TagDefineButton -> {
                    println("Unhandled tag: TagDefineButton")
                }
                /*
                is TagPathsArePostScript -> {
                    pathsArePostScript = true
                }
                is TagDefineButton -> {
                    symbols += AnSymbolButton(it.characterId, it.name)
                }
                is TagDefineButton2 -> {
                    symbols += AnSymbolVideo(it.characterId, it.name)
                }
                is TagDefineVideoStream -> {
                    symbols += AnSymbolVideo(it.characterId, it.name)
                }
                */
				is TagDefineBits, is TagDefineBitsLossless -> {
					var fbmp: Bitmap = Bitmap32(1, 1, premultiplied = false)
					it as IDefinitionTag

					when (it) {
						is TagDefineBits -> {
                        //is TagDefineBitsJPEG2 -> {
							var bitsData = it.bitmapData.cloneToNewByteArray()
							val nativeBitmap = try {
								//bitsData.openAsync().readBitmapListNoNative(JPEG + PNG).first()
                                if (bitsData.size >= 4 && bitsData.sliceArray(0 until 4).hexLower == "ffd9ffd8") {
                                    bitsData = bitsData.sliceArray(4 until bitsData.size)
                                }
                                bitsData.openAsync().readBitmap(ImageDecodingProps(format = context.imageFormats))
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
                            //println("LOSSLESS: ${it.bitmapWidth}, ${it.bitmapHeight} : ${it.bitmapFormat}, it.hasAlpha=${it.hasAlpha}")
							when (it.bitmapFormat) {
								BitmapFormat.BIT_8 -> {
									val ncolors = it.bitmapColorTableSize
									val s = FastByteArrayInputStream(uncompressedData)
                                    //println("FastByteArrayInputStream(uncompressedData): size=${uncompressedData.size}, alpha=${it.hasAlpha}, width=${it.actualWidth}, height=${it.actualHeight}, ncolors=$ncolors")
									val clut = if (it.hasAlpha) {
										(0 until ncolors).map { s.readS32LE() }.toIntArray()
									} else {
										(0 until ncolors).map { 0x00FFFFFF.inv() or s.readU24LE() }.toIntArray()
									}
                                    val nbytes = it.actualWidth * it.actualHeight
                                    val rpixels = ByteArray(nbytes)
                                    s.read(rpixels, 0, nbytes)

									val bmp = Bitmap8(it.actualWidth, it.actualHeight, rpixels, RgbaArray(clut))
									fbmp = bmp
								}
								BitmapFormat.BIT_15 -> {
									fbmp = Bitmap32(it.actualWidth, it.actualHeight, BGRA_5551.decode(uncompressedData))
								}
								BitmapFormat.BIT_24_32 -> {
									val components = uncompressedData.size / (it.bitmapWidth * it.bitmapHeight)
									val colorFormat = BGRA
									//fbmp = Bitmap32(it.bitmapWidth, it.bitmapHeight, colorFormat.decode(uncompressedData, littleEndian = false))
									val bmp = Bitmap32(
										it.bitmapWidth,
										it.bitmapHeight,
										colorFormat.decode(uncompressedData, littleEndian = false)
									)
                                    fbmp = bmp
                                    if (!it.hasAlpha) {
										for (n in 0 until fbmp.ints.size) {
                                            bmp.setRgbaAtIndex(n, RGBA(bmp.getRgbaAtIndex(n).rgb, 0xFF))
                                        }
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
					val rasterizerRequest = SWFShapeRasterizerRequest(swf, tag, config)
					//val rasterizer = LoggerShapeExporter(SWFShapeRasterizer(swf, debug, it))
					val symbol = AnSymbolShape(it.characterId, null, rasterizerRequest.shapeBounds, null, rasterizerRequest.path)
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
                        //println("colorTransform=$ct")
						depth.colorTransform = ct
						//allColorTransforms += ct
						//println(depth.colorTransform)
					} else {
                        depth.colorTransform = ColorTransform()
                    }
					if (it.hasMatrix) {
						val m = it.matrix!!.matrix
						depth.matrix = m
						//allMatrices += m
					}
					if (it.hasBlendMode) depth.blendMode = when (it.blendMode) {
						com.soywiz.korfl.as3swf.BlendMode.NORMAL_0 -> BlendMode.NORMAL
						com.soywiz.korfl.as3swf.BlendMode.NORMAL_1 -> BlendMode.NORMAL
						com.soywiz.korfl.as3swf.BlendMode.LAYER -> BlendMode.INHERIT
						com.soywiz.korfl.as3swf.BlendMode.MULTIPLY -> BlendMode.MULTIPLY
						com.soywiz.korfl.as3swf.BlendMode.SCREEN -> BlendMode.SCREEN
						com.soywiz.korfl.as3swf.BlendMode.LIGHTEN -> BlendMode.LIGHTEN
						com.soywiz.korfl.as3swf.BlendMode.DARKEN -> BlendMode.DARKEN
						com.soywiz.korfl.as3swf.BlendMode.DIFFERENCE -> BlendMode.DIFFERENCE
						com.soywiz.korfl.as3swf.BlendMode.ADD -> BlendMode.ADD
						com.soywiz.korfl.as3swf.BlendMode.SUBTRACT -> BlendMode.SUBTRACT
						com.soywiz.korfl.as3swf.BlendMode.INVERT -> BlendMode.INVERT
						com.soywiz.korfl.as3swf.BlendMode.ALPHA -> BlendMode.ALPHA
						com.soywiz.korfl.as3swf.BlendMode.ERASE -> BlendMode.ERASE
					//com.soywiz.korfl.as3swf.BlendMode.OVERLAY ->  BlendMode.OVERLAY
						com.soywiz.korfl.as3swf.BlendMode.OVERLAY -> BlendMode.INHERIT
						com.soywiz.korfl.as3swf.BlendMode.HARDLIGHT -> BlendMode.HARDLIGHT
						else -> BlendMode.INHERIT
					}
                    depth.filterList = when {
                        it.hasFilterList -> it.surfaceFilterList.toList()
                        else -> emptyList()
                    }
					val uid = getUid(depthId)
					val metaData = it.metaData
					if (metaData != null && metaData is Map<*, *> && "props" in metaData) {
						val uidInfo = mc.uidInfo[uid]
						val eprops = runIgnoringExceptions { Json.parse(metaData["props"].toString()) as Map<String, String> }
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
                is IDefinitionTag -> {
                    println("Unhandled tag $it - Can't continue without handling th define tag $it")
                    //error("Can't continue without handling th define tag $it")
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

