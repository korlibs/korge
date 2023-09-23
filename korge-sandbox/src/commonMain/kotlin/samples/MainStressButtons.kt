package samples

import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.util.*

class MainStressButtons : Scene() {
    override suspend fun SContainer.sceneMain() {
        container {
            scaleAvg = .5
            uiVerticalStack {
                //for (row in 0 until 20) {
                for (row in 0 until 40) {
                    uiHorizontalStack {
                        for (col in 0 until 20) {
                            uiButton("%02d%02d".format(col, row))
                        }
                    }
                }
            }
        }
    }
}
