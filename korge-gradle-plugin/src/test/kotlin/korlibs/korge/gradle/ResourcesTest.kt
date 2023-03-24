package korlibs.korge.gradle

import korlibs.korge.gradle.targets.KorgeIconProvider
import kotlin.test.Test

class ResourcesTest {
    @Test
    fun test() {
        KorgeIconProvider().getIconBytes()
    }
}