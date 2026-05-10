package korlibs.korge.tests

import korlibs.kgl.*

actual fun enrichTestGameWindow(window: ViewsForTesting.TestGameWindow) {
    runCatching { window.androidContextAny = getAndroidTestContext() }
}
