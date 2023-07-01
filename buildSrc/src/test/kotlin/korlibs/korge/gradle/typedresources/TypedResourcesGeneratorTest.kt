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
            )
        )
        val generatedNormalized = generated.trim().replace("\t", "    ")

        val expectedNormalized = """
import korlibs.audio.sound.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.image.bitmap.*
import korlibs.image.atlas.*
import korlibs.image.font.*
import korlibs.image.format.*

// AUTO-GENERATED FILE! DO NOT MODIFY!

@Retention(AnnotationRetention.BINARY) annotation class ResourceVfsPath(val path: String)
inline class TypedVfsFile(val __file: VfsFile)
inline class TypedVfsFileTTF(val __file: VfsFile) {
  suspend fun read(): korlibs.image.font.TtfFont = this.__file.readTtfFont()
}
inline class TypedVfsFileBitmap(val __file: VfsFile) {
  suspend fun read(): korlibs.image.bitmap.Bitmap = this.__file.readBitmap()
  suspend fun readSlice(atlas: MutableAtlasUnit? = null, name: String? = null): BmpSlice = this.__file.readBitmapSlice(name, atlas)
}
inline class TypedVfsFileSound(val __file: VfsFile) {
  suspend fun read(): korlibs.audio.sound.Sound = this.__file.readSound()
}
interface TypedAtlas<T>

object KR : __KR.KR

object __KR {
    
    interface KR {
        val __file get() = resourcesVfs[""]
        @ResourceVfsPath("0000") val `n0000` get() = __KR.KR0000
        @ResourceVfsPath("fonts") val `fonts` get() = __KR.KRFonts
        @ResourceVfsPath("gfx") val `gfx` get() = __KR.KRGfx
        @ResourceVfsPath("hello.png") val `hello` get() = TypedVfsFileBitmap(resourcesVfs["hello.png"])
        @ResourceVfsPath("images") val `images` get() = __KR.KRImages
        @ResourceVfsPath("other") val `other` get() = __KR.KROther
        @ResourceVfsPath("sfx") val `sfx` get() = __KR.KRSfx
    }
    
    object KR0000 {
        val __file get() = resourcesVfs["0000"]
        @ResourceVfsPath("0000/1111") val `n1111` get() = __KR.KR00001111
    }
    
    object KRFonts {
        val __file get() = resourcesVfs["fonts"]
        @ResourceVfsPath("fonts/hello.ttf") val `hello` get() = TypedVfsFileTTF(resourcesVfs["fonts/hello.ttf"])
    }
    
    object KRGfx {
        val __file get() = resourcesVfs["gfx"]
        @ResourceVfsPath("gfx/demo.atlas.json") val `demo` get() = AtlasGfxDemoAtlas.TypedAtlas(resourcesVfs["gfx/demo.atlas.json"])
    }
    
    object KRImages {
        val __file get() = resourcesVfs["images"]
        @ResourceVfsPath("images/image.ase") val `image` get() = AseImagesImageAse.TypedAse(resourcesVfs["images/image.ase"])
    }
    
    object KROther {
        val __file get() = resourcesVfs["other"]
        @ResourceVfsPath("other/file.raw") val `file` get() = TypedVfsFile(resourcesVfs["other/file.raw"])
    }
    
    object KRSfx {
        val __file get() = resourcesVfs["sfx"]
        @ResourceVfsPath("sfx/sound.mp3") val `sound` get() = TypedVfsFileSound(resourcesVfs["sfx/sound.mp3"])
    }
    
    object KR00001111 {
        val __file get() = resourcesVfs["0000/1111"]
        @ResourceVfsPath("0000/1111/222a.png") val `n222a` get() = TypedVfsFileBitmap(resourcesVfs["0000/1111/222a.png"])
    }
}

inline class AtlasGfxDemoAtlas(val __atlas: korlibs.image.atlas.Atlas) {
    inline class TypedAtlas(val __file: VfsFile) { suspend fun read(): AtlasGfxDemoAtlas = AtlasGfxDemoAtlas(this.__file.readAtlas()) }
    @ResourceVfsPath("gfx/demo.atlas/hello.png") val `hello` get() = __atlas["hello.png"]
    @ResourceVfsPath("gfx/demo.atlas/world.png") val `world` get() = __atlas["world.png"]
}

inline class AseImagesImageAse(val data: korlibs.image.format.ImageDataContainer) {
    inline class TypedAse(val __file: VfsFile) { suspend fun read(atlas: korlibs.image.atlas.MutableAtlasUnit? = null): AseImagesImageAse = AseImagesImageAse(this.__file.readImageDataContainer(korlibs.image.format.ASE.toProps(), atlas)) }
    enum class TypedAnimation(val animationName: String) {
        ;
        companion object {
            val list: List<TypedAnimation> = values().toList()
        }
    }
    inline class TypedImageData(val data: ImageData) {
        val animations: TypedAnimation.Companion get() = TypedAnimation
    }
    val animations: TypedAnimation.Companion get() = TypedAnimation
    val default: TypedImageData get() = TypedImageData(data.default)
}
""".trimIndent().trim()

        if (expectedNormalized != generatedNormalized) {
            println(generatedNormalized)
        }

        assertEquals(expectedNormalized, generatedNormalized)
    }
}
