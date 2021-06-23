import com.soywiz.korge.*
import com.soywiz.korge.awt.*
import com.soywiz.korge.editor.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*

suspend fun main() = Korge(scaleMode = ScaleMode.NO_SCALE, scaleAnchor = Anchor.TOP_LEFT, clipBorders = false) {
    val component = injector.get<ViewsDebuggerComponent>()
    ktreeEditorKorge(stage, component.actions, views, BaseKorgeFileToEdit(MemoryVfsMix(mapOf("test.ktree" to "<ktree></ktree>"))["test.ktree"]), { })
}
