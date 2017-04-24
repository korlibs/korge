package com.soywiz.korge.animate

import com.soywiz.korag.AG
import com.soywiz.korau.format.play
import com.soywiz.korge.html.Html
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.*
import com.soywiz.korio.async.spawn
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.redirect
import com.soywiz.korio.util.redirectField
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Rectangle
import java.util.*

interface AnElement {
	val library: AnLibrary
	val symbol: AnSymbol
}

class AnShape(override val library: AnLibrary, override val symbol: AnSymbolShape) : View(library.views), AnElement {
	val dx = symbol.bounds.x.toFloat()
	val dy = symbol.bounds.y.toFloat()
	val tex = symbol.textureWithBitmap?.texture ?: views.transparentTexture
	val texScale = symbol.textureWithBitmap?.scale ?: 1.0
	val texWidth = (tex.width / texScale).toFloat()
	val texHeight = (tex.height / texScale).toFloat()
	val smoothing = true

	override fun render(ctx: RenderContext, m: Matrix2d) {
		ctx.batch.addQuad(tex, x = dx, y = dy, width = texWidth, height = texHeight, m = m, filtering = smoothing, col1 = globalColor)
	}

	override fun hitTestInternal(x: Double, y: Double): View? {
		val sLeft = dx.toDouble()
		val sTop = dy.toDouble()
		val sRight = sLeft + tex.width
		val sBottom = sTop + tex.height
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) && (symbol.path?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) ?: true)) this else null
	}

	override fun getLocalBounds(out: Rectangle) {
		out.setTo(dx, dy, tex.width, tex.height)
	}

	override fun updateInternal(dtMs: Int) = Unit
}

class AnEmptyView(override val library: AnLibrary, override val symbol: AnSymbolEmpty = AnSymbolEmpty) : View(library.views), AnElement {

}

class AnTextField(override val library: AnLibrary, override val symbol: AnTextFieldSymbol) : Container(library.views), AnElement, IText, IHtml {
	private val textField = views.text("", 16.0).apply {
		textBounds.copyFrom(symbol.bounds)
		html = symbol.initialHtml
		relayout()
	}

	init {
		this += textField
	}

	var format: Html.Format by textField::format.redirect()
	override var text: String get() = textField.text; set(value) = run { textField.text = value }
	override var html: String get() = textField.html; set(value) = run { textField.html = value }
}

//class PopMaskView(views: Views) : View(views)

var RenderContext.stencilIndex by Extra.Property { 0 }

class AnMovieClip(override val library: AnLibrary, override val symbol: AnSymbolMovieClip) : Container(library.views), AnElement {
	val totalDepths = symbol.limits.totalDepths
	val totalUids = symbol.limits.totalUids
	val dummyDepths = Array<View>(totalDepths) { View(views) }
	val maskPushDepths = IntArray(totalDepths) { -1 }
	val maskPopDepths = BooleanArray(totalDepths) { false }
	//val popMasks = Array<View>(totalDepths) { PopMaskView(views) } // @TODO: Just create what required
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

	companion object {
		class RenderState(val stencil: AG.StencilState, val colorMask: AG.ColorMaskState) {
			fun set(ctx: RenderContext, referenceValue: Int) {
				ctx.flush()
				//println(referenceValue)
				stencil.referenceValue = referenceValue
				ctx.batch.stencil = stencil
				ctx.batch.colorMask = colorMask
			}
		}

		// @TODO: Move this to a class handling masks
		val STATE_NONE = RenderState(
			AG.StencilState(enabled = false),
			AG.ColorMaskState(true, true, true, true)
		)
		val STATE_SHAPE = RenderState(
			AG.StencilState(
				enabled = true,
				compareMode = AG.CompareMode.ALWAYS,
				actionOnBothPass = AG.StencilOp.SET,
				actionOnDepthFail = AG.StencilOp.SET,
				actionOnDepthPassStencilFail = AG.StencilOp.SET,
				referenceValue = 0,
				readMask = 0xFF,
				writeMask = 0xFF
			),
			AG.ColorMaskState(false, false, false, false)
		)
		val STATE_CONTENT = RenderState(
			AG.StencilState(
				enabled = true,
				compareMode = AG.CompareMode.EQUAL,
				actionOnBothPass = AG.StencilOp.KEEP,
				actionOnDepthFail = AG.StencilOp.KEEP,
				actionOnDepthPassStencilFail = AG.StencilOp.KEEP,
				referenceValue = 0,
				readMask = 0xFF,
				writeMask = 0x00
			),
			AG.ColorMaskState(true, true, true, true)
		)

		//val STATE_NONE = RenderState(AG.StencilState(enabled = false), AG.ColorMaskState(true, true, true, true))
		//val STATE_SHAPE = STATE_NONE
		//val STATE_CONTENT = STATE_NONE
	}

	private val tempMatrix = Matrix2d()
	override fun render(ctx: RenderContext, m: Matrix2d) {
		Arrays.fill(maskPopDepths, false)

		val isGlobal = (m === globalMatrix)
		var usedStencil = false

		//println("::::")
		for ((depth, child) in children.toList().withIndex()) {
			val maskDepth = maskPushDepths[depth]

			// Push Mask
			if (maskDepth >= 0) {
				maskPopDepths[maskDepth] = true
				ctx.stencilIndex++
				usedStencil = true
				STATE_SHAPE.set(ctx, ctx.stencilIndex)
				//println(" shape")
			}

			//println("$depth:")
			//println(ctx.batch.colorMask)
			if (isGlobal) {
				child.render(ctx, child.globalMatrix)
			} else {
				tempMatrix.multiply(child.localMatrix, m)
				child.render(ctx, tempMatrix)
			}

			// Mask content
			if (maskDepth >= 0) {
				//println(" content")
				STATE_CONTENT.set(ctx, ctx.stencilIndex)
			}

			// Pop Mask
			if (maskPopDepths[depth]) {
				//println(" none")
				STATE_NONE.set(ctx, referenceValue = 0)
				ctx.stencilIndex--
			}

			//println("  " + ctx.batch.colorMask)

		}

		// Reset stencil
		if (usedStencil && ctx.stencilIndex <= 0) {
			ctx.stencilIndex = 0
			ctx.ag.clear(clearColor = false, clearDepth = false, clearStencil = true, stencil = 0)
		}
	}

	private fun update() {
		for (depth in 0 until totalDepths) {
			val timelines = currentState?.state?.timelines ?: continue
			val timeline = timelines[depth]
			if (smoothing) {
				timeline.findAndHandle(currentTime) { index, left, right, ratio ->
					if (left != null) {
						maskPushDepths[left.depth] = left.clipDepth
					}

					//if (left != null && left.popMask) {
					//	//println("$depth: $left")
					//	replaceDepth(depth, popMasks[depth])
					//} else {
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
					//}
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

	val stateNames get() = symbol.states.map { it.value.name }

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

val View?.playingName: String get() {
	return (this as? AnMovieClip?)?.currentState?.name ?: ""
}

fun View?.seekStill(name: String, ratio: Double = 0.0) {
	(this as? AnMovieClip?)?.seekStill(name, ratio)
}
