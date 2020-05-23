package com.soywiz.korge.animate

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korge.html.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*
import kotlin.math.*

interface AnElement {
	val library: AnLibrary
	val symbol: AnSymbol
}

fun AnElement.createDuplicated() = symbol.create(library)
fun AnElement.createDuplicatedView() = symbol.create(library) as View

abstract class AnBaseShape(final override val library: AnLibrary, final override val symbol: AnSymbolBaseShape) :
	View(), AnElement {
	var ninePatch: Rectangle? = null

	abstract val dx: Float
	abstract val dy: Float
	abstract val tex: BmpSlice
	abstract val texScale: Double
	abstract val texWidth: Float
	abstract val texHeight: Float
	abstract val smoothing: Boolean

	val posCuts = arrayOf(Point(0.0, 0.0), Point(0.25, 0.25), Point(0.75, 0.75), Point(1.0, 1.0))
	val texCuts = arrayOf(Point(0.0, 0.0), Point(0.25, 0.25), Point(0.75, 0.75), Point(1.0, 1.0))

	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
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
			posCuts[2].setTo(
				1.0 - ((texWidth - npRight) / texWidth) / ascaleX,
				1.0 - ((texHeight - npBottom) / texHeight) / ascaleY
			)
			texCuts[1].setTo((npLeft / texWidth), (npTop / texHeight))
			texCuts[2].setTo((npRight / texWidth), (npBottom / texWidth))

			ctx.batch.drawNinePatch(
				ctx.getTex(tex),
				x = dx,
				y = dy,
				width = texWidth,
				height = texHeight,
				posCuts = posCuts,
				texCuts = texCuts,
				m = this.globalMatrix,
				filtering = smoothing,
				colorMul = renderColorMul,
				colorAdd = renderColorAdd,
				blendFactors = renderBlendMode.factors
			)
		} else {
			ctx.batch.drawQuad(
				ctx.getTex(tex),
				x = dx,
				y = dy,
				width = texWidth,
				height = texHeight,
				m = globalMatrix,
				filtering = smoothing,
				colorMul = renderColorMul,
				colorAdd = renderColorAdd,
				blendFactors = renderBlendMode.factors
			)
		}
	}

	override fun hitTest(x: Double, y: Double): View? {
		val sLeft = dx.toDouble()
		val sTop = dy.toDouble()
		val sRight = sLeft + texWidth
		val sBottom = sTop + texHeight
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) &&
			(symbol.path?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) != false)
		) this else null
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(dx, dy, texWidth, texHeight)
	}

	override fun toString(): String = super.toString() + ":symbol=" + symbol

	override fun createInstance(): View = symbol.create(library) as View
}

class AnShape(library: AnLibrary, val shapeSymbol: AnSymbolShape) : AnBaseShape(library, shapeSymbol), AnElement {
	override val dx = shapeSymbol.bounds.x.toFloat()
	override val dy = shapeSymbol.bounds.y.toFloat()
	override val tex = shapeSymbol.textureWithBitmap?.texture ?: Bitmaps.transparent
	override val texScale = shapeSymbol.textureWithBitmap?.scale ?: 1.0
	override val texWidth = (tex.width / texScale).toFloat()
	override val texHeight = (tex.height / texScale).toFloat()
	override val smoothing = true
}

class AnMorphShape(library: AnLibrary, val morphSymbol: AnSymbolMorphShape) : AnBaseShape(library, morphSymbol),
	AnElement {
	private val timedResult = Timed.Result<TextureWithBitmapSlice>()

	var texWBS: TextureWithBitmapSlice? = null
	override var dx: Float = 0f
	override var dy: Float = 0f
	override var tex: BmpSlice = Bitmaps.transparent
	override var texScale = 1.0
	override var texWidth = 0f
	override var texHeight = 0f
	override var smoothing = true

	private fun updatedRatio() {
		val result = morphSymbol.texturesWithBitmap.find((ratio * 1000).toInt(), timedResult)
		texWBS = result.left ?: result.right

		dx = texWBS?.bounds?.x?.toFloat() ?: 0f
		dy = texWBS?.bounds?.y?.toFloat() ?: 0f
		tex = texWBS?.texture ?: Bitmaps.transparent
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

	override fun createInstance(): View = AnMorphShape(library, morphSymbol)

	override fun copyPropsFrom(source: View) {
		val src = (source as AnMorphShape)
		this.dx = src.dx
		this.dy = src.dy
		this.tex = src.tex
		this.texScale = src.texScale
		this.texWidth = src.texWidth
		this.texHeight = src.texHeight
		this.smoothing = src.smoothing
	}
}

class AnEmptyView(override val library: AnLibrary, override val symbol: AnSymbolEmpty = AnSymbolEmpty) : DummyView(), AnElement {
	override fun createInstance(): View = symbol.create(library) as View
}

class AnTextField(override val library: AnLibrary, override val symbol: AnTextFieldSymbol) : Container(),
	AnElement, IText, IHtml {
	private val textField = Text("", 16.0).apply {
		textBounds.copyFrom(symbol.bounds)
		html = symbol.initialHtml
		relayout()
	}

	init {
		this += textField
	}

	var format: Html.Format by textField::format.redirected()
	override var text: String by textField::text.redirected()
	override var html: String by textField::html.redirected()

	override fun createInstance(): View = symbol.create(library) as View
}

//class PopMaskView(views: Views) : View(views)

var RenderContext.stencilIndex by Extra.Property { 0 }


class TimelineRunner(val view: AnMovieClip, val symbol: AnSymbolMovieClip) {
	//var firstUpdateSingleFrame = false
	val library: AnLibrary = view.library
	val views = library.views
	var currentTime = 0
	var currentStateName: String? = null
	var currentSubtimeline: AnSymbolMovieClipSubTimeline? = null
	val currentStateTotalTime: Int get() = currentSubtimeline?.totalTime ?: 0
	val onStop = Signal<Unit>()
	val onChangeState = Signal<String>()
	val onEvent = Signal<String>()

	var running = true
		private set(value) {
			field = value
			if (!value) {
				onStop(Unit)
			}
		}

	init {
		gotoAndPlay("default")
	}

	fun getStateTime(name: String): Int {
		val substate = symbol.states[name] ?: return 0
		return substate.subTimeline.totalTime - substate.startTime
	}

	fun gotoAndRunning(running: Boolean, name: String, time: Int = 0) {
		val substate = symbol.states[name]
		if (substate != null) {
			this.currentStateName = substate.name
			this.currentSubtimeline = substate.subTimeline
			this.currentTime = substate.startTime + time
			this.running = running
			//this.firstUpdateSingleFrame = true
			update(0)
			onChangeState(name)
		}
		//println("currentStateName: $currentStateName, running=$running, currentTime=$currentTime, time=$time, totalTime=$currentStateTotalTime")
	}

	fun gotoAndPlay(name: String, time: Int = 0) = gotoAndRunning(true, name, time)
	fun gotoAndStop(name: String, time: Int = 0) = gotoAndRunning(false, name, time)

	fun update(time: Int) {
		//println("Update[1]: $currentTime")
		//println("$currentStateName: $currentTime: running=$running")
		if (!running) return
		//println("Update[2]: $currentTime")
		if (currentSubtimeline == null) return
		//println("Update[3]: $currentTime")
		val cs = currentSubtimeline!!
		eval(currentTime, min(currentStateTotalTime, currentTime + time))
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
				currentTime += accumulatedTime
				eval(currentTime - accumulatedTime, currentTime)
			}

		}
	}

	private val tempRangeResult = Timed.RangeResult()

	private fun eval(prev: Int, current: Int) {
		if (prev >= current) return
		val actionsTimeline = this.currentSubtimeline?.actions ?: return
		val result = actionsTimeline.getRangeIndices(prev, current - 1, out = tempRangeResult)

		execution@ for (n in result.startIndex..result.endIndex) {
			val action = actionsTimeline.objects[n]
			//println(" Action: $action")
			when (action) {
				is AnPlaySoundAction -> {


					library.views.asyncImmediately {
						val data = (library.symbolsById[action.soundId] as AnSymbolSound?)?.getNativeSound()
						if (data != null) {
							data.play()
						}
					}
					//println("play sound!")
				}
				is AnEventAction -> {
					//println("Dispatched event(${onEvent}): ${action.event}")
					onEvent(action.event)
				}
			}
		}
	}
}

interface AnPlayable {
	fun play(name: String): Unit
}

class AnSimpleAnimation(
	val frameTime: Int,
	val animations: Map<String, List<BmpSlice?>>,
	val anchor: Anchor = Anchor.TOP_LEFT
) : Container(), AnPlayable {
	override fun createInstance(): View = AnSimpleAnimation(frameTime, animations, anchor)

	val image = Image(Bitmaps.transparent)
	val defaultAnimation = animations.values.firstOrNull() ?: listOf()
	var animation = defaultAnimation
	val numberOfFrames get() = animation.size
	private var elapsedTime = 0

	init {
		image.anchorX = anchor.sx
		image.anchorY = anchor.sy
		myupdate()
		this += image
	}

	override fun play(name: String) {
		animation = animations[name] ?: defaultAnimation
	}

	init {
		addUpdatable { dtMs ->
			elapsedTime = (elapsedTime + dtMs) % (numberOfFrames * frameTime)
			myupdate()
		}
	}

	private fun myupdate() {
		val frameNum = elapsedTime / frameTime
		val bmpSlice = animation.getOrNull(frameNum % numberOfFrames) ?: Bitmaps.transparent
		image.bitmap = bmpSlice
	}
}

class AnMovieClip(override val library: AnLibrary, override val symbol: AnSymbolMovieClip) : Container(),
	AnElement, AnPlayable {
	override fun clone(): View = createInstance().apply {
		this@apply.copyPropsFrom(this)
	}

	override fun createInstance(): View = symbol.create(library) as View

	private val tempTimedResult = Timed.Result<AnSymbolTimelineFrame>()
	val totalDepths = symbol.limits.totalDepths
	val totalUids = symbol.limits.totalUids
	val dummyDepths = Array(totalDepths) { DummyView() }
	val maskPushDepths = IntArray(totalDepths + 10) { -1 }
	val maskPopDepths = BooleanArray(totalDepths + 10) { false }
	val viewUids = Array(totalUids) {
		val info = symbol.uidInfo[it]
		val view = library.create(info.characterId) as View
		view.addProps(info.extraProps)
		view
	}
	var firstUpdate = true
	var smoothing = library.defaultSmoothing
	val singleFrame = symbol.limits.totalFrames <= 1
	val unsortedChildren = ArrayList<View>(dummyDepths.toList())
	val timelineRunner = TimelineRunner(this, symbol)
	val onStop get() = timelineRunner.onStop
	val onEvent get() = timelineRunner.onEvent
	val onChangeState get() = timelineRunner.onChangeState

	val currentState: String? get() = timelineRunner.currentStateName

	init {
		dummyDepths.fastForEach { d ->
			this += d
		}
		addUpdatable { updateInternal(it) }
	}

	private fun replaceDepth(depth: Int, view: View): Boolean {
		val result = unsortedChildren[depth].replaceWith(view)
		unsortedChildren[depth] = view
		return result
	}

	override fun reset() {
		super.reset()
		viewUids.fastForEach { view ->
			view.reset()
		}
		for (n in 0 until unsortedChildren.size) {
			replaceDepth(n, dummyDepths[n])
		}
	}

	companion object {
		class RenderState(val stencil: AG.StencilState, val colorMask: AG.ColorMaskState) {
			fun set(ctx: RenderContext, referenceValue: Int) {
				ctx.flush()
				if (ctx.masksEnabled) {
					stencil.referenceValue = referenceValue
					ctx.batch.stencil = stencil
					ctx.batch.colorMask = colorMask
				} else {
					stencil.referenceValue = 0
					ctx.batch.stencil = STATE_NONE.stencil
					ctx.batch.colorMask = STATE_NONE.colorMask
				}
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
				readMask = 0x00,
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

	private val tempMatrix = Matrix()
	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return

		maskPopDepths.fill(false)

		var usedStencil = false

		var state = 0

		//println("::::")

        forEachChildrenWithIndex { depth, child ->
			val maskDepth = maskPushDepths.getOrElse(depth) { -1 }

			// Push Mask
			if (maskDepth >= 0) {
				if (maskDepth in maskPopDepths.indices) {
					maskPopDepths[maskDepth] = true
					ctx.stencilIndex++
					usedStencil = true
					STATE_SHAPE.set(ctx, ctx.stencilIndex)
					state = 1
					//println(" shape")
				}
			}

			val showChild = when {
				ctx.masksEnabled -> true
				else -> {
					true
					//(ctx.stencilIndex <= 0) || (state != 2)
				}
			}

			//println("$depth:")
			//println(ctx.batch.colorMask)
			if (showChild) {
				child.render(ctx)
			}

			// Mask content
			if (maskDepth >= 0) {
				//println(" content")
				STATE_CONTENT.set(ctx, ctx.stencilIndex)
				state = 2
			}

			// Pop Mask
			if (maskPopDepths.getOrElse(depth) { false }) {
				//println(" none")
				STATE_NONE.set(ctx, referenceValue = 0)
				ctx.stencilIndex--
				state = 0
			}

			//println("  " + ctx.batch.colorMask)
		}

		// Reset stencil
		if (usedStencil && ctx.stencilIndex <= 0) {
			//println("ctx.stencilIndex: ${ctx.stencilIndex}")
			ctx.stencilIndex = 0
			ctx.ag.clear(clearColor = false, clearDepth = false, clearStencil = true, stencil = ctx.stencilIndex)
		}
	}

	private fun update() {
		for (depth in 0 until totalDepths) {
			val timelines = timelineRunner.currentSubtimeline?.timelines ?: continue
			val timeline = timelines[depth]
			if (timeline.size <= 0) continue // No Frames!
			val hasMultipleFrames = timeline.size > 1

			if (smoothing) {
				val (index, left, right, ratio) = timeline.find(timelineRunner.currentTime, out = tempTimedResult)
				if (left != null) maskPushDepths[left.depth] = left.clipDepth

				val view = if (left != null && left.uid >= 0) viewUids[left.uid] else dummyDepths[depth]

				//if (view.name == "action") {
				//	println("action")
				//}

				val placed = replaceDepth(depth, view)
				if (placed || hasMultipleFrames) {
					if ((left != null) && (right != null) && (left.uid == right.uid)) {
						//println("$currentTime: $index")
						AnSymbolTimelineFrame.setToViewInterpolated(view, left, right, ratio)
					} else {
						//println("$currentTime: $index")
						left?.setToView(view)
						//println(left.colorTransform)
					}
					if (symbol.ninePatch != null && view is AnBaseShape) view.ninePatch = symbol.ninePatch
				}
			} else {
				val (index, left) = timeline.findWithoutInterpolation(timelineRunner.currentTime, out = tempTimedResult)
				if (left != null) maskPushDepths[left.depth] = left.clipDepth
				val view = if (left != null && left.uid >= 0) viewUids[left.uid] else dummyDepths[depth]
				//println("$currentTime: $index")
				val placed = replaceDepth(depth, view)
				if (placed || hasMultipleFrames) {
					left?.setToView(view)
					if (symbol.ninePatch != null && view is AnBaseShape) view.ninePatch = symbol.ninePatch
				}
			}
		}
		//timelineRunner.firstUpdateSingleFrame = false
	}

	val stateNames get() = symbol.states.map { it.value.name }

	/**
	 * Changes the state and plays it
	 */
	override fun play(name: String) {
		timelineRunner.gotoAndPlay(name)
		update()
	}

	suspend fun playAndWaitStop(name: String): Unit { playAndWaitEvent(name, setOf()) }

	suspend fun playAndWaitEvent(name: String, vararg events: String): String? = playAndWaitEvent(name, events.toSet())

	suspend fun playAndWaitEvent(name: String, eventsSet: Set<String>): String? {
		return _waitEvent(eventsSet) { play(name) }
	}

	suspend fun waitStop() = _waitEvent(setOf())

	suspend fun waitEvent(vararg events: String) = _waitEvent(events.toSet())
	suspend fun waitEvent(eventsSet: Set<String>) = _waitEvent(eventsSet)

	private suspend fun _waitEvent(eventsSet: Set<String>, afterSignals: () -> Unit = {}): String? {
		val once = Once()
		val deferred = CompletableDeferred<String?>(Job())
		val closeables = arrayListOf<Closeable>()
		//println("Listening($onEvent) : $eventsSet")
		closeables += onStop {
			//println("onStop")
			once { deferred.complete(null) }
		}
		if (eventsSet.isNotEmpty()) {
			closeables += onChangeState {
				//println("onChangeState: $it")
				if (it in eventsSet) {
					//println("completed! $it")
					once { deferred.complete(it) }
				}
			}
			closeables += onEvent {
				//println("onEvent: $it")
				if (it in eventsSet) {
					//println("completed! $it")
					once { deferred.complete(it) }
				}
			}
		}
		try {
			afterSignals()
			return deferred.await()
		} finally {
			closeables.fastForEach { c ->
				c.close()
			}
		}
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

	private fun updateInternal(dtMs: Int) {
		if (timelineRunner.running && (firstUpdate || !singleFrame)) {
			firstUpdate = false
			timelineRunner.update(dtMs * 1000)
			update()
		}
	}

	override fun toString(): String = super.toString() + ":symbol=" + symbol
}

fun View?.play(name: String) { (this as? AnPlayable?)?.play(name) }
suspend fun View?.playAndWaitStop(name: String) { (this as? AnMovieClip?)?.playAndWaitStop(name) }
suspend fun View?.playAndWaitEvent(name: String, vararg events: String) =
	run { (this as? AnMovieClip?)?.playAndWaitEvent(name, *events) }

suspend fun View?.waitStop() { (this as? AnMovieClip?)?.waitStop() }
suspend fun View?.waitEvent(vararg events: String) { (this as? AnMovieClip?)?.waitEvent(*events) }

val View?.playingName: String? get() = (this as? AnMovieClip?)?.timelineRunner?.currentStateName
fun View?.seekStill(name: String, ratio: Double = 0.0) { (this as? AnMovieClip?)?.seekStill(name, ratio) }
