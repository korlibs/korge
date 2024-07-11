package korlibs.korge.view

import korlibs.datastructure.*
import korlibs.time.*
import korlibs.math.umod
import korlibs.image.bitmap.*
import korlibs.io.async.Signal
import korlibs.math.geom.*
import korlibs.math.geom.vector.VectorPath
import kotlin.time.*

inline fun Container.sprite(
    initialAnimation: SpriteAnimation, anchor: Anchor = Anchor.TOP_LEFT, callback: @ViewDslMarker Sprite.() -> Unit = {}
): Sprite = Sprite(initialAnimation, anchor).addTo(this, callback)

inline fun Container.sprite(
    texture: BmpSlice = Bitmaps.white, anchor: Anchor = Anchor.TOP_LEFT, callback: @ViewDslMarker Sprite.() -> Unit = {}
): Sprite = Sprite(texture, anchor).addTo(this, callback)

inline fun Container.sprite(
    texture: Bitmap, anchor: Anchor = Anchor.TOP_LEFT, callback: @ViewDslMarker Sprite.() -> Unit = {}
): Sprite = Sprite(texture, anchor).addTo(this, callback)

/**
 * A [Sprite] is basically an [Image] with added abilities to display a [SpriteAnimation]
 * The regular usage is to initialize the [Sprite] with one [SpriteAnimation]. The first
 * displayed bitmap will be the first element of the [SpriteAnimation]s spriteStack.
 * @property animationRequested Boolean
 * @property animationNumberOfFramesRequested Int
 * @property onAnimationCompleted Signal<SpriteAnimation>
 * @property onAnimationStopped Signal<SpriteAnimation>
 * @property onAnimationStarted Signal<SpriteAnimation>
 * @property animationLooped Boolean
 * @property lastAnimationFrameTime TimeSpan
 * @property animationRequestedDuration TimeSpan
 * @property spriteDisplayTime TimeSpan
 * @property currentAnimation SpriteAnimation?
 * @property currentSpriteIndex Int
 * @constructor It is possible to initialize a [Sprite] with a static [Bitmap] or [BmpSlice].
 * This will be exchanged when starting a [SpriteAnimation] with one of the available play functions
 */
open class Sprite(
    bitmap: BmpSlice = Bitmaps.white,
    anchor: Anchor = Anchor.TOP_LEFT,
    hitShape: VectorPath? = null,
    smoothing: Boolean = true
) : BaseImage(bitmap, anchor, hitShape, smoothing) {
    constructor(
        bitmap: Bitmap,
        anchor: Anchor = Anchor.TOP_LEFT,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true
    ) : this(bitmap.slice(), anchor, hitShape, smoothing)

    constructor(
        initialAnimation: SpriteAnimation,
        anchor: Anchor = Anchor.TOP_LEFT,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true
    ) : this(initialAnimation.firstSprite, anchor, hitShape, smoothing) {
        currentAnimation = initialAnimation
        setFrame(0)
    }

    private var animationRequested = false
    var totalFramesPlayed = 0
    private var animationNumberOfFramesRequested = 0
        set(value) {
            if (value == 0) {
                stopAnimation()
                when (animationType) {
                    AnimationType.STANDARD -> triggerEvent(onAnimationCompleted)
                    else -> triggerEvent(onAnimationStopped)
                }
            }
            field = value
        }
    private var animationType = AnimationType.STANDARD

    private var _onAnimationCompleted: Signal<SpriteAnimation>? = null
    private var _onAnimationStopped: Signal<SpriteAnimation>? = null
    private var _onAnimationStarted: Signal<SpriteAnimation>? = null
    private var _onFrameChanged: Signal<SpriteAnimation>? = null

    val onAnimationCompleted: Signal<SpriteAnimation>
        get() {
            if (_onAnimationCompleted == null) _onAnimationCompleted = Signal()
            return _onAnimationCompleted!!
        }

    val onAnimationStopped: Signal<SpriteAnimation>
        get() {
            if (_onAnimationStopped == null) _onAnimationStopped = Signal()
            return _onAnimationStopped!!
        }

    val onAnimationStarted: Signal<SpriteAnimation>
        get() {
            if (_onAnimationStarted == null) _onAnimationStarted = Signal()
            return _onAnimationStarted!!
        }

    val onFrameChanged: Signal<SpriteAnimation>
        get() {
            if (_onFrameChanged == null) _onFrameChanged = Signal()
            return _onFrameChanged!!
        }

    var spriteDisplayTime: Duration
        set(value) { fastSpriteDisplayTime = value.fast }
        get() = fastSpriteDisplayTime.toDuration()
    var fastSpriteDisplayTime: FastDuration = 50.fastMilliseconds
    private var animationLooped = false
    private var lastAnimationFrameTime: FastDuration = 0.fastSeconds
    private var animationRemainingDuration: FastDuration = 0.fastSeconds
        set(value) {
            if (value <= 0.milliseconds && animationType == AnimationType.DURATION) {
                stopAnimation()
                triggerEvent(_onAnimationCompleted)
            }
            field = value
        }

    private var currentAnimation: SpriteAnimation? = null

    var currentSpriteIndex = 0
        private set(value) {
            field = value umod totalFrames
            bitmap = currentAnimation?.getSprite(value) ?: bitmap
        }

    private var reversed = false

    init {
        addFastUpdater { frameTime ->
            //println("UPDATER: animationRequested=$animationRequested")
            if (animationRequested) {
                nextSprite(frameTime)
            }
        }
    }

    private fun getDefaultTime(spriteAnimation: SpriteAnimation?): FastDuration = when {
        spriteAnimation != null && spriteAnimation.fastDefaultTimePerFrame != FastDuration.NaN -> spriteAnimation.fastDefaultTimePerFrame
        else -> fastSpriteDisplayTime
    }



    fun playAnimation(
        times: Int = 1,
        spriteAnimation: SpriteAnimation? = currentAnimation,
        spriteDisplayTime: FastDuration = getDefaultTime(spriteAnimation),
        startFrame: Int = -1,
        endFrame: Int = 0,
        reversed: Boolean = false
    ) = updateCurrentAnimation(
        spriteAnimation = spriteAnimation,
        spriteDisplayTime = spriteDisplayTime,
        animationCyclesRequested = times,
        startFrame = if (startFrame >= 0) startFrame else currentSpriteIndex,
        endFrame = endFrame,
        reversed = reversed,
        type = AnimationType.STANDARD
    )

    fun playAnimation(
        spriteAnimation: SpriteAnimation? = currentAnimation,
        spriteDisplayTime: FastDuration = getDefaultTime(spriteAnimation),
        startFrame: Int = -1,
        endFrame: Int = 0,
        reversed: Boolean = false
    ) = updateCurrentAnimation(
        spriteAnimation = spriteAnimation,
        spriteDisplayTime = spriteDisplayTime,
        animationCyclesRequested = 1,
        startFrame = if (startFrame >= 0) startFrame else currentSpriteIndex,
        endFrame = endFrame,
        reversed = reversed,
        type = AnimationType.STANDARD
    )

    fun playAnimationForDuration(
        duration: Duration,
        spriteAnimation: SpriteAnimation? = currentAnimation,
        spriteDisplayTime: FastDuration = getDefaultTime(spriteAnimation),
        startFrame: Int = -1,
        reversed: Boolean = false
    ) = updateCurrentAnimation(
        spriteAnimation = spriteAnimation,
        spriteDisplayTime = spriteDisplayTime,
        duration = duration.fast,
        startFrame = if (startFrame >= 0) startFrame else currentSpriteIndex,
        reversed = reversed,
        type = AnimationType.DURATION
    )

    fun playAnimationLooped(
        spriteAnimation: SpriteAnimation? = currentAnimation,
        spriteDisplayTime: FastDuration = getDefaultTime(spriteAnimation),
        startFrame: Int = -1,
        reversed: Boolean = false
    ) = updateCurrentAnimation(
        spriteAnimation = spriteAnimation,
        spriteDisplayTime = spriteDisplayTime,
        startFrame = if (startFrame >= 0) startFrame else currentSpriteIndex,
        looped = true,
        reversed = reversed,
        type = AnimationType.LOOPED
    )







    fun playAnimation(
        times: Int = 1,
        spriteAnimation: SpriteAnimation? = currentAnimation,
        spriteDisplayTime: Duration,
        startFrame: Int = -1,
        endFrame: Int = 0,
        reversed: Boolean = false
    ) = updateCurrentAnimation(
        spriteAnimation = spriteAnimation,
        spriteDisplayTime = spriteDisplayTime.fast,
        animationCyclesRequested = times,
        startFrame = if (startFrame >= 0) startFrame else currentSpriteIndex,
        endFrame = endFrame,
        reversed = reversed,
        type = AnimationType.STANDARD
    )

    fun playAnimation(
        spriteAnimation: SpriteAnimation? = currentAnimation,
        spriteDisplayTime: Duration,
        startFrame: Int = -1,
        endFrame: Int = 0,
        reversed: Boolean = false
    ) = updateCurrentAnimation(
        spriteAnimation = spriteAnimation,
        spriteDisplayTime = spriteDisplayTime.fast,
        animationCyclesRequested = 1,
        startFrame = if (startFrame >= 0) startFrame else currentSpriteIndex,
        endFrame = endFrame,
        reversed = reversed,
        type = AnimationType.STANDARD
    )

    fun playAnimationForDuration(
        duration: Duration,
        spriteAnimation: SpriteAnimation? = currentAnimation,
        spriteDisplayTime: Duration,
        startFrame: Int = -1,
        reversed: Boolean = false
    ) = updateCurrentAnimation(
        spriteAnimation = spriteAnimation,
        spriteDisplayTime = spriteDisplayTime.fast,
        duration = duration.fast,
        startFrame = if (startFrame >= 0) startFrame else currentSpriteIndex,
        reversed = reversed,
        type = AnimationType.DURATION
    )

    fun playAnimationLooped(
        spriteAnimation: SpriteAnimation? = currentAnimation,
        spriteDisplayTime: Duration,
        startFrame: Int = -1,
        reversed: Boolean = false
    ) = updateCurrentAnimation(
        spriteAnimation = spriteAnimation,
        spriteDisplayTime = spriteDisplayTime.fast,
        startFrame = if (startFrame >= 0) startFrame else currentSpriteIndex,
        looped = true,
        reversed = reversed,
        type = AnimationType.LOOPED
    )

    fun stopAnimation() {
        animationRequested = false
        triggerEvent(_onAnimationStopped)
    }

    private fun nextSprite(frameTime: FastDuration) {
        lastAnimationFrameTime += frameTime
        if (lastAnimationFrameTime + frameTime >= this.fastSpriteDisplayTime) {
            when (animationType) {
                AnimationType.STANDARD -> {
                    if (animationNumberOfFramesRequested > 0) {
                        animationNumberOfFramesRequested--
                    }
                }

                AnimationType.DURATION -> {
                    animationRemainingDuration -= lastAnimationFrameTime
                }

                AnimationType.LOOPED -> {
                }
            }
            if (reversed) --currentSpriteIndex else ++currentSpriteIndex
            totalFramesPlayed++
            triggerEvent(_onFrameChanged)
            lastAnimationFrameTime = 0.fastSeconds
        }
    }

    val totalFrames: Int
        get() {
            val ca = currentAnimation ?: return 1
            return ca.size
        }

    private fun updateCurrentAnimation(
        spriteAnimation: SpriteAnimation?,
        spriteDisplayTime: FastDuration = getDefaultTime(spriteAnimation),
        animationCyclesRequested: Int = 1,
        duration: FastDuration = 0.fastSeconds,
        startFrame: Int = 0,
        endFrame: Int = 0,
        looped: Boolean = false,
        reversed: Boolean = false,
        type: AnimationType = AnimationType.STANDARD
    ) {
        triggerEvent(_onAnimationStarted)
        this.fastSpriteDisplayTime = spriteDisplayTime
        currentAnimation = spriteAnimation
        animationLooped = looped
        animationRemainingDuration = duration
        currentSpriteIndex = startFrame
        this.reversed = reversed
        animationType = type
        animationRequested = true
        val endFrame = endFrame umod totalFrames
        currentAnimation?.let {
            val count = when {
                startFrame > endFrame -> (if (reversed) startFrame - endFrame else it.spriteStackSize - (startFrame - endFrame))
                endFrame > startFrame -> (if (reversed) (startFrame - endFrame) umod it.spriteStackSize else endFrame - startFrame)
                else -> 0
            }
            val requestedFrames = count + (animationCyclesRequested * it.spriteStackSize)
            this.animationNumberOfFramesRequested = requestedFrames
        }
    }

    fun setFrame(index: Int) {
        currentSpriteIndex = index
    }

    private fun triggerEvent(signal: Signal<SpriteAnimation>?) {
        if (signal != null) currentAnimation?.let { signal.invoke(it) }
    }
}

//enum class AnimationType {
//    STANDARD, LOOPED, DURATION
//}

inline class AnimationType(val id: Int) {
    companion object {
        inline val STANDARD get() = AnimationType(0)
        inline val LOOPED get() = AnimationType(1)
        inline val DURATION get() = AnimationType(2)
    }
}
