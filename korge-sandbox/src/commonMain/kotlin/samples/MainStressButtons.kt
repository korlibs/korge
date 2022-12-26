package samples

import com.soywiz.korge.scene.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*

class MainStressButtons : Scene() {
    override suspend fun SContainer.sceneMain() {
        container {
            scale = 0.5
            uiVerticalStack {
                //for (row in 0 until 20) {
                for (row in 0 until 40) {
                    uiHorizontalStack {
                        for (col in 0 until 20) {
                            uiButton("Button%02d%02d".format(col, row))
                        }
                    }
                }
            }
        }
    }
}
