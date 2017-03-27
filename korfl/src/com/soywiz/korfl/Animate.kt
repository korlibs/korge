package com.soywiz.korfl

import com.soywiz.korge.render.Texture
import com.soywiz.korge.view.*
import com.soywiz.korim.geom.Matrix2d
import com.soywiz.korim.geom.Rectangle
import com.soywiz.korio.error.invalidOp

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

class AnSymbolPlace(val depth: Int, val charId: Int) {
}

class AnSymbolUpdate(val depth: Int, val transform: Matrix2d.Computed) {
}

class AnSymbolRemove(val depth: Int) {
}

class AnSymbolFrame() {
	val places = arrayListOf<AnSymbolPlace>()
	val updates = arrayListOf<AnSymbolUpdate>()
	val removes = arrayListOf<AnSymbolRemove>()
}

class AnSymbolLimits(val maxDepths: Int, val totalFrames: Int)

class AnSymbolMovieClip(id: Int, name: String?, val limits: AnSymbolLimits) : AnSymbol(id, name) {
	val labels = hashMapOf<String, Int>()
	val frames = Array<AnSymbolFrame>(limits.totalFrames) { AnSymbolFrame() }

	override fun create(library: AnLibrary): AnElement = AnMovieClip(library, this)
}

class AnShape(library: AnLibrary, val shapeSymbol: AnSymbolShape) : AnElement(library, shapeSymbol) {
	init {
		val tex = shapeSymbol.texture
		if (tex != null) this += views.image(tex, shapeSymbol.bounds.x, shapeSymbol.bounds.y)
	}
}

class AnMovieClip(library: AnLibrary, val mcSymbol: AnSymbolMovieClip) : AnElement(library, mcSymbol) {
	val totalFrames = mcSymbol.frames.size
	val maxDepths = mcSymbol.limits.maxDepths
	val dummyDepths = Array<View>(maxDepths) { View(views) }
	var nextFrame = 0
	val singleFrame = mcSymbol.frames.size <= 1

	init {
		for (d in dummyDepths) this += d
		gotoFrame(0)
	}

	private fun reset() {
		println("reset")
		for (n in children.indices) children[n].replaceWith(dummyDepths[n])
		nextFrame = 0
	}

	fun eval(frame: AnSymbolFrame) {
		for (r in frame.removes) children[r.depth] = dummyDepths[r.depth]
		for (r in frame.places) {
			children[r.depth].replaceWith(library.create(r.charId))
		}
		for (r in frame.updates) {
			val view = children[r.depth]
			view.setComputedTransform(r.transform)
		}
	}

	fun gotoFrame(index: Int) {
		val ni = index % (totalFrames + 1)
		if (ni < nextFrame) reset()
		if (nextFrame >= totalFrames) return
		while (nextFrame <= ni) {
			val frame = mcSymbol.frames[nextFrame]
			eval(frame)
			nextFrame++
		}
	}

	fun step() {
		gotoFrame(nextFrame)
	}

	private var pendingTime = 0

	override fun updateInternal(dtMs: Int) {
		if (!singleFrame) {
			pendingTime += dtMs
			while (pendingTime >= library.msPerFrame) {
				step()
				pendingTime -= library.msPerFrame
			}
		}
		super.updateInternal(dtMs)
	}
}

class AnLibrary(val views: Views, val fps: Double) {
	val msPerFrame: Int = (1000 / fps).toInt()
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
