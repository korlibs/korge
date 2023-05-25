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
                "other/file.raw" to "",
            )
        )
        println(generated)
        assertEquals(
"""
import korlibs.audio.sound.Sound
import korlibs.audio.sound.readSound
import korlibs.image.atlas.Atlas
import korlibs.image.atlas.readAtlas
import korlibs.io.file.VfsFile
import korlibs.io.file.std.resourcesVfs
import korlibs.image.format.readBitmap

// AUTO-GENERATED FILE! DO NOT MODIFY!

inline class TypedVfsFile<T>(val file: VfsFile)
interface TypedAtlas<T>

object KR {
    val __file get() = resourcesVfs[""]
    val `gfx` get() = KRGfx
    val `hello` get() = TypedVfsFile<korlibs.image.bitmap.Bitmap>(resourcesVfs["hello.png"])
    val `other` get() = KROther
    val `sfx` get() = KRSfx
}

object KRGfx {
    val __file get() = resourcesVfs["gfx"]
    val `demo` get() = TypedVfsFile<TypedAtlas<AtlasGfxDemoAtlas>>(resourcesVfs["gfx/demo.atlas.json"])
}

object KROther {
    val __file get() = resourcesVfs["other"]
    val `file` get() = TypedVfsFile<korlibs.io.file.VfsFile>(resourcesVfs["other/file.raw"])
}

object KRSfx {
    val __file get() = resourcesVfs["sfx"]
    val `sound` get() = TypedVfsFile<korlibs.audio.sound.Sound>(resourcesVfs["sfx/sound.mp3"])
}

@kotlin.jvm.JvmName("read_TypedVfsFile_TypedAtlas_AtlasGfxDemoAtlas")
suspend fun TypedVfsFile<TypedAtlas<AtlasGfxDemoAtlas>>.read() = AtlasGfxDemoAtlas(this.file.readAtlas())
inline class AtlasGfxDemoAtlas(val __atlas: korlibs.image.atlas.Atlas) {
    val `hello` get() = __atlas["hello.png"]
    val `world` get() = __atlas["world.png"]
}

@kotlin.jvm.JvmName("read_TypedVfsFile_Bitmap")
suspend fun TypedVfsFile<korlibs.image.bitmap.Bitmap>.read() = this.file.readBitmap()
@kotlin.jvm.JvmName("read_TypedVfsFile_Sound")
suspend fun TypedVfsFile<korlibs.audio.sound.Sound>.read() = this.file.readSound()
""".trimIndent().trim(),
            generated.trim().replace("\t", "    ")
        )
    }
}
