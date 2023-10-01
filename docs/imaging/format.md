---
permalink: /imaging/format/
group: imaging
layout: default
title: "Image Formats"
title_short: Image Formats
description: KorIM supports creating, loading and saving different image formats.
fa-icon: fa-file-image
priority: 40
---


## ImageFormat, ImageFormats

ImageFormat allows to read/decode and write/encode images in a specific format. This class supports decoding static images, animations, and layered animated formats like `ASE` supporting most of its features.

```kotlin
abstract class ImageFormat(vararg exts: String) {
	val extensions: List<String>

    // Basic methods to implement
	open fun readImage(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): ImageData = TODO()

    open fun readImageContainer(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): ImageDataContainer

	open fun writeImage(
		image: ImageData,
		s: SyncStream,
		props: ImageEncodingProps = ImageEncodingProps("unknown")
	): Unit = throw UnsupportedOperationException()

    // Extended useful methods

	open fun decodeHeader(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): ImageInfo?

	fun read(s: SyncStream, filename: String = "unknown"): Bitmap

	suspend fun read(file: VfsFile): Bitmap
	fun read(s: ByteArray, filename: String = "unknown"): Bitmap

	fun read(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): Bitmap

	fun read(s: ByteArray, props: ImageDecodingProps = ImageDecodingProps()): Bitmap

	fun check(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): Boolean

	fun decode(s: SyncStream, props: ImageDecodingProps = ImageDecodingProps()): Bitmap
	fun decode(s: ByteArray, props: ImageDecodingProps = ImageDecodingProps()): Bitmap

	fun encode(frames: List<ImageFrame>, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray

	fun encode(image: ImageData, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray
	fun encode(bitmap: Bitmap, props: ImageEncodingProps = ImageEncodingProps("unknown")): ByteArray

	suspend fun read(file: VfsFile, props: ImageDecodingProps = ImageDecodingProps()): Bitmap
}

data class ImageDecodingProps(
    val filename: String = "unknown",
    val width: Int? = null,
    val height: Int? = null,
    override var extra: ExtraType = null
) : Extra

data class ImageEncodingProps(
    val filename: String = "",
    val quality: Double = 0.81,
    override var extra: ExtraType = null
) : Extra
```

## NativeImageFormatProvider

All the targets support at least native `PNG` and `JPEG` decoding:

```kotlin
expect val nativeImageFormatProvider: NativeImageFormatProvider

// This will try to use the native image format provider
suspend fun VfsFile.readBitmapOptimized()
```

### Show images

```kotlin
suspend fun Bitmap.showImageAndWait(kind: Int = 0)
suspend fun ImageData.showImagesAndWait(kind: Int = 0) suspend fun List<Bitmap>.showImagesAndWait(kind: Int = 0)
suspend fun SizedDrawable.showImageAndWait(kind: Int = 0)
```

## RegisteredImageFormats

```kotlin
object RegisteredImageFormats : ImageFormat() {
    var formats: ImageFormats
    fun register(vararg formats: ImageFormat)
    fun unregister(vararg formats: ImageFormat)
    inline fun <T> temporalRegister(vararg formats: ImageFormat, callback: () -> T): T
}
```

## ImageInfo

```kotlin
open class ImageInfo : Extra by Extra.Mixin() {
	var width: Int = 0
	var height: Int = 0
	var bitsPerPixel: Int = 8

	val size: Size get() = Size(width, height)

	override fun toString(): String = "ImageInfo(width=$width, height=$height, bpp=$bitsPerPixel, extra=$extra)"
}
```

## ImageDataContainer, ImageData, ImageAnimation, ImageFrame, ImageFrameLayer, ImageLayer

```kotlin
open class ImageDataContainer(
    val imageDatas: List<ImageData>
) {
    val imageDatasByName = imageDatas.associateBy { it.name }
    val default = imageDatasByName[null] ?: imageDatas.first()

    operator fun get(name: String?): ImageData? = imageDatasByName[name]
}

open class ImageLayer constructor(
    var index: Int,
    val name: String?
)

open class ImageData(
    val frames: List<ImageFrame>,
    val loopCount: Int = 0,
    val width: Int,
    val height: Int,
    val layers: List<ImageLayer> = fastArrayListOf(),
    val animations: List<ImageAnimation> = fastArrayListOf(),
    val name: String? = null,
) : Extra by Extra.Mixin() {
    val defaultAnimation: ImageAnimation
    val animationsByName: Map<String, ImageAnimation>
    val area: Int
    val framesByName: Map<String, ImageName>
    val framesSortedByProority: List<ImageFrame>
    val mainBitmap: Bitmap
}

data class ImageDataWithAtlas(val image: ImageData, val atlas: AtlasPacker.Result<ImageFrameLayer>)

open class ImageFrame(
    val index: Int,
    val time: TimeSpan = 0.seconds,
    val layerData: List<ImageFrameLayer> = emptyList(),
) : Extra by Extra.Mixin() {
    val first: ImageFrameLayer?
    val slice: BmpSlice
    val targetX: Int
    val targetY: Int
    val main: Boolean
    val includeInAtlas: Boolean

    val duration: TimeSpan
    val width: Int
    val height: Int
	val area: Int
    val bitmap: Bitmap
    val name: String?
}

open class ImageFrameLayer constructor(
    val layer: ImageLayer,
    slice: BmpSlice,
    val targetX: Int = 0,
    val targetY: Int = 0,
    val main: Boolean = true,
    val includeInAtlas: Boolean = true,
    val linkedFrameLayer: ImageFrameLayer? = null,
) {
    var slice: BmpSlice
    val width: Int
    val height: Int
    val area: Int
    val bitmap: Bitmap
    val bitmap32: Bitmap32
}

open class ImageAnimation(
    val frames: List<ImageFrame>,
    val direction: Direction,
    val name: String,
    val layers: List<ImageLayer>
) {
    enum class Direction { FORWARD, REVERSE, PING_PONG }
}
```

### Packing ImageDataContainer and ImageData into an atlas in runtime

```kotlin
fun ImageData.packInMutableAtlas(mutableAtlas: MutableAtlas<Unit>): ImageData
fun ImageDataContainer.packInMutableAtlas(mutableAtlas: MutableAtlas<Unit>): ImageDataContainer 
```

## Formats

KorIM supports lots of formats out of the fox

### ASE

ASEPrite image format. Supports reading.

### BMP

Bitmap uncompressed format. Supports reading and writing.

### DDS, DXT, DXt1, DXT3, DXT4, DXT5

Supports reading DDS files, and DXT encoded images.

### GIF

Supports decoding static and animated GIF files.

### ICO

Supports reading windows icon files.

### KRA

Supports reading krita image files.

### PNG

Supports reading and writing PNG (Portable Network Graphics) files.

### PSD

Supports reading PSD files (only the flattened global layer for now)

### SVG

Supports reading SVG and rasterizing it to the default size. (For SVG vector reading check the vector graphics section)

### TGA

Supports reading and writing Targa uncompressed image files.

### WEBP

Supports reading and writing WEBP image files via WebAssembly.

## Reading images

You can read images from files in any location with Vfsfile.

You can use the `format=` argument in the reading functions to specific a specific format or a `ImageFormats` group, or register the formats you are going to use into the `RegisteredImageFormats`;

```kotlin
suspend fun VfsFile.readBitmap()
suspend fun VfsFile.readBitmapOptimized()
suspend fun VfsFile.readImageDataContainer()
```
