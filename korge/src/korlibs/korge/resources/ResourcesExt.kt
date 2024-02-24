package korlibs.korge.resources

import korlibs.audio.format.*
import korlibs.audio.sound.*
import korlibs.image.bitmap.*
import korlibs.image.font.readBitmapFont
import korlibs.image.font.readFont
import korlibs.image.font.readTtfFont
import korlibs.image.format.readBitmapSlice
import korlibs.inject.injector
import korlibs.io.resources.ResourceCache
import korlibs.io.resources.ResourcePath
import korlibs.io.resources.Resources
import korlibs.io.resources.resource

suspend fun resources(): Resources = injector().get()

fun resourceBitmap(@ResourcePath path: String, mipmaps: Boolean = true, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readBitmapSlice().also { it.bmp.mipmaps(enable = mipmaps) } }
fun resourceFont(@ResourcePath path: String, mipmaps: Boolean = true, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readFont(mipmaps = mipmaps) }
fun resourceBitmapFont(@ResourcePath path: String, mipmaps: Boolean = true, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readBitmapFont(mipmaps = mipmaps) }
fun resourceTtfFont(@ResourcePath path: String, preload: Boolean = false, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readTtfFont() }
fun resourceSound(@ResourcePath path: String, props: AudioDecodingProps = AudioDecodingProps.DEFAULT, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readSound(props) }
fun resourceMusic(@ResourcePath path: String, props: AudioDecodingProps = AudioDecodingProps.DEFAULT, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readMusic(props) }
