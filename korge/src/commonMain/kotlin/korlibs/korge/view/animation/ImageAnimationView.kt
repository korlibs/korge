package korlibs.korge.view.animation

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.time.*
import korlibs.memory.*
import korlibs.korge.view.*
import korlibs.korge.view.tiles.*
import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.image.tiles.*

inline fun Container.imageAnimationView(
    animation: ImageAnimation? = null,
    direction: ImageAnimation.Direction? = null,
    block: @ViewDslMarker ImageAnimationView<Image>.() -> Unit = {}
): ImageAnimationView<Image> = ImageAnimationView(animation, direction) { Image(Bitmaps.transparent) }.addTo(this, block)

fun ImageAnimationView(
    animation: ImageAnimation? = null,
    direction: ImageAnimation.Direction? = null,
): ImageAnimationView<Image> = ImageAnimationView(animation, direction) { Image(Bitmaps.transparent) }

open class ImageAnimationView<T: SmoothedBmpSlice>(
    animation: ImageAnimation? = null,
    direction: ImageAnimation.Direction? = null,
    val createImage: () -> T
) : Container(), Playable {
    private var nframes: Int = 1

    fun createTilemap(): TileMap = TileMap()

    var onPlayFinished: (() -> Unit)? = null
    var onDestroyLayer: ((T) -> Unit)? = null
    var onDestroyTilemapLayer: ((TileMap) -> Unit)? = null

    var animation: ImageAnimation? = animation
        set(value) {
            if (field !== value) {
                field = value
                didSetAnimation()
            }
        }
    var direction: ImageAnimation.Direction? = direction
        set(value) {
            if (field !== value) {
                field = value
                setFirstFrame()
            }
        }

    private val computedDirection: ImageAnimation.Direction get() = direction ?: animation?.direction ?: ImageAnimation.Direction.FORWARD
    private val layers = fastArrayListOf<View>()
    private val layersByName = FastStringMap<View>()
    private var nextFrameIn = 0.milliseconds
    private var nextFrameIndex = 0
    private var dir = +1

    fun getLayer(name: String): View? = layersByName[name]

    var smoothing: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                layers.fastForEach {
                    if (it is SmoothedBmpSlice) it.smoothing = value
                }
            }
        }


    private fun setFrame(frameIndex: Int) {
        val frame = if (animation?.frames?.isNotEmpty() == true) animation?.frames?.getCyclicOrNull(frameIndex) else null
        if (frame != null) {
            frame.layerData.fastForEach {
                val image = layers[it.layer.index]
                when (it.layer.type) {
                    ImageLayer.Type.NORMAL -> {
                        (image as SmoothedBmpSlice).bitmap = it.slice
                    }
                    else -> {
                        image as TileMap
                        val tilemap = it.tilemap
                        if (tilemap == null) {
                            image.stackedIntMap = StackedIntArray2(IntArray2(1, 1, 0))
                            image.tileset = TileSet.EMPTY
                        } else {
                            image.stackedIntMap = StackedIntArray2(tilemap.data)
                            image.tileset = tilemap.tileSet ?: TileSet.EMPTY
                        }
                    }
                }
                image.xy(it.targetX, it.targetY)
            }
            nextFrameIn = frame.duration
            dir = when (computedDirection) {
                ImageAnimation.Direction.FORWARD -> +1
                ImageAnimation.Direction.REVERSE -> -1
                ImageAnimation.Direction.PING_PONG -> if (frameIndex + dir !in 0 until nframes) -dir else dir
                ImageAnimation.Direction.ONCE_FORWARD -> if (frameIndex < nframes - 1) +1 else 0
                ImageAnimation.Direction.ONCE_REVERSE -> if (frameIndex == 0) 0 else -1
            }
            nextFrameIndex = (frameIndex + dir) umod nframes
        } else {
            layers.fastForEach {
                if (it is SmoothedBmpSlice) {
                    it.bitmap = Bitmaps.transparent
                }
            }
        }
    }

    private fun setFirstFrame() {
        if (computedDirection == ImageAnimation.Direction.REVERSE || computedDirection == ImageAnimation.Direction.ONCE_REVERSE) {
            setFrame(nframes - 1)
        } else {
            setFrame(0)
        }
    }

    private fun didSetAnimation() {
        nframes = animation?.frames?.size ?: 1
        // Before clearing layers let parent possibly recycle layer objects (e.g. return to pool, etc.)
        for (layer in layers) {
            if (layer is TileMap) {
                onDestroyTilemapLayer?.invoke(layer)
            } else {
                onDestroyLayer?.invoke(layer as T)
            }
        }
        layers.clear()
        removeChildren()
        dir = +1
        val animation = this.animation
        if (animation != null) {
            for (layer in animation.layers) {
                val image: View = when (layer.type) {
                    ImageLayer.Type.NORMAL -> {
                        createImage().also { it.smoothing = smoothing } as View
                    }
                    ImageLayer.Type.TILEMAP -> createTilemap()
                    ImageLayer.Type.GROUP -> TODO()
                }
                layers.add(image)
                layersByName[layer.name ?: "default"] = image
                addChild(image as View)
            }
        }
        setFirstFrame()
    }

    private var running = true
    override fun play() { running = true }
    override fun stop() { running = false }
    override fun rewind() { setFirstFrame() }

    init {
        didSetAnimation()
        addUpdater {
            //println("running=$running, nextFrameIn=$nextFrameIn, nextFrameIndex=$nextFrameIndex")
            if (running) {
                nextFrameIn -= it
                if (nextFrameIn <= 0.0.milliseconds) {
                    setFrame(nextFrameIndex)
                    // Check if animation should be played only once
                    if (dir == 0) {
                        running = false
                        onPlayFinished?.invoke()
                    }
                }
            }
        }
    }
}