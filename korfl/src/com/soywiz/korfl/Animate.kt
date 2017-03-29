package com.soywiz.korfl

import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.*
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

class AnSymbolShape(id: Int, name: String?, val bounds: Rectangle, var texture: Texture?) : AnSymbol(id, name) {
	override fun create(library: AnLibrary): AnElement = AnShape(library, this)
}

//class AnSymbolPlace(val depth: Int, val charId: Int)
//class AnSymbolUpdate(val depth: Int, val transform: Matrix2d.Computed)
//class AnSymbolRemove(val depth: Int)
//class AnSymbolFrame {
//	val places = arrayListOf<AnSymbolPlace>()
//	val updates = arrayListOf<AnSymbolUpdate>()
//	val removes = arrayListOf<AnSymbolRemove>()
//}

class AnSymbolTimelineFrame(
	val uid: Int,
	val transform: Matrix2d.Computed
)

class AnDepthTimeline(val depth: Int) : Timed<AnSymbolTimelineFrame>() {
}

class AnSymbolLimits(val totalDepths: Int, val totalFrames: Int, val totalUids: Int, val totalTime: Int)

class AnSymbolMovieClip(id: Int, name: String?, val limits: AnSymbolLimits) : AnSymbol(id, name) {
	val labels = hashMapOf<String, Int>()
	//val frames = Array<AnSymbolFrame>(limits.totalFrames) { AnSymbolFrame() } // @TODO: Remove this!
	val timelines = Array<AnDepthTimeline>(limits.totalDepths) { AnDepthTimeline(it) }
	val uidToCharacterId = IntArray(limits.totalUids) { -1 }

	override fun create(library: AnLibrary): AnElement = AnMovieClip(library, this)
}

class AnShape(library: AnLibrary, val shapeSymbol: AnSymbolShape) : AnElement(library, shapeSymbol) {
	init {
		val tex = shapeSymbol.texture
		if (tex != null) this += views.image(tex, shapeSymbol.bounds.x, shapeSymbol.bounds.y)
	}
}

class AnMovieClip(library: AnLibrary, val mcSymbol: AnSymbolMovieClip) : AnElement(library, mcSymbol) {
	//val totalFrames = mcSymbol.frames.size
	val totalDepths = mcSymbol.limits.totalDepths
	val totalUids = mcSymbol.limits.totalUids
	var nextFrame = 0
	val singleFrame = mcSymbol.limits.totalFrames <= 1
	val dummyDepths = Array<View>(totalDepths) { View(views) }
	val viewUids = Array<View>(totalUids) { library.create(mcSymbol.uidToCharacterId[it]) }
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
					if ((left != null) && (right != null) && (left.uid == right.uid)) {
						//println("$currentTime: $index")
						val view = viewUids[left.uid]
						children[depth].replaceWith(view)
						view.setMatrixInterpolated(ratio, left.transform.matrix, right.transform.matrix)
						//view.setComputedTransform(left.transform)
					} else {
						//println("$currentTime: $index")
						val view = if (left != null) viewUids[left.uid] else dummyDepths[depth]
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

	fun createMainTimeLine() = createMovieClip(0)
}
