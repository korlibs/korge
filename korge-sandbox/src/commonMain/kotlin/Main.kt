import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.math.interpolation.*
import samples.*
import samples.asteroids.*
import samples.connect4.*
import samples.minesweeper.*
import samples.pong.*

val DEFAULT_KORGE_BG_COLOR = Colors.DARKCYAN.mix(Colors.BLACK, 0.8.toRatio())

suspend fun main() = Korge(
    windowSize = Korge.DEFAULT_WINDOW_SIZE,
    bgcolor = DEFAULT_KORGE_BG_COLOR,
    //bgcolor = Colors.WHITE,
    clipBorders = false,
    //scaleMode = ScaleMode.EXACT,
    //debug = true,
    debug = false,
    //debugAg = true,
    multithreaded = true,
    forceRenderEveryFrame = false // Newly added optimization!
    //forceRenderEveryFrame = true
    //debugAg = true,
).start {
    //image(resourcesVfs["korge.png"].readBitmap()); text("hello world!", textSize = 64.0, color = Colors.RED); return@Korge
    //text("hello world!", textSize = 64.0); return@Korge

    //graphics(renderer = GraphicsRenderer.CPU) {

    //graphics(renderer = GraphicsRenderer.SYSTEM) {
    //    fill(createSweepGradient(100, 100).add(Colors.RED, Colors.GREEN, Colors.BLUE)) {
    //    //fill(createPattern(bmp, transform = MMatrix().translate(100, 100))) {
    //        //this.rect(100, 100, 200, 200)
    //        this.rect(50, 50, 200, 200)
    //    }
    //}
    //return@Korge
    //sceneContainer(views).changeTo({ MainGifAnimation() }); return@Korge
    //sceneContainer(views).changeTo({ MainStressButtons() }); return@Korge
    //sceneContainer(views).changeTo({ MainTransitionFilter() }); return@Korge

    demoSelector(
        //Demo(::MainGpuVectorRendering),
        //Demo(::MainColorTransformFilter),
        Demo(::MainMasks),
        //Demo(::MainStressMatrixMultiplication),
        //Demo(::MainSDF),
        listOf(
            Demo(::MainStressMatrixMultiplication),
            Demo(::MainStressButtons),
            Demo(::MainVectorNinePatch),
            Demo(::MainGraphicsText),
            Demo(::MainImageOrientationTest),
            Demo(::MainCache),
            Demo(::MainSDF),
            Demo(::MainTextInput),
            Demo(::MainBlending),
            Demo(::MainVector),
            Demo(::MainText),
            Demo(::MainAtlas),
            Demo(::MainOnScreenController),
            Demo(::MainScenes),
            Demo(::MainInput),
            Demo(::MainGestures),
            Demo(::MainFilters),
            Demo(::MainCoroutine),
            Demo(::MainPong),
            Demo(::MainUI),
            Demo(::MainNinePatch),
            Demo(::MainTweens),
            Demo(::MainShapes),
            Demo(::MainSpriteAnim),
            Demo(::MainMineSweeper),
            Demo(::MainHelloWorld),
            Demo(::MainFlag),
            Demo(::MainAsteroids),
            Demo(::MainEmojiColrv1),
            Demo(::MainRotatedAtlas),
            Demo(::MainConnect4),
            Demo(::MainMutableAtlasTest),
            Demo(::MainTerminalEmulator),
            Demo(::MainParticles),
            Demo(::MainBezierSample),
            Demo(::MainEditor),
            Demo(::MainAnimations),
            Demo(::MainBunnymark),
            Demo(::MainFiltersSample),
            Demo(::MainTextMetrics),
            Demo(::MainRenderText),
            Demo(::MainVectorRendering),
            Demo(::MainFilterScale),
            Demo(::MainExifTest),
            Demo(::MainColorTransformFilter),
            Demo(::MainMipmaps),
            Demo(::MainBmpFont),
            Demo(::MainPolyphonic),
            Demo(::MainSprites10k),
            Demo(::MainCustomSolidRectShader),
            Demo(::MainBlur),
            Demo(::MainFiltersRenderToBitmap),
            Demo(::MainColorPicker),
            Demo(::MainMasks),
            Demo(::MainHaptic),
            Demo(::MainDraggable),
            Demo(::MainGifAnimation),
            Demo(::MainTransition),
            Demo(::MainTilemapTest),
            Demo(::MainTextureIssue),
            Demo(::MainClipping),
            Demo(::MainTweenPoint),
            Demo(::MainEasing),
            Demo(::MainVectorFill),
            Demo(::MainFilterSwitch),
            Demo(::MainSvgAnimation),
            Demo(::MainArc),
            Demo(::MainStrokesExperiment),
            Demo(::MainStrokesExperiment2),
            Demo(::MainStrokesExperiment3),
            Demo(::MainZIndex),
            Demo(::MainDpi),
            Demo(::MainBezier),
            Demo(::MainUITreeView),
            Demo(::MainUIImageTester),
            Demo(::MainCircles),
            Demo(::MainEmoji),
            Demo(::MainBVH),
            Demo(::MainImageTrace),
            Demo(::MainRotateCircle),
            Demo(::MainTrimmedAtlas),
            Demo(::MainRotatedTexture),
            Demo(::MainCircleColor),
            Demo(::MainGpuVectorRendering),
            Demo(::MainGpuVectorRendering2),
            Demo(::MainGpuVectorRendering3),
            Demo(::MainSound),
            Demo(::MainTiledBackground),
            Demo(::MainAseprite),
        )
    )
}

class Demo(val sceneBuilder: () -> Scene, val name: String = sceneBuilder()::class.portableSimpleName.removePrefix("Main")) {
    override fun toString(): String = name
}

suspend fun Stage.demoSelector(default: Demo, all: List<Demo>) {
    val container = sceneContainer(width = width, height = height - 48.0) { }.xy(0, 48)

    lateinit var comboBox: UIComboBox<Demo>

    suspend fun setDemo(demo: Demo?) {
        //container.removeChildren()
        if (demo != null) {
            comboBox.selectedItem = demo
            views.clearColor = DEFAULT_KORGE_BG_COLOR
            container.changeTo({ injector ->
                demo.sceneBuilder().also { it.init(injector) }
            })
        }
    }

    uiHorizontalStack(padding = 8.0) {
        alignLeftToLeftOf(this@demoSelector.stage, padding = 8.0).alignTopToTopOf(this@demoSelector.stage, padding = 8.0)
        comboBox = uiComboBox<Demo>(width = 200.0, items = (listOf(default) + all).distinctBy { it.name }.sortedBy { it.name }) {
            this.viewportHeight = 600
            this.onSelectionUpdate.add {
                //println(it)
                launchImmediately { setDemo(it.selectedItem!!) }
            }
        }
        uiCheckBox(width = 200.0, text = "forceRenderEveryFrame", checked = views.forceRenderEveryFrame) {
            onChange { views.forceRenderEveryFrame = it.checked }
        }
        uiCheckBox(width = 150.0, text = "toggleDebug", checked = views.debugViews) {
            onChange { views.debugViews = it.checked }
        }
    }
    comboBox.selectedItem = default
    comboBox.focusNoOpen()
    //setDemo(default)
}
