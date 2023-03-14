import com.soywiz.korge.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.interpolation.*
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
) {
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
        //Demo(::MainSDF),
        listOf(
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
            Demo(::MainTriangulation),
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

    comboBox = uiComboBox<Demo>(width = 200.0, items = (listOf(default) + all).distinctBy { it.name }.sortedBy { it.name }) {
        this.viewportHeight = 600
        this.onSelectionUpdate.add {
            //println(it)
            launchImmediately { setDemo(it.selectedItem!!) }
        }
    }.alignLeftToLeftOf(stage, padding = 8.0).alignTopToTopOf(stage, padding = 8.0)
    uiCheckBox(width = 300.0, text = "forceRenderEveryFrame", checked = views.forceRenderEveryFrame) {
        //x = 300.0
        alignLeftToRightOf(comboBox, padding = 16.0)
        alignTopToTopOf(comboBox, padding = 0.0)
        //alignRightToLeftOf(comboBox)
        onChange {
            views.forceRenderEveryFrame = it.checked
        }
    }
    comboBox.selectedItem = default
    comboBox.focusNoOpen()
    //setDemo(default)
}
