import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.scene.sceneContainer
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.ui.uiComboBox
import com.soywiz.korge.ui.uiText
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.position
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.mix
import com.soywiz.korio.annotations.Keep
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.lang.portableSimpleName
import samples.*
import samples.asteroids.MainAsteroids
import samples.fleks.*
import samples.connect4.*
import samples.minesweeper.MainMineSweeper
import samples.pong.MainPong
import samples.tictactoeswf.MainTicTacToeSwf

suspend fun main() = Korge(
    bgcolor = Colors.DARKCYAN.mix(Colors.BLACK, 0.8),
    clipBorders = false,
    //scaleMode = ScaleMode.EXACT,
    //debug = true,
    multithreaded = true,
    //debugAg = true,
) {
    //uiButton("HELLO WORLD!", width = 300.0).position(100, 100); return@Korge

    demoSelector(
        //Demo(::MainVector),
        Demo(::MainSpine),
        listOf(
            Demo(::MainVector),
            Demo(::MainText),
            Demo(::MainAtlas),
            Demo(::MainBunnysSlow),
            Demo(::MainOnScreenController),
            Demo(::MainScenes),
            Demo(::MainKTree),
            Demo(::MainLipSync),
            Demo(::MainInput),
            Demo(::MainGestures),
            Demo(::MainFilters),
            Demo(::MainCoroutine),
            Demo(::MainVideo),
            Demo(::MainTicTacToeSwf),
            Demo(::MainPong),
            Demo(::MainUI),
            Demo(::MainLua),
            Demo(::MainOldMask),
            Demo(::MainNinePatch),
            Demo(::MainTweens),
            Demo(::MainTriangulation),
            Demo(::MainShapes),
            Demo(::MainSpriteAnim),
            Demo(::MainMineSweeper),
            Demo(::MainHelloWorld),
            Demo(::MainFlag),
            Demo(::MainAsteroids),
            Demo(::MainBox2d),
            Demo(::MainEmojiColrv1),
            Demo(::MainRotatedAtlas),
            Demo(::MainConnect4),
            Demo(::MainSWF),
            Demo(::MainSpine),
            Demo(::MainDragonbones),
            Demo(::MainMutableAtlasTest),
            Demo(::MainTerminalEmulator),
            Demo(::MainParticles),
            Demo(::MainBezierSample),
            Demo(::MainEditor),
            Demo(::MainAnimations),
            Demo(::MainBunnymark),
            Demo(::MainKorviSample),
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
            Demo(::MainStage3d),
            Demo(::MainFleksSample),
            Demo(::MainBlur),
            Demo(::MainFiltersRenderToBitmap),
            Demo(::MainColorPicker),
            Demo(::MainMasks),
            Demo(::MainHaptic),
            Demo(::MainSkybox),
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
            Demo(::MainVampire),
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
            Demo(::MainTilemapWithScroll),
            Demo(::MainTiledBackground),
            Demo(::MainAseprite),
            Demo(::MainAseParallaxSample),
        )
    )
}

class Demo(val sceneBuilder: () -> Scene, val name: String = sceneBuilder()::class.portableSimpleName.removePrefix("Main")) {
    override fun toString(): String = name
}

suspend fun Stage.demoSelector(default: Demo, all: List<Demo>) {
    val container = sceneContainer(width = width, height = height - 32.0) { }.xy(0, 32)

    suspend fun setDemo(demo: Demo?) {
        //container.removeChildren()
        if (demo != null) {
            container.changeTo({ injector ->
                demo.sceneBuilder().also { it.init(injector) }
            })
        }
    }

    uiComboBox(width = 300.0, items = (listOf(default) + all).distinctBy { it.name }.sortedBy { it.name }) {
        this.viewportHeight = 600
        this.onSelectionUpdate.add {
            println(it)
            launchImmediately { setDemo(it.selectedItem!!) }
        }
    }
    setDemo(default)
}
