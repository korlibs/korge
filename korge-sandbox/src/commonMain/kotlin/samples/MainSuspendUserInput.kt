package samples

import com.soywiz.korge.input.mouse
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.ui.uiHorizontalStack
import com.soywiz.korge.ui.uiText
import com.soywiz.korge.ui.uiVerticalStack
import com.soywiz.korge.util.CancellableGroup
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SContainer
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.lang.cancel
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
