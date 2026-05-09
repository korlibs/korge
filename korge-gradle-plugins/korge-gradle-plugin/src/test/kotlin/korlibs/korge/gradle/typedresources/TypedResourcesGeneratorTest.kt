package korlibs.korge.gradle.typedresources

import korlibs.korge.gradle.util.*
import org.junit.*
import org.junit.Assert.assertEquals

class TypedResourcesGeneratorTest {
    @Test
    fun test() {
        val generated = TypedResourcesGenerator().generateForFolders(
            MemorySFile(
                "hello.png" to "",
                "sfx/sound.mp3" to "",
                "gfx/demo.atlas/hello.png" to "",
                "gfx/demo.atlas/world.png" to "",
                "0000/1111/222a.png" to "",
                "other/file.raw" to "",
                "fonts/hello.ttf" to "",
                "images/image.ase" to "",
                "images/image2.ase" to "INVALID213123123621639172639127637216",
            )
        ) { e, message ->

        }

        fun String.normalize(): String {
            return this.trimIndent().replace("\t", "    ").trim().lines().map { it.trimEnd() }.joinToString("\n")
        }

        val generatedNormalized = generated.trim().normalize()
        val expectedNormalized = getResourceText("expected.KR.generated.txt").normalize()

        if (expectedNormalized != generatedNormalized) {
            println(generatedNormalized)
        }

        assertEquals(expectedNormalized, generatedNormalized)
    }
}
