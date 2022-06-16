import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.scene.sceneContainer
import com.soywiz.korge.ui.uiComboBox
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.mix
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.lang.portableSimpleName
import samples.*

suspend fun main() = Korge(
    bgcolor = Colors.DARKCYAN.mix(Colors.BLACK, 0.8),
    clipBorders = false,
    //scaleMode = ScaleMode.EXACT,
    //debug = true,
    multithreaded = true,
    //debugAg = true,
) {
    demoSelector(
        Demo(::MainTrimmedAtlas),
        listOf(
            Demo(::MainSWF),
            Demo(::MainSpine),
            Demo(::MainDragonbones),
            Demo(::MainMutableAtlasTest),
            Demo(::TerminalEmulatorMain),
            Demo(::ParticlesMain),
            Demo(::BezierSample),
            Demo(::MainEditor),
            Demo(::Bunnymark),
            Demo(::MainKorviSample),
            Demo(::MainFiltersSample),
            Demo(::MainTextMetrics),
            Demo(::MainRenderText),
            Demo(::MainVectorRendering),
            Demo(::MainFilterScale),
            Demo(::MainExifTest),
            Demo(::MainColorTransformFilter),
            Demo(::MainMipmaps),
            Demo(::MainCustomSolidRectShader),
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

    uiComboBox(width = 300.0, items = listOf(default) + all) {
        this.onSelectionUpdate.add {
            println(it)
            launchImmediately { setDemo(it.selectedItem!!) }
        }
    }
    setDemo(default)
}
