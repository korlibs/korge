package com.soywiz.korfl

import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.replaceWith
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.vector.GraphicsPath
import com.soywiz.korio.error.invalidOp
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.math.Matrix2d

open class AnElement(val library: AnLibrary, val symbol: AnSymbol) : Container(library.views) {
}

open class AnSymbol(
	val id: Int = 0,
	val name: String? = null
) {
	open fun create(library: AnLibrary): AnElement = TODO()
}

object AnSymbolEmpty : AnSymbol(0, "")

class AnSymbolShape(id: Int, name: String?, val bounds: Rectangle, var texture: Texture?, val path: GraphicsPath? = null) : AnSymbol(id, name) {
	override fun create(library: AnLibrary): AnElement = AnShape(library, this)
}

class AnSymbolBitmap(id: Int, name: String?, val bmp: Bitmap) : AnSymbol(id, name) {
	//override fun create(library: AnLibrary): AnElement = AnShape(library, this)
}

class AnSymbolTimelineFrame(
	val uid: Int,
	val transform: Matrix2d.Computed,
	val name: String?
)

class AnDepthTimeline(val depth: Int) : Timed<AnSymbolTimelineFrame>()

class AnSymbolLimits(val totalDepths: Int, val totalFrames: Int, val totalUids: Int, val totalTime: Int)

class AnSymbolUidDef(val characterId: Int)

class AnSymbolMovieClip(id: Int, name: String?, val limits: AnSymbolLimits) : AnSymbol(id, name) {
	val labels = hashMapOf<String, Int>()
	val timelines = Array<AnDepthTimeline>(limits.totalDepths) { AnDepthTimeline(it) }
	val uidInfo = Array(limits.totalUids) { AnSymbolUidDef(-1) }

	override fun create(library: AnLibrary): AnElement = AnMovieClip(library, this)
}

class AnShape(library: AnLibrary, val shapeSymbol: AnSymbolShape) : AnElement(library, shapeSymbol) {
	val dx = shapeSymbol.bounds.x.toFloat()
	val dy = shapeSymbol.bounds.y.toFloat()
	val tex = shapeSymbol.texture ?: views.dummyTexture
	val smoothing = true

	override fun render(ctx: RenderContext) {
		ctx.batch.addQuad(tex, x = dx, y = dy, m = globalMatrix, filtering = smoothing, col1 = globalCol1)
	}

	override fun hitTest(x: Double, y: Double): View? {
		val sLeft = dx.toDouble()
		val sTop = dy.toDouble()
		val sRight = sLeft + tex.width
		val sBottom = sTop + tex.height

		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) && (shapeSymbol.path?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) ?: true)) this else null
	}
}

class AnMovieClip(library: AnLibrary, val mcSymbol: AnSymbolMovieClip) : AnElement(library, mcSymbol) {
	val totalDepths = mcSymbol.limits.totalDepths
	val totalUids = mcSymbol.limits.totalUids
	var nextFrame = 0
	val singleFrame = mcSymbol.limits.totalFrames <= 1
	val dummyDepths = Array<View>(totalDepths) { View(views) }
	val viewUids = Array<View>(totalUids) { library.create(mcSymbol.uidInfo[it].characterId) }
	var firstUpdate = true

	init {
		for (d in dummyDepths) this += d
		updateInternal(0)
	}

	override fun reset() {
		super.reset()
		println("reset")
		for (view in viewUids) view.reset()
		for (n in children.indices) children[n].replaceWith(dummyDepths[n])
		nextFrame = 0
	}

	private var currentTime = 0

	override fun updateInternal(dtMs: Int) {
		if (firstUpdate || !singleFrame) {
			firstUpdate = false
			currentTime += dtMs
			for (depth in 0 until totalDepths) {
				val timeline = mcSymbol.timelines[depth]
				timeline.findAndHandle(currentTime) { index, left, right, ratio ->
					val view = if (left != null) viewUids[left.uid] else dummyDepths[depth]
					if ((left != null) && (right != null) && (left.uid == right.uid)) {
						//println("$currentTime: $index")
						children[depth].replaceWith(view)
						view.setMatrixInterpolated(ratio, left.transform.matrix, right.transform.matrix)
						view.name = left.name
						//view.setComputedTransform(left.transform)
					} else {
						//println("$currentTime: $index")
						children[depth].replaceWith(view)
						if (left != null) {
							view.setComputedTransform(left.transform)
						}
					}
				}
			}
			if (currentTime >= mcSymbol.limits.totalTime) {
				currentTime -= mcSymbol.limits.totalTime
			}
		}

		super.updateInternal(dtMs)
	}
}

class AnLibrary(val views: Views, val fps: Double) {
	val msPerFrameDouble: Double = (1000 / fps)
	val msPerFrame: Int = msPerFrameDouble.toInt()
	var bgcolor: Int = 0xFFFFFFFF.toInt()
	val symbolsById = arrayListOf<AnSymbol>()
	val symbolsByName = hashMapOf<String, AnSymbol>()

	fun addSymbol(symbol: AnSymbol) {
		while (symbolsById.size <= symbol.id) symbolsById += AnSymbolEmpty
		symbolsById[symbol.id] = symbol
		if (symbol.name != null) symbolsByName[symbol.name] = symbol
	}

	fun create(id: Int) = symbolsById[id].create(this)
	fun createShape(id: Int) = create(id) as AnShape
	fun createMovieClip(id: Int) = create(id) as AnMovieClip

	fun create(name: String) = symbolsByName[name]?.create(this) ?: invalidOp("Can't find symbol with name '$name'")
	fun createShape(name: String) = create(name) as AnShape
	fun createMovieClip(name: String) = create(name) as AnMovieClip

	fun getBitmap(id: Int) = (symbolsById[id] as AnSymbolBitmap).bmp
	fun getBitmap(name: String) = (symbolsByName[name] as AnSymbolBitmap).bmp

	fun createMainTimeLine() = createMovieClip(0)
}
