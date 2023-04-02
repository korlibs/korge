
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.io.lang.*
import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.interpolation.*
import samples.*
import samples.asteroids.*
import samples.connect4.*
import samples.minesweeper.*
import samples.pong.*

val DEFAULT_KORGE_BG_COLOR = Colors.DARKCYAN.mix(Colors.BLACK, 0.8.toRatio())

//class MyScene : Scene() {
//    override suspend fun SContainer.sceneMain() {
//        solidRect(100, 100, Colors.BLUE)
//    }
//}
//
//suspend fun main() = Korge(
//    windowSize = Korge.DEFAULT_WINDOW_SIZE,
//    backgroundColor = DEFAULT_KORGE_BG_COLOR,
//    displayMode = KorgeDisplayMode.DEFAULT.copy(clipBorders = false),
//    debug = false,
//    multithreaded = true,
//    forceRenderEveryFrame = false,
//    mainSceneClass = MyScene::class,
//    configInjector = {
//        mapPrototype { MyScene() }
//    }
//).start()

suspend fun main() = Korge(
    windowSize = Korge.DEFAULT_WINDOW_SIZE,
    backgroundColor = DEFAULT_KORGE_BG_COLOR,
    displayMode = KorgeDisplayMode.CENTER_NO_CLIP,
    debug = false,
    forceRenderEveryFrame = false
).start {
    //solidRect(200, 200, Colors.RED); return@start
    //solidRect(50, 50, Colors.GREEN).xy(50, 50)
    //    .filters(WaveFilter(amplitudeX = 15, amplitudeY = 10, crestDistanceX = 25.0, crestDistanceY = 10.0).also { filter ->
    //        addUpdater { filter.time += it }
    //        invalidateRender()
    //    }).also { return@start }
    //lateinit var circle1: Circle
    //lateinit var circle2: Circle
    //val container = container {
    //    xy(200, 200)
    //    scale(1.2)
    //    circle1 = circle(128.0, Colors.RED)
    //        .scale(0.75)
    //        .anchor(Anchor.MIDDLE_CENTER)
    //    circle2 = circle(64.0, Colors.BLUE)
    //        .scale(1.5)
    //        .anchor(Anchor.MIDDLE_CENTER)
    //}
    //circle1.xy(167, 100)
    //circle2.xy(0, 0)
    //return@start
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
        //Demo(::MainMasks),
        Demo(::MainShape2dScene),
        //Demo(::MainStressMatrixMultiplication),
        //Demo(::MainSDF),
        listOf(
            Demo(::MainShape2dScene),
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
