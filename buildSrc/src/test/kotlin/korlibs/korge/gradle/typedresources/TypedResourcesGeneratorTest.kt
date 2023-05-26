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
        //println(generated)
        assertEquals(
"""
import korlibs.image.atlas.Atlas
import korlibs.io.file.VfsFile
import korlibs.io.file.std.resourcesVfs
import korlibs.image.atlas.readAtlas
import korlibs.audio.sound.readSound
import korlibs.image.format.readBitmap

// AUTO-GENERATED FILE! DO NOT MODIFY!

@Retention(AnnotationRetention.BINARY) annotation class ResourceVfsPath(val path: String)
inline class TypedVfsFile(val __file: VfsFile)
inline class TypedVfsFileBitmap(val __file: VfsFile) { suspend fun read(): korlibs.image.bitmap.Bitmap = this.__file.readBitmap() }
inline class TypedVfsFileSound(val __file: VfsFile) { suspend fun read(): korlibs.audio.sound.Sound = this.__file.readSound() }
interface TypedAtlas<T>

object KR : __KR.KR

object __KR {
    
    interface KR {
        val __file get() = resourcesVfs[""]
        @ResourceVfsPath("gfx") val `gfx` get() = __KR.KRGfx
        @ResourceVfsPath("hello.png") val `hello` get() = TypedVfsFileBitmap(resourcesVfs["hello.png"])
        @ResourceVfsPath("other") val `other` get() = __KR.KROther
        @ResourceVfsPath("sfx") val `sfx` get() = __KR.KRSfx
    }
    
    object KRGfx {
        val __file get() = resourcesVfs["gfx"]
        @ResourceVfsPath("gfx/demo.atlas.json") val `demo` get() = AtlasGfxDemoAtlas.TypedAtlas(resourcesVfs["gfx/demo.atlas.json"])
    }
    
    object KROther {
        val __file get() = resourcesVfs["other"]
        @ResourceVfsPath("other/file.raw") val `file` get() = TypedVfsFile(resourcesVfs["other/file.raw"])
    }
    
    object KRSfx {
        val __file get() = resourcesVfs["sfx"]
        @ResourceVfsPath("sfx/sound.mp3") val `sound` get() = TypedVfsFileSound(resourcesVfs["sfx/sound.mp3"])
    }
}

inline class AtlasGfxDemoAtlas(val __atlas: korlibs.image.atlas.Atlas) {
    inline class TypedAtlas(val __file: VfsFile) { suspend fun read(): AtlasGfxDemoAtlas = AtlasGfxDemoAtlas(this.__file.readAtlas()) }
    @ResourceVfsPath("gfx/demo.atlas/hello.png") val `hello` get() = __atlas["hello.png"]
    @ResourceVfsPath("gfx/demo.atlas/world.png") val `world` get() = __atlas["world.png"]
}
""".trimIndent().trim(),
            generated.trim().replace("\t", "    ")
        )
    }
}
