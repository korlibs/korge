package com.soywiz.korge.animate

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.TimeSpan
import com.soywiz.klock.microseconds
import com.soywiz.klock.milliseconds
import com.soywiz.klock.min
import com.soywiz.klock.seconds
import com.soywiz.kmem.clamp01
import com.soywiz.korge.debug.ObservableProperty
import com.soywiz.korge.debug.UiListEditableValue
import com.soywiz.korge.debug.UiRowEditableValue
import com.soywiz.korge.debug.uiCollapsibleSection
import com.soywiz.korge.debug.uiEditableValue
import com.soywiz.korge.html.Html
import com.soywiz.korge.internal.KorgeDeprecated
import com.soywiz.korge.render.MaskStates
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.DummyView
import com.soywiz.korge.view.GraphicsRenderer
import com.soywiz.korge.view.IHtml
import com.soywiz.korge.view.IText
import com.soywiz.korge.view.Image
import com.soywiz.korge.view.Graphics
import com.soywiz.korge.view.TextOld
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewLeaf
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.replaceWith
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.vector.EmptyShape
import com.soywiz.korim.vector.Shape
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.util.Once
import com.soywiz.korma.geom.Anchor
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korui.UiContainer
import com.soywiz.korui.button
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job

interface AnElement {
	val library: AnLibrary
	val symbol: AnSymbol
}

fun AnElement.createDuplicated() = symbol.create(library)
fun AnElement.createDuplicatedView() = symbol.create(library) as View

abstract class AnBaseShape(final override val library: AnLibrary, final override val symbol: AnSymbolBaseShape) :
	Container(), AnElement {
	var ninePatch: Rectangle? = null

	abstract var dx: Float
	abstract var dy: Float
	abstract val tex: BmpSlice
    abstract val shape: Shape?
    abstract val graphicsRenderer: GraphicsRenderer
	abstract val texScale: Double
	abstract val texWidth: Float
	abstract val texHeight: Float
	abstract val smoothing: Boolean
    var graphics: Graphics? = null
    //private var graphics: Graphics? = null

    var dxDouble: Double
        get() = dx.toDouble()
        set(value) { dx = value.toFloat() }

    var dyDouble: Double
        get() = dy.toDouble()
        set(value) { dy = value.toFloat() }

    val posCuts = arrayOf(Point(0.0, 0.0), Point(0.25, 0.25), Point(0.75, 0.75), Point(1.0, 1.0))
	val texCuts = arrayOf(Point(0.0, 0.0), Point(0.25, 0.25), Point(0.75, 0.75), Point(1.0, 1.0))

    private var cachedShape: Shape? = null

    private fun ensureShape(): Shape? {
        if (shape != null) {
            if (graphics == null) {
                graphics = graphics(EmptyShape, library.graphicsRenderer ?: graphicsRenderer)
                //graphics = graphics(shape!!)
            }
            if (cachedShape !== shape) {
                cachedShape = shape
                graphics?.shape = shape!!
            }
        }
        return shape
    }

	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
        if (ensureShape() != null) {
            graphics?.renderer = library.graphicsRenderer ?: graphicsRenderer
            super.renderInternal(ctx)
            return
        }
        ctx.useBatcher { batch ->
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

                batch.drawNinePatch(
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
                    blendMode = renderBlendMode,
                    premultiplied = tex.base.premultiplied,
                    wrap = false,
                )
            } else {
                batch.drawQuad(
                    ctx.getTex(tex),
                    x = dx,
                    y = dy,
                    width = texWidth,
                    height = texHeight,
                    m = globalMatrix,
                    filtering = smoothing,
                    colorMul = renderColorMul,
                    colorAdd = renderColorAdd,
                    blendMode = renderBlendMode,
                    premultiplied = tex.base.premultiplied,
                    wrap = false,
                )
            }
        }
	}

    override var hitShape: VectorPath? = null
        get() = field ?: symbol.path
        set(value) { field = value }

    //override fun hitTest(x: Double, y: Double): View? {
	//	val sLeft = dx.toDouble()
	//	val sTop = dy.toDouble()
	//	val sRight = sLeft + texWidth
	//	val sBottom = sTop + texHeight
	//	return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) &&
	//		(symbol.path?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) != false)
	//	) this else null
	//}

	override fun getLocalBoundsInternal(out: Rectangle) {
        if (ensureShape() != null) {
            return super.getLocalBoundsInternal(out)
        }
		out.setTo(dx, dy, texWidth, texHeight)
	}

	override fun toString(): String = super.toString() + ":symbol=" + symbol

	override fun createInstance(): View = symbol.create(library) as View

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        val view = this
        container.uiCollapsibleSection("AnBaseShape") {
            uiEditableValue(Pair(view::dxDouble, view::dyDouble), name = "dxy", clamp = false)
            button("Center").onClick {
                view.dx = (-view.width / 2).toFloat()
                view.dy = (-view.height / 2).toFloat()
            }
        }
        super.buildDebugComponent(views, container)
    }
}

class AnShape(library: AnLibrary, val shapeSymbol: AnSymbolShape) : AnBaseShape(library, shapeSymbol), AnElement {
	override var dx = shapeSymbol.bounds.x.toFloat()
	override var dy = shapeSymbol.bounds.y.toFloat()
	override val tex = shapeSymbol.textureWithBitmap?.texture ?: Bitmaps.transparent
    override val shape: Shape? = shapeSymbol.shapeGen?.invoke()
    override val graphicsRenderer: GraphicsRenderer get() = shapeSymbol.graphicsRenderer
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
    override var shape: Shape? = null
    override var graphicsRenderer: GraphicsRenderer = morphSymbol.graphicsRenderer
	override var texScale = 1.0
	override var texWidth = 0f
	override var texHeight = 0f
	override var smoothing = true

	private fun updatedRatio() {
        if (morphSymbol.shapeGen != null) {
            shape = morphSymbol.shapeGen!!.invoke(ratio)
            return
        }
		val result = morphSymbol.texturesWithBitmap.find(ratio.seconds, timedResult)
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

@OptIn(KorgeDeprecated::class)
class AnTextField(override val library: AnLibrary, override val symbol: AnTextFieldSymbol) : Container(),
	AnElement, IText, IHtml, ViewLeaf {
    private val textField = TextOld("", 16.0).apply {
        fontsCatalog = library.fontsCatalog
		textBounds.copyFrom(this@AnTextField.symbol.bounds)
		html = this@AnTextField.symbol.initialHtml
		relayout()
	}

	init {
		this += textField
	}

	var format: Html.Format by textField::format
	override var text: String by textField::text
	override var html: String by textField::html

	override fun createInstance(): View = symbol.create(library) as View

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection("AnTextField") {
            uiEditableValue(::text)
        }
        super.buildDebugComponent(views, container)
    }
}

//class PopMaskView(views: Views) : View(views)

class TimelineRunner(val view: AnMovieClip, val symbol: AnSymbolMovieClip) {
	//var firstUpdateSingleFrame = false
	val library: AnLibrary get() = view.library
	val context get() = library.context
	var currentTime = 0.milliseconds
	var currentStateName: String? = null
	var currentSubtimeline: AnSymbolMovieClipSubTimeline? = null
	val currentStateTotalTime: TimeSpan get() = if (currentSubtimeline != null) currentSubtimeline!!.totalTime else 0.milliseconds
	val onStop = Signal<Unit>()
	val onChangeState = Signal<String>()
	val onEvent = Signal<String>()

	var running = true
		internal set(value) {
			field = value
			if (!value) {
				onStop(Unit)
			}
		}

	init {
		gotoAndPlay("default")
	}

	fun getStateTime(name: String): TimeSpan {
		val substate = symbol.states[name] ?: return 0.milliseconds
		return substate.subTimeline.totalTime - substate.startTime
	}

	fun gotoAndRunning(running: Boolean, name: String, time: TimeSpan = 0.milliseconds) {
		val substate = symbol.states[name]
		if (substate != null) {
			this.currentStateName = substate.name
			this.currentSubtimeline = substate.subTimeline
			this.currentTime = substate.startTime + time
			this.running = running
			//this.firstUpdateSingleFrame = true
			update(0.milliseconds)
			onChangeState(name)
		} else {
            println("Can't find state with name '$name' : ${symbol.states.keys}")
        }
        //println("gotoAndRunning: running=$running, name=$name, time=$time")
		//println("currentStateName: $currentStateName, running=$running, currentTime=$currentTime, time=$time, totalTime=$currentStateTotalTime")
	}

	fun gotoAndPlay(name: String, time: TimeSpan = 0.milliseconds) = gotoAndRunning(true, name, time)
	fun gotoAndStop(name: String, time: TimeSpan = 0.milliseconds) = gotoAndRunning(false, name, time)

    var ratio: Double
        get() = currentTime / currentStateTotalTime
        set(value) {
            currentTime = (currentStateTotalTime * value.clamp01())
        }

	fun update(time: TimeSpan) {
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
				gotoAndRunning(cs.nextStatePlay, nextState, 0.milliseconds)
				currentTime += accumulatedTime
				eval(currentTime - accumulatedTime, currentTime)
			}

		}
	}

	private val tempRangeResult = Timed.RangeResult()

	private fun eval(prev: TimeSpan, current: TimeSpan) {
		if (prev >= current) return
		val actionsTimeline = this.currentSubtimeline?.actions ?: return
		val result = actionsTimeline.getRangeIndices(prev, current - 1.microseconds, out = tempRangeResult)

		execution@ for (n in result.startIndex..result.endIndex) {
			val action = actionsTimeline.objects[n]
			//println(" Action: $action")
			when (action) {
				is AnPlaySoundAction -> {
                    launchImmediately(library.context.coroutineContext) {
						(library.symbolsById[action.soundId] as AnSymbolSound?)?.getNativeSound()?.play()
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
	val frameTime: TimeSpan,
	val animations: Map<String, List<BmpSlice?>>,
	val anchor: Anchor = Anchor.TOP_LEFT
) : Container(), AnPlayable {
	override fun createInstance(): View = AnSimpleAnimation(frameTime, animations, anchor)

	val image = Image(Bitmaps.transparent)
	val defaultAnimation = animations.values.firstOrNull() ?: listOf()
	var animation = defaultAnimation
	val numberOfFrames get() = animation.size
	private var elapsedTime = 0.milliseconds

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
		addUpdater {
			elapsedTime = (elapsedTime + it) % (frameTime * numberOfFrames)
			myupdate()
		}
	}

	private fun myupdate() {
		val frameNum = (elapsedTime / frameTime).toInt()
		val bmpSlice = animation.getOrNull(frameNum % numberOfFrames) ?: Bitmaps.transparent
		image.bitmap = bmpSlice
	}
}

class AnMovieClip(override val library: AnLibrary, override val symbol: AnSymbolMovieClip) : Container(),
	AnElement, AnPlayable {
	override fun clone(): View = createInstance().apply {
		this@apply.copyPropsFrom(this)
	}

    override fun getLocalBoundsInternal(out: Rectangle) {
        if (symbol.id == 0) {
            out.setTo(0, 0, library.width, library.height)
        } else {
            super.getLocalBoundsInternal(out)
        }
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
        if (info.characterId == symbol.id) error("Recursive detection")
		(library.create(info.characterId) as View).also { it.addProps(info.extraProps) }
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
        addUpdater { updateInternal(it) }
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

	private val tempMatrix = Matrix()
    private val tempLocalRenderState = MaskStates.LocalRenderState()
	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return

		maskPopDepths.fill(false)

		var usedStencil = false

		var state = 0

		//println("::::")

        forEachChildWithIndex { depth: Int, child: View ->
			val maskDepth = maskPushDepths.getOrElse(depth) { -1 }

			// Push Mask
			if (maskDepth >= 0) {
				if (maskDepth in maskPopDepths.indices) {
					maskPopDepths[maskDepth] = true
					ctx.stencilIndex++
					usedStencil = true
                    MaskStates.STATE_SHAPE.set(ctx, ctx.stencilIndex, tempLocalRenderState)
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
                MaskStates.STATE_CONTENT.set(ctx, ctx.stencilIndex, tempLocalRenderState)
				state = 2
			}

			// Pop Mask
			if (maskPopDepths.getOrElse(depth) { false }) {
				//println(" none")
                MaskStates.STATE_NONE.set(ctx, referenceValue = 0, tempLocalRenderState)
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

    fun playAndStop(name: String) {
        timelineRunner.gotoAndStop(name)
        update()
    }

    fun play() {
        //println("stop")
        timelineRunner.running = true
        update()
    }

    fun stop() {
        //println("stop")
        timelineRunner.running = false
        update()
    }

    override fun buildDebugComponent(views: Views, container: UiContainer) {
        container.uiCollapsibleSection("SWF") {
            addChild(UiRowEditableValue(app, "symbol", UiListEditableValue(app, { library.symbolsByName.keys.toList() }, ObservableProperty(
                name = "symbol",
                internalSet = { symbolName ->
                    val views = stage?.views
                    val newView = library.create(symbolName) as View
                    this@AnMovieClip.replaceWith(newView)
                    views?.debugHightlightView(newView)
                },
                internalGet = { symbol.name ?: "#${symbol.id}" },
            ))))
            addChild(UiRowEditableValue(app, "gotoAndPlay", UiListEditableValue(app, { stateNames }, ObservableProperty(
                name = "gotoAndPlay",
                internalSet = { frameName -> this@AnMovieClip.play(frameName) },
                internalGet = { timelineRunner.currentStateName ?: "__start" },
            ))))
            addChild(UiRowEditableValue(app, "gotoAndStop", UiListEditableValue(app, { stateNames }, ObservableProperty(
                name = "gotoAndStop",
                internalSet = { frameName -> this@AnMovieClip.playAndStop(frameName) },
                internalGet = { timelineRunner.currentStateName ?: "__start" },
            ))))
            button("start").onClick { play() }
            button("stop").onClick { stop() }
        }
        super.buildDebugComponent(views, container)
    }

    override var ratio: Double
        get() = timelineRunner.ratio
        set(value) {
            //println("set ratio: $value")
            timelineRunner.ratio = value
            update()
        }

    suspend fun playAndWaitStop(name: String) { playAndWaitEvent(name, setOf()) }

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
		timelineRunner.gotoAndStop(name, (totalTime * ratio))
		//println("seekStill($name,$ratio) : $currentTime,$running,$currentState")
		update()
	}

	private fun updateInternal(dt: TimeSpan) {
		if (timelineRunner.running && (firstUpdate || !singleFrame)) {
			firstUpdate = false
			timelineRunner.update(dt)
            //println("Updating ${dtMs * 1000}")
			update()
		} else {
            //println("Not updating")
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
