package com.soywiz.korge.view.animation

import com.soywiz.kds.ExtraTypeCreate
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.setExtra
import com.soywiz.korge.view.*
import com.soywiz.korge.view.tiles.SingleTile
import com.soywiz.korge.view.tiles.repeat
import com.soywiz.korim.atlas.MutableAtlas
import com.soywiz.korim.format.*
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korma.geom.SizeInt
import com.soywiz.korma.math.clamp

inline fun Container.parallaxDataView(
    data: ParallaxDataContainer,
    scale: Double = 1.0,
    smoothing: Boolean = false,
    callback: @ViewDslMarker ParallaxDataView.() -> Unit = {}
): ParallaxDataView = ParallaxDataView(data, scale, smoothing).addTo(this, callback)

/**
 * The ParallaxDataView is a special view object which can be used to show some background layers behind the play
 * field of a game level, menu, intro, extro, etc. It is configured through a data object [ParallaxDataContainer] which
 * stores model data for the view (cf. MVC pattern).
 *
 * The definition of a parallax layer is an image which is (in most cases) repeating in X and/or Y direction.
 * Thus scrolling of the parallax layer means that the parallax layer is moving relative to the scene camera. In the
 * camera view it looks like that the image is moving repeatedly over the screen. By constructing multiple layers which
 * are sitting one over another and which are moving with different speed factors a nice depth effect can be created.
 * That simulates the parallax effect of early 1990 video games.
 *
 * The parallax layers can be configured through a set of data classes which control how the parallax layers are
 * constructed, presented and moved over the screen. The actual image data for a layer is taken from an Aseprite file
 * which follows specific rules. This file must contain at least one frame with a couple of layers with defined names.
 * Also, for the so-called parallax plane the Aseprite file has to contain specific slice objects. An example Aseprite
 * template file is included in Korge's example "parallax-scrolling-aseprite".
 *
 * Types of parallax layers:
 * 1. Background layers
 * 2. parallax plane with attached layers
 * 3. Foreground layers
 *
 * Background layers (1) are drawn first and per definition are shown behind the parallax plane.
 * The parallax plane (2) is a special layer in the Aseprite image which is sliced into stripes of different lengths.
 * These stripes are moving with different speed which increases with more distance to the central vanishing point on
 * the screen. This results in a pseudo 3D plane effect which can be seen in early 1990 video games like Street Fighter 2
 * or Lionheart (Amiga). The parallax plane can have attached layers which are by themselves layers like background
 * layers. The difference is that they are moving depending on their position (top or bottom border) on the parallax
 * plane.
 * Finally, the Foreground layers (3) are drawn and thus are positioned in front of the parallax plane with its attached
 * layers. Any of these layer types can be also kept empty. With a combination of all layer types it is possible to
 * achieve different parallax layer effects.
 *
 * Please see the description of [ParallaxDataContainer] and its sub-data classes [ParallaxConfig],
 * [ParallaxLayerConfig], [ParallaxPlaneConfig] and [ParallaxAttachedLayerConfig] to set up a valid and meaningful
 * configuration for the parallax view.
 */
class ParallaxDataView(
    data: ParallaxDataContainer,
    scale: Double = 1.0,
    smoothing: Boolean = false
) : Container() {

    // Delta movement in X or Y direction of the parallax background depending on the scrolling direction
    var deltaX: Double = 0.0
    var deltaY: Double = 0.0

    // Percentage of the position diagonally to the scrolling direction (only used with parallax plane setup)
    var diagonal: Double = 0.0  // Range: [0...1]

    // Accessing properties of layer objects
    private val layerMap: HashMap<String, View> = HashMap()

    // The middle point of the parallax plane (central vanishing point on the screen)
    private val parallaxPlaneMiddlePoint: Double =
        when (data.config.mode) {
            ParallaxMode.HORIZONTAL_PLANE -> data.config.size.width * 0.5
            ParallaxMode.VERTICAL_PLANE -> data.config.size.height * 0.5
            ParallaxMode.NO_PLANE -> 0.0  // not used without parallax plane setup
        }

    private val parallaxLayerSize =
        when (data.config.mode) {
            ParallaxMode.HORIZONTAL_PLANE -> data.backgroundLayers?.height ?: data.foregroundLayers?.height?: data.attachedLayers?.height ?: 0
            ParallaxMode.VERTICAL_PLANE -> data.backgroundLayers?.width ?: data.foregroundLayers?.width ?: data.attachedLayers?.width ?: 0
            ParallaxMode.NO_PLANE -> 0  // not used without parallax plane setup
        }

    // Calculate array of speed factors for each line in the parallax plane.
    // The array will contain numbers starting from 1.0 -> 0.0 and then from 0.0 -> 1.0
    // The first part of the array is used as speed factor for the upper / left side of the parallax plane.
    // The second part is used for the lower / right side of the parallax plane.
    private val parallaxPlaneSpeedFactor = DoubleArray(
        parallaxLayerSize
    ) { i ->
        val midPoint: Double = parallaxLayerSize * 0.5
        (data.config.parallaxPlane?.speed ?: 1.0) * (
            // The pixel in the point of view must not stand still, they need to move with the lowest possible speed (= 1 / midpoint)
            // Otherwise the midpoint is "running" away over time
            if (i < midPoint)
                1 - (i / midPoint)
            else
                (i - midPoint + 1) / midPoint
            )
    }

    fun getLayer(name: String): View? {
        return layerMap[name]
    }

    private fun constructParallaxPlane(
        parallaxPlane: ImageDataContainer?,
        attachedLayers: ImageData?,
        config: ParallaxPlaneConfig?,
        isScrollingHorizontally: Boolean,
        smoothing: Boolean
    ) {
        if (parallaxPlane == null || config == null) return
        if (parallaxPlane.imageDatas[0].frames.isEmpty()) error("Parallax plane not found. Check that name of parallax plane layer in Aseprite matches the name in the parallax config.")
        if (parallaxPlaneSpeedFactor.size < parallaxPlane.imageDatas.size) error("Parallax data must at least contain one layer in backgroundLayers, foregroundLayers or attachedLayers!")

        layerMap[config.name] = container {
            parallaxPlane.imageDatas.fastForEach { data ->
                imageDataView(data, playing = false, smoothing = smoothing, repeating = true) {
                    val layer = getLayer(config.name)
                    if (layer == null) {
                        error("Could not find parallax plane '${config.name}' in ImageData. Check that name of parallax plane in Aseprite matches the name in the parallax config.")
                    } else layer as SingleTile
                    if (isScrollingHorizontally) {
                        layer.repeat(repeatX = true)
                        x = parallaxPlaneMiddlePoint
                        val speedFactor = parallaxPlaneSpeedFactor[layer.y.toInt()]
                        addUpdater {
                            // Calculate the offset for the inner scrolling of the layer depending of its y-position
                            x += ((deltaX * speedFactor) + (config.selfSpeed * speedFactor)) * it.milliseconds
                        }
                    } else {
                        layer.repeat(repeatY = true)
                        y = parallaxPlaneMiddlePoint
                        val speedFactor = parallaxPlaneSpeedFactor[layer.x.toInt()]
                        addUpdater {
                            // Calculate the offset for the inner scrolling of the layer depending of its x-position
                            y += ((deltaY * speedFactor) + (config.selfSpeed * speedFactor)) * it.milliseconds
                        }
                    }
                }
            }
        }

        if (attachedLayers != null && config.attachedLayers != null) {
            if (attachedLayers.frames.isEmpty()) error("No attached layers not found. Check that name of attached layers in Aseprite matches the name in the parallax config.")

            val imageData = imageDataView(attachedLayers, playing = false, smoothing = smoothing, repeating = true)

            for (conf in config.attachedLayers) {
                val layer = imageData.getLayer(conf.name)
                    ?: error("Could not find layer '${config.name}' in ImageData. Check that name of attached layer in Aseprite matches the name in the parallax config.")

                layerMap[conf.name] = (layer as SingleTile).apply {
                    repeat(repeatX = isScrollingHorizontally && conf.repeat, repeatY = !isScrollingHorizontally && conf.repeat)

                    if (isScrollingHorizontally) {
                        // Attach the layer to the position on the parallax plane (either top or bottom border
                        // depending on attachBottomRight config)
                        val speedFactor =
                            parallaxPlaneSpeedFactor[layer.y.toInt() + (layer.height.toInt()
                                .takeIf { conf.attachBottomRight }
                                ?: 0)]
                        addUpdater {
                            // Calculate the offset for the inner scrolling of the layer
                            x += ((deltaX * speedFactor) + (config.selfSpeed * speedFactor)) * it.milliseconds
                        }
                    } else {
                        val speedFactor =
                            parallaxPlaneSpeedFactor[layer.x.toInt() + (layer.width.toInt()
                                .takeIf { conf.attachBottomRight }
                                ?: 0)]
                        addUpdater {
                            // Calculate the offset for the inner scrolling of the layer
                            x += ((deltaY * speedFactor) + (config.selfSpeed * speedFactor)) * it.milliseconds
                        }
                    }
                }
            }
        }
    }

    private fun constructLayer(
        layers: ImageData?,
        config: List<ParallaxLayerConfig>?,
        mode: ParallaxMode,
        smoothing: Boolean
    ) {
        if (layers == null || config == null || layers.frames.isEmpty()) return

        val imageData = imageDataView(layers, playing = false, smoothing = smoothing, repeating = true)

        for (conf in config) {
            val layer = imageData.getLayer(conf.name)
                ?: error("Could not find layer '${conf.name}' in ImageData. Check that name of layer in Aseprite matches the name in the parallax config.")

            layerMap[conf.name] = (layer as SingleTile).apply {
                repeat(repeatX = conf.repeatX, repeatY = conf.repeatY)

                if (!(conf.speedX.isNaN() && conf.speedY.isNaN() && conf.selfSpeedX.isNaN() && conf.selfSpeedY.isNaN())) {
                    // Prevent that one number is possibly NaN
                    val speedX = if (conf.speedX.isNaN()) 0.0 else conf.speedX
                    val speedY = if (conf.speedY.isNaN()) 0.0 else conf.speedY
                    val selfSpeedX = if (conf.selfSpeedX.isNaN()) 0.0 else conf.selfSpeedX
                    val selfSpeedY = if (conf.selfSpeedY.isNaN()) 0.0 else conf.selfSpeedY

                    // Do horizontal or vertical movement depending on parallax scrolling direction
                    when (mode) {
                        ParallaxMode.HORIZONTAL_PLANE -> {
                            addUpdater {
                                // Calculate the offset for the inner scrolling of the layer
                                x += (deltaX * speedX + selfSpeedX) * it.milliseconds
                            }
                        }
                        ParallaxMode.VERTICAL_PLANE -> {
                            addUpdater {
                                // Calculate the offset for the inner scrolling of the layer
                                y += (deltaY * speedY + selfSpeedY) * it.milliseconds
                            }
                        }
                        ParallaxMode.NO_PLANE -> {
                            addUpdater {
                                // Calculate the offset for the inner scrolling of the layer
                                x += (deltaX * speedX + selfSpeedX) * it.milliseconds
                                y += (deltaY * speedY + selfSpeedY) * it.milliseconds
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        // Only the base container for all view objects needs to be scaled
        this.scale = scale

        // First create background layers in the back
        constructLayer(data.backgroundLayers, data.config.backgroundLayers, data.config.mode, smoothing)

        // Then construct the two parallax planes with their attached layers
        if (data.config.mode != ParallaxMode.NO_PLANE) {
            constructParallaxPlane(
                data.parallaxPlane,
                data.attachedLayers,
                data.config.parallaxPlane,
                data.config.mode == ParallaxMode.HORIZONTAL_PLANE,
                smoothing
            )
            // Do horizontal or vertical movement depending on parallax scrolling direction
            if (data.config.mode == ParallaxMode.HORIZONTAL_PLANE) {
                // Move parallax plane inside borders
                addUpdater {
                    // Sanity check of diagonal movement - it has to be between 0.0 and 1.0
                    diagonal = diagonal.clamp(0.0, 1.0)
                    y = -(diagonal * (parallaxLayerSize - data.config.size.height))
                }
            } else {
                addUpdater {
                    diagonal = diagonal.clamp(0.0, 1.0)
                    x = -(diagonal * (parallaxLayerSize - data.config.size.width))
                }
            }
        }

        // Last construct the foreground layers on top
        constructLayer(data.foregroundLayers, data.config.foregroundLayers, data.config.mode, smoothing)
    }
}

suspend fun VfsFile.readParallaxDataContainer(
    config: ParallaxConfig,
    format: ImageFormat = ASE,
    atlas: MutableAtlas<Unit>? = null,
): ParallaxDataContainer {
    val props = ImageDecodingProps(this.baseName, extra = ExtraTypeCreate())
    return ParallaxDataContainer(
        config = config,
        backgroundLayers = if (config.backgroundLayers != null) {
            props.setExtra("layers", config.backgroundLayers.joinToString(separator = ",") { it.name })
            props.setExtra("disableSlicing", true)
            val out = format.readImage(this.readAsSyncStream(), props)
            if (atlas != null) out.packInMutableAtlas(atlas) else out
        } else null,
        foregroundLayers = if (config.foregroundLayers != null) {
            props.setExtra("layers", config.foregroundLayers.joinToString(separator = ",") { it.name })
            props.setExtra("disableSlicing", true)
            val out = format.readImage(this.readAsSyncStream(), props)
            if (atlas != null) out.packInMutableAtlas(atlas) else out
        } else null,
        attachedLayers = if (config.parallaxPlane?.attachedLayers != null) {
            props.setExtra("layers", config.parallaxPlane.attachedLayers.joinToString(separator = ",") { it.name })
            props.setExtra("disableSlicing", true)
            val out = format.readImage(this.readAsSyncStream(), props)
            if (atlas != null) out.packInMutableAtlas(atlas) else out
        } else null,
        parallaxPlane = if (config.parallaxPlane != null) {
            props.setExtra("layers", config.parallaxPlane.name)
            props.setExtra("disableSlicing", false)
            props.setExtra("useSlicePosition", true)
            val out = format.readImageContainer(this.readAsSyncStream(), props)
            if (atlas != null) out.packInMutableAtlas(atlas) else out
        } else null
    )
}

/**
 * This class contains all data which is needed by the [ParallaxDataView] to display the parallax view on the screen.
 * It stores the [ParallaxConfig] and all [ImageData] objects for the background, foreground and attached Layers. The
 * parallax plane is a sliced Aseprite image and therefore consists of a [ImageDataContainer] object.
 *
 * All these image data objects are read from one Aseprite file. Function [readParallaxDataContainerFromAseFile]
 * uses special [ImageDecodingProps] to control which details of the Aseprite file are read into which image
 * data object.
 */
data class ParallaxDataContainer(
    val config: ParallaxConfig,
    val backgroundLayers: ImageData?,
    val foregroundLayers: ImageData?,
    val attachedLayers: ImageData?,
    val parallaxPlane: ImageDataContainer?
)

/**
 * This is the main parallax configuration. It contains the virtual [size] of the parallax background which describes
 * the resolution in pixels which is displayed on the screen.
 * The [aseName] is the name of the aseprite file which is used for reading the image data.
 * (Currently it is not used. It will be used when reading the config from YAML file.)
 *
 * The parallax [mode] has to be one of the following enum values:
 * - [ParallaxMode.NO_PLANE]
 *   This type is used to set up a parallax background which will scroll repeatedly in X and Y direction. For this
 *   type of parallax effect it makes most sense to repeat the layers in X and Y direction (see [ParallaxLayerConfig]).
 *   The [parallaxPlane] object will not be used in this mode.
 * - [ParallaxMode.HORIZONTAL_PLANE]
 *   This is the default parallax mode. It is used to create an endless scrolling horizontal parallax background.
 *   Therefore, it makes sense to repeat the parallax layers in X direction in [ParallaxLayerConfig]. Also in this
 *   mode the [parallaxPlane] object is active which also can contain attached layers. If the virtual height of [size]
 *   is greater than the visible height on the screen then the view can be scrolled up and down with the diagonal
 *   property of [ParallaxDataView].
 * - [ParallaxMode.VERTICAL_PLANE]
 *   This mode is the same as [ParallaxMode.HORIZONTAL_PLANE] but in vertical direction.
 *
 * [backgroundLayers] and [foregroundLayers] contain the configuration for independent layers. They can be used with
 * all three parallax [mode]s. [parallaxPlane] is the configuration for the special parallax plane with attached
 * layers. Please look at [ParallaxLayerConfig] and [ParallaxPlaneConfig] data classes for more details.
 */
data class ParallaxConfig(
    val size: SizeInt,
    val aseName: String = "",
    val mode: ParallaxMode = ParallaxMode.HORIZONTAL_PLANE,
    val backgroundLayers: List<ParallaxLayerConfig>? = null,
    val parallaxPlane: ParallaxPlaneConfig? = null,
    val foregroundLayers: List<ParallaxLayerConfig>? = null
)

/**
 * This is the configuration of the parallax plane which can be used in [ParallaxMode.HORIZONTAL_PLANE] and
 * [ParallaxMode.VERTICAL_PLANE] modes. The parallax plane itself consists of a top and a bottom part. The top part
 * can be used to represent a ceiling (e.g. of a cave, building or sky). The bottom part is usually showing some ground.
 * The top part is the upper half of the Aseprite image. The bottom part is the bottom part. This is used to simulate
 * a central vanishing point in the resulting parallax effect.
 *
 * [name] has to be set to the name of the layer in the Aseprite which contains the image for the sliced stripes
 * of the parallax plane.
 * [speed] is the factor for scrolling the parallax plane relative to the game play field (which usually contains the
 * level map).
 * [selfSpeed] is the factor for scrolling the parallax plane continuously in a direction independently of the player
 * input.
 * [attachedLayers] contain the config for further layers which are "attached" to the parallax plane. These layers
 * will scroll depending on their positions on the parallay plane.
 */
data class ParallaxPlaneConfig(
    val name: String,
    val speed: Double = 1.0,
    val selfSpeed: Double = 0.0,
    val attachedLayers: List<ParallaxAttachedLayerConfig>? = null
)

/**
 * This is the configuration for layers which are attached to the parallax plane. These layers are moving depending
 * on its position on the parallax plane. They can be attached to the top or the bottom part of the parallax plane.
 *
 * [name] has to be set to the name of the layer in the used Aseprite file. The image on this layer will be taken for
 * the layer object.
 * [repeat] describes if the image of the layer object should be repeated in the scroll direction (horizontal or
 * vertical) of the parallax plane.
 *
 * When mode is set to [ParallaxMode.HORIZONTAL_PLANE] and [attachBottomRight] is set to false then the top
 * border of the layer is attached to the parallax plane. If [attachBottomRight] is set to true than the bottom
 * border is attached.
 * When mode is set to [ParallaxMode.VERTICAL_PLANE] and [attachBottomRight] is set to false then the left border of the
 * layer is attached to the parallax plane. If [attachBottomRight] is set to true than the right border is attached.
 */
data class ParallaxAttachedLayerConfig(
    val name: String,
    val repeat: Boolean = false,
    val attachBottomRight: Boolean = false  // on false -> attach top (if scrolling horizontal) / left (if scrolling vertical)
)

/**
 * This is the configuration for an independent parallax layer. Independent means that these layers are not attached
 * to the parallax plane. Their speed in X and Y direction can be configured independently by [speedX] and [speedY].
 * Also, their self-Speed [selfSpeedX] and [selfSpeedY] can be configured independently.
 *
 * [name] has to be set to the name of the layer in the used Aseprite file. The image on this layer will be taken for
 * the layer object.
 * [repeatX] and [repeatY] describes if the image of the layer object should be repeated in X and Y direction.
 * [speedX] and [speedY] are the factors for scrolling the parallax layer in X and Y direction relative to the game
 * play field.
 * [selfSpeedX] and [selfSpeedY] are the factors for scrolling the parallax layer in X and Y direction continuously
 * and independently of the player input.
 */
data class ParallaxLayerConfig(
    val name: String,
    val repeatX: Boolean = false,
    val repeatY: Boolean = false,
    val speedX: Double = Double.NaN,
    val speedY: Double = Double.NaN,
    val selfSpeedX: Double = Double.NaN,
    val selfSpeedY: Double = Double.NaN
)

enum class ParallaxMode {
    HORIZONTAL_PLANE, VERTICAL_PLANE, NO_PLANE
}
