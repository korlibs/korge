package korlibs.korge.gradle.korgefleks


/**
 * This is the main parallax configuration.
 *
 * The parallax [mode] has to be one of the following enum values:
 * - [NO_PLANE]
 *   This type is used to set up a parallax background which will scroll repeatedly in X and Y direction. For this
 *   type of parallax effect it makes most sense to repeat the layers in X and Y direction (see [ParallaxLayerInfo]).
 *   The [parallaxPlane] object will not be used in this mode.
 * - [HORIZONTAL_PLANE]
 *   This is the default parallax mode. It is used to create an endless scrolling horizontal parallax background.
 *   Therefore, it makes sense to repeat the parallax layers in X direction in [ParallaxLayerInfo]. Also in this
 *   mode the [parallaxPlane] object is active which also can contain attached layers. If the virtual height of [size]
 *   is greater than the visible height on the screen then the view can be scrolled up and down with the diagonal
 *   property of [ParallaxDataView].
 * - [VERTICAL_PLANE]
 *   This mode is the same as [HORIZONTAL_PLANE] but in vertical direction.
 *
 * [backgroundLayers] and [foregroundLayers] contain the configuration for independent layers. They can be used with
 * all three parallax [mode]s.
 *
 * [parallaxPlane] is the configuration for the special parallax plane with attached
 * layers. Please look at [ParallaxLayerInfo] and [ParallaxPlaneInfo] data classes for more details.
 */
data class ParallaxInfo(
    val name: String,
    val parallaxWidth: Int = 0,   // width of the parallax effect used in VERTICAL_PLANE mode for horizontal scrolling
    val parallaxHeight: Int = 0,  // height of the parallax effect used in HORIZONTAL_PLANE mode for vertical scrolling
    val mode: Mode = Mode.NO_PLANE,

    val backgroundLayers: List<ParallaxLayerInfo> = emptyList(),
    val foregroundLayers: List<ParallaxLayerInfo> = emptyList(),

    val parallaxPlane: ParallaxPlaneInfo? = null
) {
    enum class Mode {
        HORIZONTAL_PLANE, VERTICAL_PLANE, NO_PLANE
    }

    /**
     * This is the configuration for an independent parallax layer. Independent means that these layers are not attached
     * to the parallax plane. Their speed in X and Y direction can be configured by [speedFactor].
     * Their self-Speed [selfSpeedX] and [selfSpeedY] can be configured independently.
     *
     * [repeatX] and [repeatY] describes if the image of the layer object should be repeated in X and Y direction.
     * [speedFactor] is the factors for scrolling the parallax layer in X and Y direction relative to the game
     * play field.
     * [selfSpeedX] and [selfSpeedY] are the factors for scrolling the parallax layer in X and Y direction continuously
     * and independently of the player input.
     */
    data class ParallaxLayerInfo(
        val name: String,
        val targetX: Int = 0,  // offset from the left corner of the parallax background image used in VERTICAL_PLANE mode
        val targetY: Int = 0,  // offset from the top corner of the parallax background image used in HORIZONTAL_PLANE mode

        val repeatX: Boolean = false,
        val repeatY: Boolean = false,
        val centerX: Boolean = false,  // Center the layer in the parallax background image
        val centerY: Boolean = false,
        val speedFactor: Float? = null,  // It this is null than no movement is applied to the layer
        val selfSpeedX: Float = 0f,
        val selfSpeedY: Float = 0f
    )

    /**
     * This is the configuration of the parallax plane which can be used in [HORIZONTAL_PLANE] and
     * [VERTICAL_PLANE] modes. The parallax plane itself consists of a top and a bottom part. The top part
     * can be used to represent a ceiling (e.g. of a cave, building or sky). The bottom part is usually showing some ground.
     * The top part is the upper half of the Aseprite image. The bottom part is the bottom half of the image. This is used
     * to simulate a central vanishing point in the resulting parallax effect.
     *
     * [speedFactor] is the factor for scrolling the parallax plane relative to the game play field (which usually contains the
     * level map).
     * [selfSpeed] is the amount of velocity for scrolling the parallax plane continuously in a direction independently of the player
     * input.
     * [attachedLayersFront] contains the config for further layers which are "attached" on top of the parallax plane.
     * [attachedLayersRear] contains the config for further layers which are "attached" below the parallax plane.
     * Both attached layer types will scroll depending on their position on the parallax plane.
     */
    data class ParallaxPlaneInfo(
        val name: String,
        val speedFactor: Float = 1f,
        val selfSpeed: Float = 0f,
        val topAttachedLayers: List<ParallaxAttachedLayerInfo> = emptyList(),
        val bottomAttachedLayers: List<ParallaxAttachedLayerInfo> = emptyList()
    )

    /**
     * This is the configuration for layers which are attached to the parallax plane. These layers are moving depending
     * on its position on the parallax plane. They can be attached to the top or the bottom part of the parallax plane.
     *
     * [repeat] describes if the image of the layer object should be repeated in the scroll direction (horizontal or
     * vertical) of the parallax plane.
     *
     * When mode is set to [HORIZONTAL_PLANE] and [attachBottomRight] is set to false then the top
     * border of the layer is attached to the parallax plane. If [attachBottomRight] is set to true than the bottom
     * border is attached.
     * When [mode][ParallaxConfig.Mode] is [VERTICAL_PLANE] and [attachBottomRight] is false then the left border
     * of the layer will be attached to the parallax plane. If [attachBottomRight] is true then the right border
     * will be attached.
     */
    data class ParallaxAttachedLayerInfo(
        val name: String,
        val attachIndex: Int,
        val repeat: Boolean = true,
        val attachBottomRight: Boolean = true
    )
}
