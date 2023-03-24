package samples

import korlibs.korge.input.mouse
import korlibs.korge.scene.Scene
import korlibs.korge.ui.uiButton
import korlibs.korge.ui.uiHorizontalStack
import korlibs.korge.ui.uiText
import korlibs.korge.ui.uiVerticalStack
import korlibs.korge.view.Container
import korlibs.korge.view.SContainer
import korlibs.io.async.launchImmediately
import korlibs.io.lang.*
import kotlinx.coroutines.CompletableDeferred

class MainSuspendUserInput : Scene() {
    override suspend fun SContainer.sceneMain() {
        launchImmediately {
            uiVerticalStack(adjustSize = false) {
                while (true) {
                    val result = waitForMouseEvents()
                    uiText("Your response was $result")
                }
            }
        }
    }

    suspend fun Container.waitForMouseEvents(): Boolean {
        val cancellableContainer = CancellableGroup()
        try {
            val completed = CompletableDeferred<Boolean>()
            val stack = uiHorizontalStack {
                cancellableContainer += uiButton("YES").mouse.onClickCloseable { completed.complete(true) }
                cancellableContainer += uiButton("NO").mouse.onClickCloseable { completed.complete(false) }
            }
            cancellableContainer += { stack.removeFromParent() }
            return completed.await()
        } finally {
            cancellableContainer.cancel()
        }
    }
}
