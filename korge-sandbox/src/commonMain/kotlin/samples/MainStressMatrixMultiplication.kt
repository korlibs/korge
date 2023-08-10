@file:OptIn(ExperimentalStdlibApi::class)

package samples

import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.*

class MainStressMatrixMultiplication : Scene() {
    override suspend fun SContainer.sceneMain() {
        container {
            views.debugViews = true
            val text = text("result")
            var N = 1_000
            var step = 0
            addUpdater {
                var sum = 0f
                for (n in 0 until N) {
                    val mat = this.localMatrix * this@sceneMain.localMatrix * this@MainStressMatrixMultiplication.stage.localMatrix * Matrix().scaled(n.toFloat()).translated(xD.toFloat() * 10f, yD.toFloat() * -20f)
                    sum += mat.a + mat.b + mat.c + mat.d + mat.tx + mat.ty
                }
                text.text = "result: $sum, $step"
                step++
            }
            uiVerticalStack {
                xy(100, 100)
                val group = UIRadioButtonGroup()
                val b1K = uiRadioButton(text = "1K", group = group)
                val b10K = uiRadioButton(text = "10K", group = group)
                val b100K = uiRadioButton(text = "100K", group = group)
                val b1M = uiRadioButton(text = "1M", group = group)
                val b10M = uiRadioButton(text = "10M", group = group)
                group.onChanged {
                    when (it) {
                        b1K -> N = 1_000
                        b10K -> N = 10_000
                        b100K -> N = 100_000
                        b1M -> N = 1_000_000
                        b10M -> N = 10_000_000
                    }
                }
                b1K.checked = true
            }
        }
    }
}
