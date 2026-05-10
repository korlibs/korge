package korlibs.korge.ui

import korlibs.korge.annotations.*
import korlibs.korge.tests.*
import kotlin.test.*

@OptIn(KorgeExperimental::class)
class UITreeViewTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        uiTooltipContainer { tooltips ->
            uiTreeView(
                UITreeViewList(
                    listOf(
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
