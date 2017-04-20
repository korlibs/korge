package com.soywiz.korge.animate

import com.soywiz.korau.format.play
import com.soywiz.korge.html.Html
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.*
import com.soywiz.korio.async.spawn
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Rectangle

interface AnElement {
	val library: AnLibrary
	val symbol: AnSymbol
}

class AnShape(override val library: AnLibrary, override val symbol: AnSymbolShape) : View(library.views), AnElement {
	val dx = symbol.bounds.x.toFloat()
	val dy = symbol.bounds.y.toFloat()
	val tex = symbol.textureWithBitmap?.texture ?: views.transparentTexture
	val smoothing = true

	override fun render(ctx: RenderContext, m: Matrix2d) {
		ctx.batch.addQuad(tex, x = dx, y = dy, m = m, filtering = smoothing, col1 = globalCol1)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		val sLeft = dx.toDouble()
		val sTop = dy.toDouble()
		val sRight = sLeft + tex.width
		val sBottom = sTop + tex.height
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) && (symbol.path?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) ?: true)) this else null
	}

	override fun getLocalBounds(out: Rectangle) {
		out.setTo(dx, dy,  tex.width, tex.height)
	}

	override fun updateInternal(dtMs: Int) = Unit
}

class AnTextField(override val library: AnLibrary, override val symbol: AnTextFieldSymbol) : View(library.views), AnElement, IText, IHtml {
	private val textField = views.text(views.defaultFont, "", 16.0).apply {
		textBounds.copyFrom(symbol.bounds)
		html = symbol.initialHtml
		relayout()
	}

	override fun render(ctx: RenderContext, m: Matrix2d) {
		textField.render(ctx, m)
	}

	override fun getLocalBounds(out: Rectangle) {
		textField.getLocalBounds(out)
	}

	override var text: String get() = textField.text; set(value) = run { textField.text = value }
	override var html: String get() = textField.html; set(value) = run { textField.html = value }
}

class AnMovieClip(override val library: AnLibrary, override val symbol: AnSymbolMovieClip) : Container(library.views), AnElement {
	val totalDepths = symbol.limits.totalDepths
	val totalUids = symbol.limits.totalUids
	val dummyDepths = Array<View>(totalDepths) { View(views) }
	val viewUids = Array<View>(totalUids) {
		val info = symbol.uidInfo[it]
		val view = library.create(info.characterId) as View
		view.addProps(info.extraProps)
		view
	}
	var running = true
	var firstUpdate = true
	var smoothing = library.defaultSmoothing
	var currentState: AnSymbolMovieClipStateWithStartTime? = symbol.states["default"]
	private var currentTime = 0
	val singleFrame = symbol.limits.totalFrames <= 1
	val currentStateStartTime: Int get() = currentState?.startTime ?: 0
	val currentStateLoopTime: Int get() = currentState?.state?.loopStartTime ?: 0
	val currentStateTotalTime: Int get() = currentState?.state?.totalTime ?: 0
	val currentStateTotalTimeMinusStart: Int get() = currentStateTotalTime - currentStateStartTime
	val unsortedChildren = ArrayList(dummyDepths.toList())

	fun currentStateCalcEffectiveTime(time: Int) = currentState?.state?.calcEffectiveTime(time) ?: time

	init {
		for (d in dummyDepths) this += d
		updateInternal(0)
	}

	private fun replaceDepth(depth: Int, view: View) {
		unsortedChildren[depth].replaceWith(view)
		unsortedChildren[depth] = view
	}

	override fun reset() {
		super.reset()
		for (view in viewUids) view.reset()
		for (n in unsortedChildren.indices) {
			replaceDepth(n, dummyDepths[n])
		}
	}

	private fun update() {
		for (depth in 0 until totalDepths) {
			val timelines = currentState?.state?.timelines ?: continue
			val timeline = timelines[depth]
			if (smoothing) {
				timeline.findAndHandle(currentTime) { index, left, right, ratio ->
					val view = if (left != null && left.uid >= 0) viewUids[left.uid] else dummyDepths[depth]
					if ((left != null) && (right != null) && (left.uid == right.uid)) {
						//println("$currentTime: $index")
						replaceDepth(depth, view)
						view.setMatrixInterpolated(ratio, left.transform.matrix, right.transform.matrix)
						//view.setComputedTransform(left.transform)
					} else {
						//println("$currentTime: $index")
						replaceDepth(depth, view)
						if (left != null) {
							view.setComputedTransform(left.transform)
						}
					}
					if (left != null) {
						view.name = left.name
						view.alpha = left.alpha
					}
				}
			} else {
				timeline.findAndHandleWithoutInterpolation(currentTime) { index, left ->
					val view = if (left != null) viewUids[left.uid] else dummyDepths[depth]
					//println("$currentTime: $index")
					replaceDepth(depth, view)
					if (left != null) {
						view.setComputedTransform(left.transform)
						view.name = left.name
						view.alpha = left.alpha
					}
				}
			}
		}
	}

	/**
	 * Changes the state and plays it
	 */
	fun play(name: String) {
		currentState = symbol.states[name]
		currentTime = currentStateStartTime
		running = true
		update()
	}

	/**
	 * Changes the state, seeks a point in between the state and pauses it. Useful for interpolate between points, eg. progressBars.
	 */
	fun seekStill(name: String, ratio: Double = 0.0) {
		currentState = symbol.states[name]
		currentTime = currentStateStartTime + (currentStateTotalTimeMinusStart * ratio).toInt()
		running = false
		//println("seekStill($name,$ratio) : $currentTime,$running,$currentState")
		update()
	}

	private fun eval(prev: Int, current: Int) {
		val actionsTimeline = this.currentState?.state?.actions ?: return
		var startIndex = 0
		var endIndex = 0
		actionsTimeline.getRangeIndices(prev, current - 1) { start, end -> startIndex = start; endIndex = end }
		execution@ for (n in startIndex..endIndex) {
			val actions = actionsTimeline.objects[n]
			for (action in actions.actions) {
				when (action) {
					is AnPlaySoundAction -> {
						spawn {
							val data = (library.symbolsById[action.soundId] as AnSymbolSound?)?.data
							data?.play()
						}
						//println("play sound!")
					}
				}
			}
		}
	}

	override fun updateInternal(dtMs: Int) {
		if (running && (firstUpdate || !singleFrame)) {
			firstUpdate = false
			val previousTime = currentTime
			currentTime = currentStateCalcEffectiveTime(currentTime + dtMs * 1000)
			eval(Math.min(currentTime, previousTime), currentTime)
			update()
		}

		super.updateInternal(dtMs)
	}
}

fun View?.play(name: String) {
	(this as? AnMovieClip?)?.play(name)
}

fun View?.seekStill(name: String, ratio: Double = 0.0) {
	(this as? AnMovieClip?)?.seekStill(name, ratio)
}
