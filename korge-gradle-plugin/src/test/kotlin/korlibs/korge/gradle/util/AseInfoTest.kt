package korlibs.korge.gradle.util

import korlibs.korge.gradle.typedresources.getResourceBytes
import org.junit.Assert
import org.junit.Test

class AseInfoTest {
    @Test
    fun test() {
        val info = ASEInfo.Companion.getAseInfo(getResourceBytes("sprites.ase"))
        Assert.assertEquals(0, info.slices.size)
        Assert.assertEquals(listOf("TestNum", "FireTrail", "FireTrail2"), info.tags.map { it.tagName })
    }
}
