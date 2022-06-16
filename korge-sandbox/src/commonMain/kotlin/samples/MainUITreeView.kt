package samples

import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.UIText
import com.soywiz.korge.ui.UITreeViewList
import com.soywiz.korge.ui.UITreeViewNode
import com.soywiz.korge.ui.tooltip
import com.soywiz.korge.ui.uiTooltipContainer
import com.soywiz.korge.ui.uiTreeView
import com.soywiz.korge.view.Container

class MainUITreeView : Scene() {
    override suspend fun Container.sceneMain() {
        uiTooltipContainer { tooltips ->
            uiTreeView(
                UITreeViewList(listOf(
                    UITreeViewNode("hello"),
                    UITreeViewNode(
                        "world",
                        UITreeViewNode("test"),
                        UITreeViewNode(
                            "demo",
                            UITreeViewNode("demo"),
                            UITreeViewNode("demo"),
                            UITreeViewNode(
                                "demo",
                                UITreeViewNode("demo")
                            ),
                        ),
                    ),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                    UITreeViewNode("hello"),
                ), height = 16.0, genView = {
                    UIText("$it").tooltip(tooltips, "Tooltip for $it")
                })
            )
        }
    }
}
