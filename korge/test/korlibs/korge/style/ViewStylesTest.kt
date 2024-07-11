package korlibs.korge.style

import korlibs.image.color.*
import korlibs.korge.view.*
import kotlin.test.*

internal class ViewStylesTest {
    @Test
    fun viewStylesReturnsDefaultStyleIfNotDefined() {
        val container = Container()

        assertEquals(container.styles.textColor, Colors.WHITE)
    }

    @Test
    fun viewStylesReturnsLocalStyleIfDefined() {
        val container = Container()
        container.styles {
            textColor = Colors.RED
        }

        assertEquals(container.styles.textColor, Colors.RED)
    }

    @Test
    fun viewStylesPassesDownStyleToChildren() {
        val container = Container()
        container.styles {
            textColor = Colors.RED
        }

        container.container {
            assertEquals(styles.textColor, container.styles.textColor)
        }
    }

    @Test
    fun viewStylesOverridesPassedDownStyleOfParentWithLocalStyle() {
        val container = Container()
        container.styles {
            textColor = Colors.RED
        }

        container.container {
            styles {
                textColor = Colors.BLUE
            }

            assertNotEquals(styles.textColor, container.styles.textColor)
            assertEquals(styles.textColor, Colors.BLUE)
        }
    }

    @Test
    fun viewStylesKeepsPassedDownStyleOfParentWhenNotInLocalStyle() {
        val container = Container()
        container.styles {
            textColor = Colors.RED
            buttonBackColor = Colors.YELLOW
        }

        container.container {
            styles {
                textColor = Colors.BLUE
            }

            assertNotEquals(styles.textColor, container.styles.textColor)
            assertEquals(styles.textColor, Colors.BLUE)
            assertEquals(styles.buttonBackColor, container.styles.buttonBackColor)
        }
    }

    @Test
    fun viewStylesPassesDownOnNonUIViewParent() {
        val container = SContainer()
        container.styles {
            textColor = Colors.RED
        }

        container.container {
            assertEquals(styles.textColor, container.styles.textColor)
        }
    }
}
