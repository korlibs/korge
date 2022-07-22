package samples.fleks.assets

import com.soywiz.klock.Stopwatch
import com.soywiz.korim.atlas.MutableAtlasUnit
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.resourcesVfs

class Assets {

    private val atlas = MutableAtlasUnit(512, 2048, border = 1)
    lateinit var images: Map<String, ImageDataContainer>

    fun getImage(name: String, slice: String = "") : ImageData {
        return if (images[name] != null) {
            if (slice.isEmpty()) {
                images[name]!!.default
            } else {
                if (images[name]!![slice] != null) {
                    images[name]!![slice]!!
                } else {
                    throw RuntimeException("Slice '$slice' of image '$name' not found in asset images!")
                }
            }
        } else {
            throw RuntimeException("Image '$name' not found in asset images!")
        }
    }

    suspend fun load(config: Config) {
        val sw = Stopwatch().start()
        println("start resources loading...")
        images = config.images.associate { it.first to resourcesVfs[it.second].readImageDataContainer(ASE.toProps(), atlas = atlas) }
        println("loaded resources in ${sw.elapsed}")
    }

    /**
     * Data class which contains the config for loading assets.
     */
    data class Config(
        var reloading: Boolean = false,
        val images: List<Pair<String, String>> = listOf()
    )
}
