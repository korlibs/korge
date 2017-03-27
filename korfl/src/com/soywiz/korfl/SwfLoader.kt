package com.soywiz.korfl

import com.codeazur.as3swf.SWF
import com.codeazur.as3swf.exporters.ShapeExporter
import com.codeazur.as3swf.tags.*
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.texture
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.geom.Matrix2d
import com.soywiz.korim.geom.Rectangle
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.util.extract8
import com.soywiz.korio.util.toIntCeil
import com.soywiz.korio.vfs.VfsFile

object SwfLoader {
	suspend fun load(views: Views, data: ByteArray): AnLibrary {
		val swf = SWF().loadBytes(data)
		val lib = AnLibrary(views, swf.frameRate)

		val shapesToPopulate = arrayListOf<Pair<AnSymbolShape, SWFShapeRasterizer>>()

		fun findLimits(tags: Iterable<ITag>): AnSymbolLimits {
			var maxDepth = -1
			var totalFrames = 0
			// Find limits
			for (it in tags) {
				when (it) {
					is TagPlaceObject -> {
						maxDepth = Math.max(maxDepth, it.depth)
					}
					is TagShowFrame -> {
						totalFrames++
					}
				}
			}
			return AnSymbolLimits(maxDepth + 1, totalFrames)
		}

		fun parseMovieClip(tags: Iterable<ITag>, mc: AnSymbolMovieClip) {
			lib.addSymbol(mc)

			var currentFrame = 0
			for (it in tags) {
				//println("Tag: $tag")
				when (it) {
					is TagFileAttributes -> {

					}
					is TagSetBackgroundColor -> {
						lib.bgcolor = decodeSWFColor(it.color)
					}
					is TagDefineSceneAndFrameLabelData -> {
						for (fl in it.frameLabels) mc.labels[fl.name] = fl.frameNumber
					}
					is TagDefineShape -> {
						val rasterizer = SWFShapeRasterizer(swf, it.shapeBounds.rect)
						it.export(rasterizer)
						val symbol = AnSymbolShape(it.characterId, it.name, rasterizer.bounds, null)
						lib.addSymbol(symbol)
						shapesToPopulate += symbol to rasterizer
					}
					is TagDefineSprite -> {
						parseMovieClip(it.tags, AnSymbolMovieClip(it.characterId, it.name, findLimits(it.tags)))
					}
					is TagPlaceObject -> {
						val frame = mc.frames[currentFrame]

						val matrix = if (it.hasMatrix) it.matrix!!.matrix else Matrix2d()

						if (it.hasCharacter) {
							frame.places += AnSymbolPlace(it.depth, it.characterId)
						}
						frame.updates += AnSymbolUpdate(it.depth, Matrix2d.Computed(matrix))
					}
					is TagRemoveObject -> {
						val frame = mc.frames[currentFrame]
						frame.removes += AnSymbolRemove(it.depth)
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

		parseMovieClip(swf.tags, AnSymbolMovieClip(0, "MainTimeLine", findLimits(swf.tags)))

		for ((shape, rasterizer) in shapesToPopulate) {
			shape.texture = views.texture(rasterizer.image)
		}

		return lib
	}
}

fun decodeSWFColor(color: Int, alpha: Double = 1.0) = RGBA.pack(color.extract8(16), color.extract8(8), color.extract8(0), (alpha * 255).toInt())

class SWFShapeRasterizer(swf: SWF, val bounds: Rectangle) : ShapeExporter(swf) {
	//val bmp = Bitmap32(bounds.width.toIntCeil(), bounds.height.toIntCeil())
	val image = NativeImage(bounds.width.toIntCeil(), bounds.height.toIntCeil())
	val ctx = image.getContext2d()

	override fun beginShape() {
		ctx.beginPath()
	}

	override fun endShape() {
		ctx.closePath()
	}

	override fun beginFills() {
		super.beginFills()
	}

	override fun endFills() {
		ctx.fill()
	}

	override fun beginLines() {
		super.beginLines()
	}

	override fun endLines() {
		super.endLines()
	}

	override fun beginFill(color: Int, alpha: Double) {
		ctx.fillStyle = Context2d.Color(decodeSWFColor(color, alpha))
	}

	override fun beginGradientFill(type: String, colors: List<Int>, alphas: List<Double>, ratios: List<Int>, matrix: Matrix2d, spreadMethod: String, interpolationMethod: String, focalPointRatio: Double) {
		super.beginGradientFill(type, colors, alphas, ratios, matrix, spreadMethod, interpolationMethod, focalPointRatio)
	}

	override fun beginBitmapFill(bitmapId: Int, matrix: Matrix2d, repeat: Boolean, smooth: Boolean) {
		super.beginBitmapFill(bitmapId, matrix, repeat, smooth)
	}

	override fun endFill() {
		ctx.fill()
	}

	override fun lineStyle(thickness: Double, color: Int, alpha: Double, pixelHinting: Boolean, scaleMode: String, startCaps: String?, endCaps: String?, joints: String?, miterLimit: Double) {
		super.lineStyle(thickness, color, alpha, pixelHinting, scaleMode, startCaps, endCaps, joints, miterLimit)
	}

	override fun lineGradientStyle(type: String, colors: List<Int>, alphas: List<Double>, ratios: List<Int>, matrix: Matrix2d, spreadMethod: String, interpolationMethod: String, focalPointRatio: Double) {
		super.lineGradientStyle(type, colors, alphas, ratios, matrix, spreadMethod, interpolationMethod, focalPointRatio)
	}

	override fun moveTo(x: Double, y: Double) {
		ctx.moveTo(x, y)
	}

	override fun lineTo(x: Double, y: Double) {
		ctx.lineTo(x, y)
	}

	override fun curveTo(controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) {
		ctx.quadraticCurveTo(controlX, controlY, anchorX, anchorY)
	}
}

suspend fun VfsFile.readSWF(views: Views): AnLibrary = SwfLoader.load(views, this.readAll())
