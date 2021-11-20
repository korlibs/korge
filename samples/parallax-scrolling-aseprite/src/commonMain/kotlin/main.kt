import com.soywiz.klock.Stopwatch
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korev.Key
import com.soywiz.korge.Korge
import com.soywiz.korge.scene.MaskTransition
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.scene.sceneContainer
import com.soywiz.korge.view.*
import com.soywiz.korge.view.animation.*
import com.soywiz.korge.view.filter.TransitionFilter
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.launch
import com.soywiz.korio.file.Vfs
import com.soywiz.korio.file.fullName
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.cancel
import com.soywiz.korio.lang.Closeable
import com.soywiz.korma.geom.SizeInt

const val scaleFactor = 3

suspend fun main() = Korge(width = 384 * scaleFactor, height = 216 * scaleFactor, bgcolor = Colors["#2b2b2b"]) {

    injector.mapPrototype { ParallaxScene() }

    val rootSceneContainer = sceneContainer()

    rootSceneContainer.changeTo<ParallaxScene>(
        transition = MaskTransition(transition = TransitionFilter.Transition.CIRCULAR, reversed = false, smooth = true),
        time = 0.5.seconds
    )
}

class ParallaxScene : Scene() {
    private val atlas = MutableAtlasUnit(1024, 1024)

    private lateinit var parallaxData: ParallaxDataContainer
    private lateinit var parallaxBackground: ParallaxDataView

    // This is the configuration for the parallax background which is shown with the parallaxBackground object below.
    // The names of the layers and the parallax plane must refer to the name of the layers in the Aseprite file "parallax_background.ase".
    // The Y-position of the attached layers in Aseprite will determine also the position of the layer in the parallax background.
    private val parallaxBackgroundConfig = ParallaxConfig(
        // Set the virtual size of the parallax background
        // It is usually equal to the virtual size of the main Korge view
        size = SizeInt(384, 216),
        // No background layers used in this example - following lines are here for reference
        //backgroundLayers = listOf(
        //    ParallaxLayerConfig("background_layer_1", repeatX = true, speedX = 0.0, selfSpeedX = 0.0),
        //    ParallaxLayerConfig("background_layer_2", repeatX = true, speedX = 0.0, selfSpeedX = 0.0)
        //),
        parallaxPlane = ParallaxPlaneConfig(
            "sliced_parallax_plane", speed = 0.8,
            attachedLayers = listOf(
                // The first three layers are attached to the top parallax plane
                ParallaxAttachedLayerConfig("top_attached_layer_1", repeat = true),
                ParallaxAttachedLayerConfig("top_attached_layer_2", repeat = true),
                ParallaxAttachedLayerConfig("top_attached_layer_3", repeat = true),
                // The next three layers are attached to the bottom parallax plane
                // For attaching the bottom border of the layers "attachBottomRight" needs to be set to true
                ParallaxAttachedLayerConfig("bottom_attached_layer_1", repeat = true, attachBottomRight = true),
                ParallaxAttachedLayerConfig("bottom_attached_layer_2", repeat = true, attachBottomRight = true),
                ParallaxAttachedLayerConfig("bottom_attached_layer_3", repeat = true, attachBottomRight = true)
            )
        ),
        // No foreground layers used in this example - following lines are here for reference
        //foregroundLayers = listOf(
        //    ParallaxLayerConfig("foreground_layer_1", repeatX = true, speedX = 0.0, selfSpeedX = 0.0),
        //    ParallaxLayerConfig("foreground_layer_2", repeatX = true, speedX = 0.0, selfSpeedX = 0.0)
        //)
    )

    private var reloading: Boolean = false  // Used for debouncing reload of config (in case the modification message comes twice from the system)

    private lateinit var resourcesWatcher: Closeable
    private var keyInput: Cancellable? = null

    override suspend fun Container.sceneInit() {
        val sw = Stopwatch().start()
        // Here the Aseprite file will be read by using the above configuration
        parallaxData = resourcesVfs["parallax_background.ase"].readParallaxDataContainer(parallaxBackgroundConfig, atlas = atlas)
        println("loaded resources in ${sw.elapsed}")
    }

    override suspend fun Container.sceneMain() {
        container {
            scale(scaleFactor)

            // Uncomment to show atlas for checking how the Aseprite layers and parallax plane will be stored in the generated atlas
            //image(atlas.bitmap) { scale(0.5); smoothing = false }

            // Create parallax background view which uses above initialized data container
            parallaxBackground = parallaxDataView(parallaxData) {
                // Add constant scrolling factor
                deltaX = 0.05
            }

            // This resource watcher will check if the Aseprite template file was changed. If yes then it will reload
            // the parallax background.
            // Thus this example can be used to live-preview when creating or changing own parallax backgrounds with
            // Aseprite. Try to change and save the parallax_background.ase file in Aseprite while this example is
            // running - it is super cool! :)
            resourcesWatcher = resourcesVfs[""].watch {
                if (it.kind == Vfs.FileEvent.Kind.MODIFIED) {
                    // Check name in resources folder
                    if (it.file.fullName.contains("parallax_background.ase") && !reloading) {
                        reloading = true  // save that reloading is in progress
                        print("Reloading ${it.file.fullName}... ")

                        // Save vertical scrolling state of background
                        val saveDiagonal = parallaxBackground.diagonal
                        val saveDelta = parallaxBackground.deltaX
                        removeChild(parallaxBackground)

                        launch {
                            // Give aseprite more time to finish writing the files
                            kotlinx.coroutines.delay(100)
                            // On reloading do not save into texture atlas otherwise it will overflow at some time
                            parallaxData = resourcesVfs["parallax_background.ase"].readParallaxDataContainer(parallaxBackgroundConfig, atlas = null)
                            parallaxBackground = parallaxDataView(parallaxData) {
                                diagonal = saveDiagonal
                                deltaX = saveDelta
                            }
                            // Guard period until reloading is activated again - this is used for debouncing watch messages
                            kotlinx.coroutines.delay(1000)
                            reloading = false
                            println("Finished")

                            // Restart keyboard controller with new background object -- needed so that after reloading
                            // the position of the parallax background can still be altered
                            keyInput?.cancel()
                            keyInput = stage?.controlWithKeyboard(parallaxBackground)
                        }
                    }
                }
            }

            keyInput = stage?.controlWithKeyboard(parallaxBackground)
        }
    }

    override suspend fun sceneDestroy() {
        resourcesWatcher.close()
        super.sceneDestroy()
    }
}

fun Stage.controlWithKeyboard(
    view: ParallaxDataView,
    up: Key = Key.UP,
    right: Key = Key.RIGHT,
    down: Key = Key.DOWN,
    left: Key = Key.LEFT,
) : Cancellable {
    return addUpdater { dt ->
        val speed = (dt / 16.0.milliseconds)
        val dx = if (keys[left]) -1.0 else if (keys[right]) +1.0 else 0.0
        val dy = if (keys[up]) -1.0 else if (keys[down]) +1.0 else 0.0
        if (dx != 0.0 || dy != 0.0) {
            // Uncomment for horizontal scrolling with keys "left" and "right"
            //view.deltaX = dx * speed * 0.1
            view.diagonal += dy * speed * 0.01
        } else {
            // Uncomment for horizontal scrolling with keys "left" and "right"
            //view.deltaX = 0.0
        }

    }
}
