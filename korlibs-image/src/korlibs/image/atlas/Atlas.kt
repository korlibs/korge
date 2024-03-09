package korlibs.image.atlas

import korlibs.image.bitmap.*
import korlibs.image.format.ImageDecodingProps
import korlibs.image.format.readBitmapSlice
import korlibs.io.file.VfsFile

class Atlas(val textures: Map<String, BmpSlice>, val info: AtlasInfo = AtlasInfo()) : AtlasLookup {
    constructor(texture: BmpSlice, info: AtlasInfo = AtlasInfo()) : this(mapOf(info.pages.first().fileName to texture), info)
    constructor(slices: List<BmpSlice>) : this(slices.mapIndexed { index, bmp -> (bmp.name.takeIf { it != "unknown" } ?: "$index") to bmp }.toMap())

    val texture get() = textures.values.first()

    inner class Entry(val info: AtlasInfo.Region, val page: AtlasInfo.Page) {
        val texture = textures[page.fileName]
            ?: error("Can't find '${page.fileName}' in ${textures.keys}")
        val slice = texture.slice(info.frame, info.name, orientation = info.imageOrientation).virtFrame(info.virtFrame)
        val name get() = info.name
        // @TODO: Use name instead
        val filename get() = info.name
    }

	val entries = info.pages.flatMap { page ->
        page.regions.map { frame ->
            Entry(frame, page)
        }
    }
    val entriesMap = entries.associateBy { it.filename }

    override fun tryGetEntryByName(name: String): Entry? = entriesMap[name]
}

interface AtlasLookup {
    fun tryGetEntryByName(name: String): Atlas.Entry?
    fun tryGet(name: String): BmpSlice? = tryGetEntryByName(name)?.slice
    operator fun get(name: String): BmpSlice = tryGet(name)
        ?: error("Can't find '$name' it atlas")
}

suspend fun VfsFile.readAtlas(
    props: ImageDecodingProps = ImageDecodingProps.DEFAULT
): Atlas {
    val content = this.readString()
    val info = when {
        content.startsWith("{") -> AtlasInfo.loadJsonSpriter(content)
        content.startsWith("<") -> AtlasInfo.loadXml(content)
        content.startsWith('\n') -> AtlasInfo.loadText(content)
        content.startsWith("\r\n") -> AtlasInfo.loadText(content)
        else -> error("Unexpected atlas starting with '${content.firstOrNull()}'")
    }
    val folder = this.parent
    val textures = info.pages.associate {
        it.fileName to folder[it.fileName].readBitmapSlice(props = props)
    }
    return Atlas(textures, info)
}
