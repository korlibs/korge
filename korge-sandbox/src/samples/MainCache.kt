package samples

import korlibs.image.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.korge.scene.*
import korlibs.korge.time.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.interpolation.*
import korlibs.math.random.*
import korlibs.time.*
import kotlin.random.*

//class MainCache : ScaledScene(512, 512) {
class MainCache : Scene() {
    override suspend fun SContainer.sceneMain() {
        //val cached = CachedContainer().addTo(this)
        //val cached = container {  }
        val cached = cachedContainer {  }
        val random = Random(0L)
        for (n in 0 until 100_000) {
            cached.solidRect(2, 2, random[Colors.RED, Colors.BLUE]).xy(2 * (n % 300), 2 * (n / 300))
        }
        uiHorizontalStack {
            uiButton("Cached").clicked {
                cached.cache = !cached.cache
                it.text = if (cached.cache) "Cached" else "Uncached"
            }
            uiText("children=${cached.numChildren}")
        }

        uiHorizontalStack {
            this.position(315, 0)
            scale(2)

            uiScrollable(size = Size(100, 100)) {
                it.sampleTextBlock(RichTextData("Default Scaling: Default", font = DefaultTtfFontAsBitmap))
            }

            uiScrollable(size = Size(100, 100)) {
                it.renderQuality = Quality.HIGH
                it.sampleTextBlock(RichTextData("Expensive Scaling: Opt-in", font = DefaultTtfFontAsBitmap))
            }

            uiScrollable(size = Size(100, 100)) {
                it.renderQuality = Quality.LOW
                it.sampleTextBlock(RichTextData("Cheap Scaling: Opt-in", font = DefaultTtfFontAsBitmap))
            }
        }

        interval(1.seconds) {
            for (n in 0 until 2000) {
                cached.getChildAt(50_000 + n).colorMul = random[Colors.RED, Colors.BLUE].mix(Colors.WHITE, 0.3.toRatio())
            }
            println(cached.getChildAt(50000)._invalidateNotifier)
        }


        //timeout(1.seconds) {
        //    rect.color = Colors.BLUE
        //}
    }

    //override suspend fun SContainer.sceneInit() {
    //    setVirtualSize(Korge.DEFAULT_WINDOW_SIZE / 2)
    //}
    //override suspend fun sceneAfterDestroy() {
    //    super.sceneAfterDestroy()
    //    setVirtualSize(Korge.DEFAULT_WINDOW_SIZE)
    //}
    //private fun setVirtualSize(size: Size) {
    //    views.setVirtualSize(size.width.toIntCeil(), size.height.toIntCeil())
    //}


    private fun Container.sampleTextBlock(richTextData: RichTextData): TextBlock {
        return textBlock(align = TextAlignment.MIDDLE_CENTER, size = Size(100, 100)) {
            text = richTextData
            fill = Colors.WHITE
        }
    }
}
