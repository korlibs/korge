package samples

import korlibs.image.color.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.math.interpolation.*

class MainTransition : Scene() {
    override suspend fun SContainer.sceneMain() {
        val transition = TransitionView().addTo(this).xy(300, 100)
        transition.startNewTransition(SolidRect(100, 100, Colors.RED))
        transition.startNewTransition(SolidRect(100, 100, Colors.BLUE), MaskTransition(
            TransitionFilter.Transition.CIRCULAR
        ))
        transition.ratio = Ratio.HALF
        transition.filters(DropshadowFilter(shadowColor = Colors.PURPLE))

        solidRect(100, 100, Colors.GREEN).filters(DropshadowFilter(shadowColor = Colors.PURPLE))

        uiButton("HELLO").xy(200, 400).scale(4)
    }
}
