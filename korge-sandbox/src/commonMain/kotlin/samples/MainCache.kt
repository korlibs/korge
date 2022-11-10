package samples

import com.soywiz.klock.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.time.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korma.random.*
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

        interval(1.seconds) {
            for (n in 0 until 2000) {
                cached.getChildAt(50_000 + n).colorMul = random[Colors.RED, Colors.BLUE].mix(Colors.WHITE, 0.3)
            }
        }
        //timeout(1.seconds) {
        //    rect.color = Colors.BLUE
        //}
    }
}
