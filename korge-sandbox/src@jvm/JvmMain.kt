import korlibs.io.async.*
import korlibs.korge.*
import samples.*

object JvmMain {
  @JvmStatic
  fun main(args: Array<String>) = runBlockingNoJs {
    Korge(
        windowSize = Korge.DEFAULT_WINDOW_SIZE,
        backgroundColor = DEFAULT_KORGE_BG_COLOR,
        displayMode = KorgeDisplayMode.CENTER_NO_CLIP,
        debug = false,
        forceRenderEveryFrame = true
    ) {
        demoSelector(
            Demo(::MainRenderImagesJvmNative),
            listOf(
                Demo(::MainRenderImagesJvmNative)
            )
        )
    }
  }
}
