import com.soywiz.klock.seconds
import com.soywiz.korge.time.timeout
import com.soywiz.korge.ui.uiGridFill
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.text
import com.soywiz.korge.view.xy

suspend fun Stage.mainTextureIssue() {
    // Press F7 after 1 + 0.3 seconds (so the texture GC has been executed), this will trigger a new program creation

    text("HELLO WORLD!").also {
        timeout(0.3.seconds) { it.removeFromParent() }
    }

    val N = 1
    //val N = 3
    for (y in 0 until N) {
        for (x in 0 until N) {
            text("($x, $y)").xy(100 + x * 128, 100 + y * 16)
        }
    }
}
