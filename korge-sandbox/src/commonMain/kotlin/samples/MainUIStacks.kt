package samples

import korlibs.image.color.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.interpolation.*

class MainUIStacks : Scene() {
    override suspend fun SContainer.sceneMain() {
        lateinit var hs: UIHorizontalStack
        lateinit var hs2: UIHorizontalStack
        lateinit var hs3: UIHorizontalStack
        uiVerticalStack {
            hs = uiHorizontalStack {
                solidRect(100, 100, Colors.BLUE)
                solidRect(100, 120, Colors.RED)
                solidRect(100, 100, Colors.GREEN)
                solidRect(100, 100, Colors.YELLOW)
            }
            uiHorizontalStack {
                solidRect(100, 100, Colors.YELLOW)
                solidRect(100, 100, Colors.GREEN)
                solidRect(100, 100, Colors.RED)
                solidRect(100, 100, Colors.BLUE)
            }
            hs2 = uiHorizontalStack {
            }
            hs3 = uiHorizontalStack {
            }
        }

        hs.forcedHeight = 32.0
        hs2.solidRect(100, 100, Colors.BLUE)
        hs3.solidRect(100, 100, Colors.YELLOW)

        uiGridFill(rows = 3, cols = 2) {
            val N = 6
            //for (n in 0 until 32) {
            for (n in 0 until N) {
                solidRect(100, 100, (n.toFloat() / (N - 1)).toRatio().interpolate(Colors.BLUE, Colors.RED))
            }
        }.xy(500, 32)
    }
}
