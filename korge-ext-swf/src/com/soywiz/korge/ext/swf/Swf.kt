package com.soywiz.korge.ext.swf

import com.codeazur.as3swf.SWF
import com.codeazur.as3swf.data.GradientType
import com.codeazur.as3swf.data.actions.ActionGotoFrame
import com.codeazur.as3swf.data.actions.ActionPlay
import com.codeazur.as3swf.data.actions.ActionStop
import com.codeazur.as3swf.data.consts.BitmapFormat
import com.codeazur.as3swf.data.consts.GradientInterpolationMode
import com.codeazur.as3swf.data.consts.GradientSpreadMode
import com.codeazur.as3swf.data.consts.LineCapsStyle
import com.codeazur.as3swf.exporters.LoggerShapeExporter
import com.codeazur.as3swf.exporters.ShapeExporter
import com.codeazur.as3swf.tags.*
import com.soywiz.korau.format.AudioFormats
import com.soywiz.korfl.abc.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.resources.Path
import com.soywiz.korge.view.Views
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.BGRA
import com.soywiz.korim.color.BGRA_5551
import com.soywiz.korim.color.RGB
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.util.*
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Rectangle
import kotlin.collections.set

@AsyncFactoryClass(SwfLibraryFactory::class)
class SwfLibrary(val an: AnLibrary)

class SwfLibraryFactory(
	val path: Path,
	val views: Views
) : AsyncFactory<SwfLibrary> {
	suspend override fun create(): SwfLibrary = SwfLibrary(ResourcesVfs[path.path].readSWF(views))
}

inline val TagPlaceObject.depth0: Int get() = this.depth - 1
inline val TagRemoveObject.depth0: Int get() = this.depth - 1

val SWF.bitmaps by Extra.Property { hashMapOf<Int, Bitmap>() }

class MySwfFrameElement(
	val depth: Int,
	val uid: Int,
	val name: String?,
	val transform: Matrix2d.Computed,
	val alpha: Double
) {
	fun toAnSymbolTimelineFrame() = AnSymbolTimelineFrame(uid, transform, name, alpha)
}

class MySwfFrame(val index: Int, maxDepths: Int) {
	var name: String? = null
	val depths = arrayListOf<MySwfFrameElement>()
	val actions = arrayListOf<Action>()

	interface Action {
		object Stop : Action
		object Play : Action
		class Goto(val frame0: Int) : Action
		class PlaySound(val soundId: Int) : Action
	}

	val isFirst: Boolean get() = index == 0
	val hasName: Boolean get() = name != null
	val hasStop: Boolean get() = Action.Stop in actions
	val hasGoto: Boolean get() = actions.any { it is Action.Goto }

	fun stop() = run { actions += Action.Stop }
	fun play() = run { actions += Action.Play }
	fun goto(frame: Int) = run { actions += Action.Goto(frame) }
	fun gotoAndStop(frame: Int) = run { goto(frame); stop() }
	fun gotoAndPlay(frame: Int) = run { goto(frame); play() }
	fun playSound(soundId: Int) = run { actions += Action.PlaySound(soundId) }
}

class MySwfTimeline {
	val frames = arrayListOf<MySwfFrame>()
}

internal val AnSymbolMovieClip.swfTimeline by Extra.Property { MySwfTimeline() }
internal val AnSymbolMovieClip.labelsToFrame0 by Extra.Property { hashMapOf<String, Int>() }

private class SwfLoaderMethod(val views: Views, val debug: Boolean) {
	lateinit var swf: SWF
	lateinit var lib: AnLibrary
	val classNameToTypes = hashMapOf<String, ABC.TypeInfo>()
	val classNameToTagId = hashMapOf<String, Int>()
	val shapesToPopulate = arrayListOf<Pair<AnSymbolShape, SWFShapeRasterizer>>()

	suspend fun load(data: ByteArray): AnLibrary {
		swf = SWF().loadBytes(data)
		lib = AnLibrary(views, swf.frameRate)
		parseMovieClip(swf.tags, AnSymbolMovieClip(0, "MainTimeLine", findLimits(swf.tags)))
		for (symbol in symbols) lib.addSymbol(symbol)
		processAs3Actions()
		generateTextures()
		generateActualTimelines()
		lib.processSymbolNames()
		return lib
	}

	fun getFrameTime(index0: Int) = (index0 * lib.msPerFrameDouble).toInt() * 1000

	suspend private fun generateActualTimelines() {
		for (symbol in lib.symbolsById.filterIsInstance<AnSymbolMovieClip>()) {
			val swfTimeline = symbol.swfTimeline
			var currentState = AnSymbolMovieClipState(symbol.limits.totalDepths)
			var justAfterStop = true
			var stateStartFrame = 0
			//println(swfTimeline.frames)
			for (frame in swfTimeline.frames) {
				//println("Frame:(${frame.index})")
				// Create State
				if (justAfterStop) {
					justAfterStop = false
					stateStartFrame = frame.index
					currentState = AnSymbolMovieClipState(symbol.limits.totalDepths)
					symbol.states["frame${frame.index}"] = AnSymbolMovieClipStateWithStartTime(currentState, 0)
				}

				val frameInState = frame.index - stateStartFrame
				val currentTime = getFrameTime(frameInState)

				val isLast = frame.index >= swfTimeline.frames.size - 1

				// Register State
				if (frame.isFirst) symbol.states["default"] = AnSymbolMovieClipStateWithStartTime(currentState, currentTime)
				if (frame.hasName) symbol.states[frame.name!!] = AnSymbolMovieClipStateWithStartTime(currentState, currentTime)

				// Compute frame
				for (depth in frame.depths) {
					currentState.timelines[depth.depth].add(currentTime, depth.toAnSymbolTimelineFrame())
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
				if (anActions.isNotEmpty()) currentState.actions.add(currentTime, AnActions(anActions))

				if (isLast || frame.hasStop || frame.hasGoto) {
					//println(" - $isLast,${frame.hasStop},${frame.hasGoto}")
					justAfterStop = true

					if (frame.hasStop) {
						currentState.loopStartTime = currentTime
					}
					if (frame.hasGoto) {
						val goto = frame.actions.filterIsInstance<MySwfFrame.Action.Goto>().first()

						currentState.loopStartTime = getFrameTime(goto.frame0 - stateStartFrame)
					}
					val stateEndFrame = frameInState
					currentState.totalTime = getFrameTime(stateEndFrame - stateStartFrame)
				}
			}
		}
	}

	suspend private fun processAs3Actions() {
		for ((className, tagId) in classNameToTagId) {
			lib.symbolsById[tagId].name = className
			val type = classNameToTypes[className] ?: continue
			val symbol = (lib.symbolsById[tagId] as AnSymbolMovieClip?) ?: continue
			val abc = type.abc
			val labelsToFrame0 = symbol.labelsToFrame0

			//println("$tagId :: $className :: $symbol :: $type")
			for (trait in type.instanceTraits) {
				val simpleName = trait.name.simpleName
				//println(" - " + trait.name.simpleName)
				if (simpleName.startsWith("frame")) {
					val frame = simpleName.substr(5).toIntOrNull() ?: continue
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
						}
					}
				}
			}
		}
	}

	suspend private fun generateTextures() {
		val atlas = shapesToPopulate.map { it.second.image }.toAtlas(views)

		for ((shape, texture) in shapesToPopulate.map { it.first }.zip(atlas)) shape.texture = texture
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

	suspend fun parseMovieClip(tags: Iterable<ITag>, mc: AnSymbolMovieClip) {
		symbols += mc

		val swfTimeline = mc.swfTimeline
		val labelsToFrame0 = mc.labelsToFrame0
		val uniqueIds = hashMapOf<Pair<Int, Int>, Int>()

		class DepthInfo(val depth: Int) {
			var uid: Int = -1
			var charId: Int = -1
			var name: String? = null
			var alpha: Double = 1.0
			var matrix: Matrix2d = Matrix2d()

			fun reset() {
				uid = -1
				charId = -1
				name = null
				matrix = Matrix2d()
			}

			fun toFrameElement(): MySwfFrameElement = MySwfFrameElement(
				depth = depth,
				uid = uid,
				name = name,
				transform = Matrix2d.Computed(matrix),
				alpha = alpha
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
						AudioFormats.decode(soundBytes.openAsync())
					} catch (e: Throwable) {
						e.printStackTrace()
						null
					}
					symbols += AnSymbolSound(it.characterId, null, audioData)
					//LocalVfs("c:/temp/temp.mp3").write()
				}
				is TagStartSound -> {
					swfCurrentFrame.playSound(it.soundId)
				}
				is TagDefineBits, is TagDefineBitsLossless -> {
					var fbmp: Bitmap = Bitmap32(1, 1)
					it as IDefinitionTag

					when (it) {
						is TagDefineBitsJPEG2 -> {
							val bitsData = it.bitmapData.cloneToNewByteArray()
							val nativeBitmap = bitsData.openAsync().readBitmap()
							//println(nativeBitmap)
							val bmp = nativeBitmap.toBMP32()
							fbmp = bmp

							if (it is TagDefineBitsJPEG3) {
								val fmaskinfo = it.bitmapAlphaData.cloneToNewFlashByteArray()
								fmaskinfo.uncompress("zlib")
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
							funcompressedData.uncompress("zlib")
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
					val rasterizer = SWFShapeRasterizer(swf, it.shapeBounds.rect)
					it.export(if (debug) LoggerShapeExporter(rasterizer) else rasterizer)
					val symbol = AnSymbolShape(it.characterId, null, rasterizer.bounds, null, rasterizer.path)
					symbols += symbol
					shapesToPopulate += symbol to rasterizer
				}
				is TagDoABC -> {
					classNameToTypes += it.abc.typesInfo.map { it.name.toString() to it }.toMap()
				}
				is TagSymbolClass -> {
					classNameToTagId += it.symbols.filter { it.name != null }.map { it.name!! to it.tagId }.toMap()
				}
				is TagDefineSprite -> {
					parseMovieClip(it.tags, AnSymbolMovieClip(it.characterId, null, findLimits(it.tags)))
				}
				is TagPlaceObject -> {
					val depth = depths[it.depth0]
					if (it.hasCharacter) depth.charId = it.characterId
					if (it.hasName) depth.name = it.instanceName
					//if (it.hasBlendMode) depth.blendMode = it.blendMode
					if (it.hasColorTransform) {
						depth.alpha = it.colorTransform!!.aMult
					}
					if (it.hasMatrix) depth.matrix = it.matrix!!.matrix
					depth.uid = getUid(it.depth0)
				}
				is TagRemoveObject -> {
					depths[it.depth0].reset()
				}
				is TagShowFrame -> {
					for (depth in depths) {
						swfCurrentFrame.depths += depth.toFrameElement()
					}
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

object SwfLoader {
	suspend fun load(views: Views, data: ByteArray, debug: Boolean = false): AnLibrary = SwfLoaderMethod(views, debug).load(data)
}

fun decodeSWFColor(color: Int, alpha: Double = 1.0) = RGBA.pack(color.extract8(16), color.extract8(8), color.extract8(0), (alpha * 255).toInt())

class SWFShapeRasterizer(val swf: SWF, val bounds: Rectangle) : ShapeExporter() {
	//val bmp = Bitmap32(bounds.width.toIntCeil(), bounds.height.toIntCeil())
	val image = NativeImage(Math.max(1, bounds.width.toIntCeil()), Math.max(1, bounds.height.toIntCeil()))
	val path = GraphicsPath()
	var processingFills = false

	val ctx = image.getContext2d().apply {
		translate(-bounds.x, -bounds.y)
	}

	override fun beginShape() {
		//ctx.beginPath()
	}

	override fun endShape() {
		//ctx.closePath()
	}

	override fun beginFills() {
		processingFills = true
		ctx.beginPath()
	}

	override fun endFills() {
		processingFills = false
	}

	override fun beginLines() {
		ctx.beginPath()
	}

	override fun closePath() {
		ctx.closePath()
		if (processingFills) path.close()
	}

	override fun endLines() {
		ctx.stroke()
	}

	override fun beginFill(color: Int, alpha: Double) {
		ctx.fillStyle = Context2d.Color(decodeSWFColor(color, alpha))
	}

	fun GradientSpreadMode.toCtx() = when (this) {
		GradientSpreadMode.PAD -> Context2d.CycleMethod.NO_CYCLE
		GradientSpreadMode.REFLECT -> Context2d.CycleMethod.REFLECT
		GradientSpreadMode.REPEAT -> Context2d.CycleMethod.REPEAT
	}

	override fun beginGradientFill(type: GradientType, colors: List<Int>, alphas: List<Double>, ratios: List<Int>, matrix: Matrix2d, spreadMethod: GradientSpreadMode, interpolationMethod: GradientInterpolationMode, focalPointRatio: Double) {
		//matrix.scale(100.0, 100.0)
		//this.createBox(width / 1638.4, height / 1638.4, rotation, tx + width / 2, ty + height / 2);
		val transform = Matrix2d.Transform().setMatrix(matrix)

		val width = transform.scaleX * 1638.4
		val height = transform.scaleY * 1638.4
		val rotation = transform.rotation
		val x = transform.x - width / 2.0
		val y = transform.y - height / 2.0
		val x0 = x
		val y0 = y
		val x1 = x + width * Math.cos(rotation)
		val y1 = y + height * Math.sin(rotation)
		val aratios = ArrayList(ratios.map { it.toDouble() / 255.0 })
		val acolors = ArrayList(colors.zip(alphas).map { decodeSWFColor(it.first, it.second) })
		when (type) {
			GradientType.LINEAR -> {
				ctx.fillStyle = Context2d.LinearGradient(x0, y0, x1, y1, aratios, acolors, spreadMethod.toCtx())
			}
			GradientType.RADIAL -> {
				val r0 = 0.0
				val r1 = Math.max(width, height)
				ctx.fillStyle = Context2d.RadialGradient(x0, y0, r0, x1, y1, r1, aratios, acolors, spreadMethod.toCtx())
			}
		}
		//ctx.fillStyle = Context2d.Color(decodeSWFColor(color, alpha))
		//super.beginGradientFill(type, colors, alphas, ratios, matrix, spreadMethod, interpolationMethod, focalPointRatio)
	}

	override fun beginBitmapFill(bitmapId: Int, matrix: Matrix2d, repeat: Boolean, smooth: Boolean) {
		val bmp = swf.bitmaps[bitmapId] ?: Bitmap32(1, 1)
		ctx.fillStyle = Context2d.BitmapPaint(bmp, matrix, repeat, smooth)
		//println(matrix)
		//ctx.fillStyle = Context2d.Bitmap()
		//super.beginBitmapFill(bitmapId, matrix, repeat, smooth)
	}

	override fun endFill() {
		ctx.fill()
	}

	override fun lineStyle(thickness: Double, color: Int, alpha: Double, pixelHinting: Boolean, scaleMode: String, startCaps: LineCapsStyle, endCaps: LineCapsStyle, joints: String?, miterLimit: Double) {
		ctx.lineWidth = thickness
		ctx.strokeStyle = Context2d.Color(decodeSWFColor(color, alpha))
		ctx.lineCap = when (startCaps) {
			LineCapsStyle.NO -> Context2d.LineCap.BUTT
			LineCapsStyle.ROUND -> Context2d.LineCap.ROUND
			LineCapsStyle.SQUARE -> Context2d.LineCap.SQUARE
		}
	}

	override fun lineGradientStyle(type: GradientType, colors: List<Int>, alphas: List<Double>, ratios: List<Int>, matrix: Matrix2d, spreadMethod: GradientSpreadMode, interpolationMethod: GradientInterpolationMode, focalPointRatio: Double) {
		super.lineGradientStyle(type, colors, alphas, ratios, matrix, spreadMethod, interpolationMethod, focalPointRatio)
	}

	override fun moveTo(x: Double, y: Double) {
		ctx.moveTo(x, y)
		if (processingFills) path.moveTo(x, y)
	}

	override fun lineTo(x: Double, y: Double) {
		ctx.lineTo(x, y)
		if (processingFills) path.lineTo(x, y)
	}

	override fun curveTo(controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) {
		ctx.quadraticCurveTo(controlX, controlY, anchorX, anchorY)
		if (processingFills) path.quadTo(controlX, controlY, anchorX, anchorY)
	}
}

suspend fun VfsFile.readSWF(views: Views, debug: Boolean = false): AnLibrary = SwfLoader.load(views, this.readAll(), debug = debug)
