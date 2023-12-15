package korlibs.korge.gradle.typedresources

import korlibs.korge.gradle.util.*
import org.junit.Assert
import org.junit.Test

class AseSpriteTest {
    @Test
    fun test() {
        val info = ASEInfo.Companion.getAseInfo(this::class.java.getResource("/sprites.ase")!!.readBytes())
        Assert.assertEquals(0, info.slices.size)
        Assert.assertEquals(listOf("TestNum", "FireTrail", "FireTrail2"), info.tags.map { it.tagName })
    }
}
