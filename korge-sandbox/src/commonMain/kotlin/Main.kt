import com.soywiz.korge.Korge
import com.soywiz.korge.input.MouseEvents
import com.soywiz.korge.input.mouse
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.scene.sceneContainer
import com.soywiz.korge.ui.UIButton
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.ui.uiComboBox
import com.soywiz.korge.ui.uiHorizontalStack
import com.soywiz.korge.ui.uiText
import com.soywiz.korge.ui.uiVerticalStack
import com.soywiz.korge.util.CancellableGroup
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.text
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.mix
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.async.waitOneAsync
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.cancel
import com.soywiz.korio.lang.portableSimpleName
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import samples.MainAnimations
import samples.MainArc
import samples.MainAseParallaxSample
import samples.MainAseprite
import samples.MainAtlas
import samples.MainBVH
import samples.MainBezier
import samples.MainBezierSample
import samples.MainBlending
import samples.MainBlur
import samples.MainBmpFont
import samples.MainBox2d
import samples.MainBunnymark
import samples.MainBunnysSlow
import samples.MainCircleColor
import samples.MainCircles
import samples.MainClipping
import samples.MainColorPicker
import samples.MainColorTransformFilter
import samples.MainCoroutine
import samples.MainCustomSolidRectShader
import samples.MainDpi
import samples.MainDraggable
import samples.MainDragonbones
import samples.MainEasing
import samples.MainEditor
import samples.MainEmoji
import samples.MainEmojiColrv1
import samples.MainExifTest
import samples.MainFilterScale
import samples.MainFilterSwitch
import samples.MainFilters
import samples.MainFiltersRenderToBitmap
import samples.MainFiltersSample
import samples.MainFlag
import samples.MainGestures
import samples.MainGifAnimation
import samples.MainGpuVectorRendering
import samples.MainGpuVectorRendering2
import samples.MainGpuVectorRendering3
import samples.MainHaptic
import samples.MainHelloWorld
import samples.MainImageTrace
import samples.MainInput
import samples.MainKTree
import samples.MainKorviSample
import samples.MainLipSync
import samples.MainLua
import samples.MainMasks
import samples.MainMipmaps
import samples.MainMutableAtlasTest
import samples.MainNinePatch
import samples.MainOldMask
import samples.MainOnScreenController
import samples.MainParticles
import samples.MainPolyphonic
import samples.MainRenderText
import samples.MainRotateCircle
import samples.MainRotatedAtlas
import samples.MainRotatedTexture
import samples.MainSWF
import samples.MainScenes
import samples.MainShapes
import samples.MainSkybox
import samples.MainSound
import samples.MainSpine
import samples.MainSpriteAnim
import samples.MainSprites10k
import samples.MainStage3d
import samples.MainStrokesExperiment
import samples.MainStrokesExperiment2
import samples.MainStrokesExperiment3
import samples.MainSvgAnimation
import samples.MainTerminalEmulator
import samples.MainText
import samples.MainTextInput
import samples.MainTextMetrics
import samples.MainTextureIssue
import samples.MainTiledBackground
import samples.MainTilemapTest
import samples.MainTilemapWithScroll
import samples.MainTransition
import samples.MainTriangulation
import samples.MainTrimmedAtlas
import samples.MainTweenPoint
import samples.MainTweens
import samples.MainUI
import samples.MainUIImageTester
import samples.MainUITreeView
import samples.MainVampire
import samples.MainVector
import samples.MainVectorFill
import samples.MainVectorRendering
import samples.MainVideo
import samples.MainXM
import samples.MainZIndex
import samples.MainSuspendUserInput
import samples.asteroids.MainAsteroids
import samples.connect4.MainConnect4
import samples.fleks.MainFleksSample
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
    //solidRect(100, 100, Colors.RED).xy(300, 300).filters(BlurFilter()); return@Korge

    demoSelector(
        //Demo(::MainSvgAnimation),
        //Demo(::MainGpuVectorRendering),
        //Demo(::MainSpine),
        //Demo(::MainDragonbones),
        //Demo(::MainBlur),
        //Demo(::MainXM),
        Demo(::MainTriangulation),
        //Demo(::MainBlending),
        listOf(
            Demo(::MainTextInput),
            Demo(::MainBlending),
            Demo(::MainXM),
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
