package samples

import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*

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
                ), height = 16.0, genView = {
                    UIText("$it").tooltip(tooltips, "Tooltip for $it")
                })
            )
        }
    }
}
