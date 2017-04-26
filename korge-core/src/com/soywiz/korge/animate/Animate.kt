package com.soywiz.korge.animate

import com.soywiz.korag.AG
import com.soywiz.korau.format.play
import com.soywiz.korge.html.Html
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.view.*
import com.soywiz.korio.async.spawn
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.redirect
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.geom.Rectangle
import java.util.*

interface AnElement {
	val library: AnLibrary
	val symbol: AnSymbol
}

abstract class AnBaseShape(override final val library: AnLibrary, override final val symbol: AnSymbolBaseShape) : View(library.views), AnElement {
	var ninePatch: Rectangle? = null

	abstract val dx: Float
	abstract val dy: Float
	abstract val tex: Texture
	abstract val texScale: Double
	abstract val texWidth: Float
	abstract val texHeight: Float
	abstract val smoothing: Boolean

	val posCuts = arrayOf(Point2d(0.0, 0.0), Point2d(0.25, 0.25), Point2d(0.75, 0.75), Point2d(1.0, 1.0))
	val texCuts = arrayOf(Point2d(0.0, 0.0), Point2d(0.25, 0.25), Point2d(0.75, 0.75), Point2d(1.0, 1.0))

	override fun render(ctx: RenderContext, m: Matrix2d) {
		//println("%08X".format(globalColor))
		//println("$id: " + globalColorTransform + " : " + colorTransform + " : " + parent?.colorTransform)
		//println(ninePatch)

		if (ninePatch != null) {
			val np = ninePatch!!
			val lm = parent!!.localMatrix

			val npLeft = np.left - dx
			val npTop = np.top - dy

			val npRight = np.right - dx
			val npBottom = np.bottom - dy

			val ascaleX = lm.a
			val ascaleY = lm.d

			posCuts[1].setTo(((npLeft) / texWidth) / ascaleX, ((npTop) / texHeight) / ascaleY)
			posCuts[2].setTo(1.0 - ((texWidth - npRight) / texWidth) / ascaleX, 1.0 - ((texHeight - npBottom) / texHeight) / ascaleY)
			texCuts[1].setTo((npLeft / texWidth), (npTop / texHeight))
			texCuts[2].setTo((npRight / texWidth), (npBottom / texWidth))

			ctx.batch.addNinePatch(tex, x = dx, y = dy, width = texWidth, height = texHeight, posCuts = posCuts, texCuts = texCuts, m = m, filtering = smoothing, colMul = globalColorMul, colAdd = globalColorAdd)
		} else {
			ctx.batch.addQuad(tex, x = dx, y = dy, width = texWidth, height = texHeight, m = m, filtering = smoothing, colMul = globalColorMul, colAdd = globalColorAdd)
		}
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

class AnShape(library: AnLibrary, symbol: AnSymbolShape) : AnBaseShape(library, symbol), AnElement {
	override val dx = symbol.bounds.x.toFloat()
	override val dy = symbol.bounds.y.toFloat()
	override val tex = symbol.textureWithBitmap?.texture ?: views.transparentTexture
	override val texScale = symbol.textureWithBitmap?.scale ?: 1.0
	override val texWidth = (tex.width / texScale).toFloat()
	override val texHeight = (tex.height / texScale).toFloat()
	override val smoothing = true
}

class AnMorphShape(library: AnLibrary, val morphSymbol: AnSymbolMorphShape) : AnBaseShape(library, morphSymbol), AnElement {
	private val timedResult = Timed.Result<TextureWithBitmapSlice>()

	var texWBS: TextureWithBitmapSlice? = null
	override var dx: Float = 0f
	override var dy: Float = 0f
	override var tex: Texture = views.transparentTexture
	override var texScale = 1.0
	override var texWidth = 0f
	override var texHeight = 0f
	override var smoothing = true

	private fun updatedRatio() {
		val result = morphSymbol.texturesWithBitmap.find((ratio * 1000).toInt(), timedResult)
		texWBS = result.left ?: result.right

		dx = texWBS?.bounds?.x?.toFloat() ?: 0f
		dy = texWBS?.bounds?.y?.toFloat() ?: 0f
		tex = texWBS?.texture ?: views.transparentTexture
		texScale = texWBS?.scale ?: 1.0
		texWidth = (tex.width / texScale).toFloat()
		texHeight = (tex.height / texScale).toFloat()
		smoothing = true
	}

	override var ratio = 0.0
		set(value) {
			field = value
			updatedRatio()
		}

	init {
		updatedRatio()
	}

	override fun render(ctx: RenderContext, m: Matrix2d) {
		//println("$dx, $dy")
		//println(texWBS?.bounds)
		super.render(ctx, m)
	}
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


class TimelineRunner(val view: AnMovieClip, val symbol: AnSymbolMovieClip) {
	val library: AnLibrary = view.library
	var running = true
	var currentTime = 0
	var currentStateName: String? = null
	var currentSubtimeline: AnSymbolMovieClipSubTimeline? = null
	val currentStateTotalTime: Int get() = currentSubtimeline?.totalTime ?: 0

	init {
		gotoAndPlay("default")
	}

	fun getStateTime(name: String): Int {
		val substate = symbol.states[name] ?: return 0
		return substate.subTimeline.totalTime - substate.startTime
	}

	fun gotoAndRunning(running: Boolean, name: String, time: Int = 0) {
		val substate = symbol.states[name]
		this.currentStateName = substate?.name
		this.currentSubtimeline = substate?.subTimeline
		this.currentTime = (substate?.startTime ?: 0) + time
		this.running = running
		update(0)
		//println("currentStateName: $currentStateName, running=$running, currentTime=$currentTime, time=$time, totalTime=$currentStateTotalTime")
	}

	fun gotoAndPlay(name: String, time: Int = 0) = gotoAndRunning(true, name, time)
	fun gotoAndStop(name: String, time: Int = 0) = gotoAndRunning(false, name, time)

	fun update(time: Int) {
		//println("Update[1]: $currentTime")
		if (!running) return
		//println("Update[2]: $currentTime")
		if (currentSubtimeline == null) return
		//println("Update[3]: $currentTime")
		val cs = currentSubtimeline!!
		eval(currentTime, Math.min(currentStateTotalTime, currentTime + time))
		currentTime += time
		//println("Update[4]: $currentTime : delta=$time")
		if (currentTime >= currentStateTotalTime) {
			//println("currentTime >= currentStateTotalTime :: ${currentTime} >= ${currentStateTotalTime}")
			val accumulatedTime = currentTime - currentStateTotalTime
			val nextState = cs.nextState

			if (nextState == null) {
				running = false
			} else {
				//gotoAndRunning(cs.nextStatePlay, nextState, accumulatedTime)
				gotoAndRunning(cs.nextStatePlay, nextState, 0)
				eval(currentTime, currentTime + accumulatedTime)
				currentTime += accumulatedTime
			}

		}
	}

	private val tempRangeResult = Timed.RangeResult()

	private fun eval(prev: Int, current: Int) {
		if (prev >= current) return
		val actionsTimeline = this.currentSubtimeline?.actions ?: return
		val result = actionsTimeline.getRangeIndices(prev, current - 1, out = tempRangeResult)

		val startIndex = result.startIndex
		val endIndex = result.endIndex

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
}

class AnMovieClip(override val library: AnLibrary, override val symbol: AnSymbolMovieClip) : Container(library.views), AnElement {
	private val tempTimedResult = Timed.Result<AnSymbolTimelineFrame>()
	val totalDepths = symbol.limits.totalDepths
	val totalUids = symbol.limits.totalUids
	val dummyDepths = Array<View>(totalDepths) { View(views) }
	val maskPushDepths = IntArray(totalDepths) { -1 }
	val maskPopDepths = BooleanArray(totalDepths) { false }
	val viewUids = Array<View>(totalUids) {
		val info = symbol.uidInfo[it]
		val view = library.create(info.characterId) as View
		view.addProps(info.extraProps)
		view
	}
	var firstUpdate = true
	var smoothing = library.defaultSmoothing
	val singleFrame = symbol.limits.totalFrames <= 1
	val unsortedChildren = ArrayList(dummyDepths.toList())
	val timelineRunner = TimelineRunner(this, symbol)

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
			val timelines = timelineRunner.currentSubtimeline?.timelines ?: continue
			val timeline = timelines[depth]
			if (smoothing) {
				val (index, left, right, ratio) = timeline.find(timelineRunner.currentTime, out = tempTimedResult)
				if (left != null) maskPushDepths[left.depth] = left.clipDepth

				val view = if (left != null && left.uid >= 0) viewUids[left.uid] else dummyDepths[depth]
				replaceDepth(depth, view)
				if ((left != null) && (right != null) && (left.uid == right.uid)) {
					//println("$currentTime: $index")
					AnSymbolTimelineFrame.setToViewInterpolated(view, left, right, ratio)
				} else {
					//println("$currentTime: $index")
					left?.setToView(view)
					//println(left.colorTransform)
				}
				if (symbol.ninePatch != null && view is AnBaseShape) view.ninePatch = symbol.ninePatch
			} else {
				val (index, left) = timeline.findWithoutInterpolation(timelineRunner.currentTime, out = tempTimedResult)
				if (left != null) maskPushDepths[left.depth] = left.clipDepth
				val view = if (left != null) viewUids[left.uid] else dummyDepths[depth]
				//println("$currentTime: $index")
				replaceDepth(depth, view)
				left?.setToView(view)
				if (symbol.ninePatch != null && view is AnBaseShape) view.ninePatch = symbol.ninePatch
			}
		}
	}

	val stateNames get() = symbol.states.map { it.value.name }

	/**
	 * Changes the state and plays it
	 */
	fun play(name: String) {
		timelineRunner.gotoAndPlay(name)
		update()
	}

	/**
	 * Changes the state, seeks a point in between the state and pauses it. Useful for interpolate between points, eg. progressBars.
	 */
	fun seekStill(name: String, ratio: Double = 0.0) {
		val totalTime = timelineRunner.getStateTime(name)
		timelineRunner.gotoAndStop(name, (totalTime * ratio).toInt())
		//println("seekStill($name,$ratio) : $currentTime,$running,$currentState")
		update()
	}

	override fun updateInternal(dtMs: Int) {
		if (timelineRunner.running && (firstUpdate || !singleFrame)) {
			firstUpdate = false
			timelineRunner.update(dtMs * 1000)
			update()
		}

		super.updateInternal(dtMs)
	}
}

fun View?.play(name: String) = run { (this as? AnMovieClip?)?.play(name) }
val View?.playingName: String? get() = (this as? AnMovieClip?)?.timelineRunner?.currentStateName
fun View?.seekStill(name: String, ratio: Double = 0.0) = run { (this as? AnMovieClip?)?.seekStill(name, ratio) }
