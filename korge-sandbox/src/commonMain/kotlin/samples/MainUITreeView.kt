package samples

import korlibs.korge.scene.Scene
import korlibs.korge.ui.UIText
import korlibs.korge.ui.UITreeViewList
import korlibs.korge.ui.UITreeViewNode
import korlibs.korge.ui.tooltip
import korlibs.korge.ui.uiTooltipContainer
import korlibs.korge.ui.uiTreeView
import korlibs.korge.view.SContainer

class MainUITreeView : Scene() {
    override suspend fun SContainer.sceneMain() {
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
                ), height = 16f, genView = {
                    UIText("$it").tooltip(tooltips, "Tooltip for $it")
                })
            )
        }
    }
}
