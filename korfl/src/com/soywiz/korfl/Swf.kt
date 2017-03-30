package com.soywiz.korfl

import com.codeazur.as3swf.SWF
import com.codeazur.as3swf.data.GradientType
import com.codeazur.as3swf.data.consts.BitmapFormat
import com.codeazur.as3swf.data.consts.GradientInterpolationMode
import com.codeazur.as3swf.data.consts.GradientSpreadMode
import com.codeazur.as3swf.data.consts.LineCapsStyle
import com.codeazur.as3swf.exporters.LoggerShapeExporter
import com.codeazur.as3swf.exporters.ShapeExporter
import com.codeazur.as3swf.tags.*
import com.soywiz.korge.resources.Path
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.texture
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.Bitmap8
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.BGRA
import com.soywiz.korim.color.BGRA_5551
import com.soywiz.korim.color.RGB
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.nativeImageFormatProvider
import com.soywiz.korim.format.showImageAndWait
import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.GraphicsPath
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.extract8
import com.soywiz.korio.util.getu
import com.soywiz.korio.util.toIntCeil
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.math.Matrix2d
import kotlin.collections.set

@AsyncFactoryClass(SwfLibraryFactory::class)
class SwfLibrary(
	val an: AnLibrary
) {

}

class SwfLibraryFactory(
	val path: Path,
	val views: Views
) : AsyncFactory<SwfLibrary> {
	suspend override fun create(): SwfLibrary = SwfLibrary(ResourcesVfs[path.path].readSWF(views))
}

inline val TagPlaceObject.depth0: Int get() = this.depth - 1
inline val TagRemoveObject.depth0: Int get() = this.depth - 1

val SWF.bitmaps by Extra.Property { hashMapOf<Int, Bitmap>() }

private class SwfLoaderMethod(val views: Views, val debug: Boolean) {
	lateinit var swf: SWF
	lateinit var lib: AnLibrary
	val shapesToPopulate = arrayListOf<Pair<AnSymbolShape, SWFShapeRasterizer>>()

	suspend fun load(data: ByteArray): AnLibrary {
		swf = SWF().loadBytes(data)
		lib = AnLibrary(views, swf.frameRate)
		parseMovieClip(swf.tags, AnSymbolMovieClip(0, "MainTimeLine", findLimits(swf.tags)))

		// @TODO: Generate an atalas using BinPacker!
		for ((shape, rasterizer) in shapesToPopulate) {
			if (debug) showImageAndWait(rasterizer.image)
			shape.texture = views.texture(rasterizer.image)
		}

		return lib
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

	fun registerBitmap(charId: Int, bmp: Bitmap, name: String? = null) {
		swf.bitmaps[charId] = bmp
		lib.addSymbol(AnSymbolBitmap(charId, name, bmp))

		//showImageAndWait(bmp)
	}

	suspend fun parseMovieClip(tags: Iterable<ITag>, mc: AnSymbolMovieClip) {
		lib.addSymbol(mc)

		var currentFrame = 0
		val uniqueIds = hashMapOf<Pair<Int, Int>, Int>()

		class DepthInfo {
			var charId: Int = -1
			var name: String? = null

			fun reset() {
				charId = -1
				name = null
			}
		}

		val depths = Array(mc.limits.totalDepths) { DepthInfo() }

		fun getUid(depth: Int): Int {
			val charId = depths[depth].charId
			return uniqueIds.getOrPut(depth to charId) {
				val uid = uniqueIds.size
				mc.uidInfo[uid] = AnSymbolUidDef(charId)
				uid
			}
		}

		for (it in tags) {
			//println("Tag: $it")
			val currentTime = (currentFrame * lib.msPerFrameDouble).toInt()
			when (it) {
				is TagFileAttributes -> {

				}
				is TagSetBackgroundColor -> {
					lib.bgcolor = decodeSWFColor(it.color)
				}
				is TagDefineSceneAndFrameLabelData -> {
					for (fl in it.frameLabels) mc.labels[fl.name] = fl.frameNumber
				}
				is TagDefineBitsJPEG2 -> {
					val bitsData = it.bitmapData.cloneToNewByteArray()
					val bmp = nativeImageFormatProvider.decode(bitsData).toBmp32()

					if (it is TagDefineBitsJPEG3) {
						val fmaskinfo = it.bitmapAlphaData.cloneToNewFlashByteArray()
						fmaskinfo.uncompress("zlib")
						val maskinfo = fmaskinfo.cloneToNewByteArray()
						//val bmpAlpha = nativeImageFormatProvider.decode(maskinfo)
						//showImageAndWait(bmpAlpha)
						for (n in 0 until bmp.area) {
							bmp.data[n] = (bmp.data[n] and 0xFFFFFF) or (maskinfo.getu(n) shl 24)
						}
						//println(maskinfo)
					}

					//val bmp = ImageFormats.read(bitsData)
					//bitsData
					//println(it)
					registerBitmap(it.characterId, bmp, it.name)


					//LocalVfs("c:\\temp\\test.png").write(ImageFormats.encode(bmp, "test.png"))

					//showImageAndWait(bmp)

				}
				is TagDefineBitsLossless -> {
					val isRgba = it.hasAlpha
					val funcompressedData = it.zlibBitmapData.cloneToNewFlashByteArray()
					funcompressedData.uncompress("zlib")
					val uncompressedData = funcompressedData.cloneToNewByteArray()
					var fbmp: Bitmap = Bitmap32(1, 1)
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
					}

					//showImageAndWait(fbmp)

					registerBitmap(it.characterId, fbmp, it.name)
					//println(it)
					//println(uncompressedData)
				}
				is TagDefineShape -> {
					val rasterizer = SWFShapeRasterizer(swf, it.shapeBounds.rect)
					it.export(if (debug) LoggerShapeExporter(rasterizer) else rasterizer)
					val symbol = AnSymbolShape(it.characterId, it.name, rasterizer.bounds, null, rasterizer.path)
					lib.addSymbol(symbol)
					shapesToPopulate += symbol to rasterizer
				}
				is TagDoABC -> {
					println(it.abc)
				}
				is TagDefineSprite -> {
					parseMovieClip(it.tags, AnSymbolMovieClip(it.characterId, it.name, findLimits(it.tags)))
				}
				is TagPlaceObject -> {
					val matrix = if (it.hasMatrix) it.matrix!!.matrix else Matrix2d()
					val depth = depths[it.depth0]
					if (it.hasCharacter) depth.charId = it.characterId
					if (it.hasName) depth.name = it.name
					//if (it.hasColorTransform) depth.colorTransform = it.colorTransform
					mc.timelines[it.depth0].add(currentTime, AnSymbolTimelineFrame(
						uid = getUid(it.depth0),
						transform = Matrix2d.Computed(matrix),
						name = depth.name
					))

					//val frame = mc.frames[currentFrame]
					//if (it.hasCharacter) frame.places += AnSymbolPlace(it.depth0, it.characterId)
					//frame.updates += AnSymbolUpdate(it.depth0, Matrix2d.Computed(matrix))
				}
				is TagRemoveObject -> {
					depths[it.depth0].reset()
					mc.timelines[it.depth0].add(currentTime, AnSymbolTimelineFrame(
						uid = -1,
						transform = Matrix2d.Computed(Matrix2d()),
						name = null
					))
					//mc.frames[currentFrame].removes += AnSymbolRemove(it.depth0)
				}
				is TagShowFrame -> {
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
	val image = NativeImage(bounds.width.toIntCeil(), bounds.height.toIntCeil())
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
