---
permalink: /imaging/bitmap/
group: imaging
layout: default
title: "Imaging Bitmaps"
title_short: "Bitmaps"
description: KorIM support several Bitmap formats and operations.
fa-icon: fa-image
priority: 10
---

## Bitmap

`Bitmap` is an abstract class used to represent images, as a bidimensional matrix with a set of RGBA pixels.

`BmpSlice` represents a region inside a `Bitmap`.

All Bitmaps have the following immutable properties:

* `width: Int` the width of the image in pixels
* `height: Int` the height of the image in pixels
* `bpp: Int` bits per pixel for this image. For example for Bitmap32 it would be 32
* `premultiplied: Boolean` specifies if the color pixels in this image are premultiplied or not
* `backingArray: Any?` an optional array reference containing the raw pixels in the internal format of the Bitmap

You can get the area in pixels of the bitmap with `bitmap.area` (`width * height`).

You can determine if a position is inside the bitmap with `bitmap.inside(x, y)`, there is an alias called `bitmap.inBounds(x, y)`.

You can clamp your coordinates by using `bitmap.clampX(x)` and `bitmap.clampY(x)`.

You can get the linear index of a position with `bitmap.index(x, y)`.

When using Bitmaps with KorGE, you can set the mutable property `mipmaps` to instruct the engine to generate mipmaps when internally converting into a texture and uploaded to the GPU.

### Getting and setting pixels

```kotlin
// Setting a color
bitmap.setRgba(x, y, Colors.RED)
bitmap.setInt(x, y, 17) // This depends on the kind of bitmap (indexed or rgba)

// Getting a color
val color: RGBA = bitmap.getRgba(x, y)
val colorValue: Int = bitmap.getInt(x, y)
val sampledColor: RGBA = bitmap.getRgbaSampled(1.5, 1.5) // Performs linear interpolation and samples neighborhood pixels to compute the color
```

 >  Note that depending on the Bitmap implementation, reading individual pixels might be costly. For example for `Bitmap32` it is pretty fast, but for a `HtmlNativeImage` this will be slow. It is recommended to use `readPixels` and `writePixels` instead or to convert the bitmap to `Bitmap32` with `bitmap.toBMP32()`

### Reading, writing and copying blocks of pixels

To read and write with the best performance a region in the Bitmap, you can use readPixelUnsafe and writePixelUnsafe:

```kotlin
val pixels = RgbaArray(width * height)
readPixelsUnsafe(x, y, width, height, pixels, offset = 0)
writePixelsUnsafe(x, y, width, height, pixels, offset = 0)
```

If you want to copy pixels from one image to another:

```kotlin
var dst: Bitmap
bitmap.copy(srcX, srcY, dst, dstX, dstY, width, height)
```

### Locking / updating the texture in KorGE

In KorGE, the bitmap is converted into a texture, and sometimes you will want to update that Bitmap and reupload the texture. To do so, you have to lock and unlock the bitmap.

```kotlin
bitmap.lock {
    // change bitmap pixels here
}
// starting here, the texture will be reuploaded
```

### Flipping the Bitmap

You can flip your image with `bitmap.flipX()` and `bitmap.flipY()`.

### Swapping rows and columns

You can swap two rows or two columns together with:

```kotlin
bitmap.swapRows(y0, y0)
bitmap.swapColumns(x0, x0)
```

### Converting into Bitmap32

You can call the `bitmap.toBMP32()` method to convert any `Bitmap` into a `Bitmap32`, if you prefer to not create a new instance if it is already a Bitmap32, you can call `bitmap.toBMP32IfRequired()`

### Cloning, extracting and creating bitmaps with the same format

To create a bitmap with the same content you can use:

`bitmap.clone()`

To create a new empty bitmap of the same type, but with different dimensions, you can use:

```kotlin
bitmap.createWithThisFormat(newWidth, newHeight)
```

To create a new bitmap with part of the contents of the original image:

```kotlin
val newBitmap = bitmap.extract(x, y, width, height)
```

### Comparing contents

You can check if two bitmaps have exactly the same pixels by calling `bitmap.contentEquals(otherBitmap)`.

### Context2d

You can create a context2d for drawing vectors, and stuff with a HTML-like API, with:

```kotlin
bitmap.context2d { context2d ->
    // ...
}
```

### Iterating over all the positions

```kotlin
bitmap.forEach { n, x, y ->
    val pixel: RGBA = bitmap.getRgba(x, y)
    val pixel: RGBA = bitmap32.data[n]
}
```

### Scaling a Bitmap

You can create a new image scaling another image, to a new size, and using a `ScaleMode` and an `Anchor` to do so.

```kotlin
val resized = bitmap.resized(width, height, ScaleMode.COVER, Anchor.CENTER, native = true)
```

## ScaleMode

For resizing we have different ScaleMode strategies

* `ScaleMode.COVER` - Keeps the aspect ratio, covers all the space cutting some parts of the image if the aspect ratio doesn't fit
* `ScaleMode.SHOW_ALL` / `ScaleMode.FIT` - Shows the whole image without losing the aspect ratio, some parts of the destination won't have pixels
* `ScaleMode.EXACT` - This will distort the image, and will fill all the pixels
* `ScaleMode.NO_SCALE` - Doesn't scale the image at all

## BitmapIndexed, Bitmap1, Bitmap2, Bitmap4, Bitmap8

KorIM also supports indexed bitmaps, that are bitmaps whose pixels are determined by an integer of an specific amount of bits.

```kotlin
// Getting a pixel value
val pixel: Int = bitmap[x, y] // equivalent to bitmap.getInt(x, y)
// Setting a pixel value // equivalent to bitmap.setInt(x, y, pixel)
bitmap[x, y] = pixel
```

### Constructing a new BitmapIndexed

```kotlin
val bitmap1 = Bitmap1(width, height)
val bitmap2 = Bitmap2(width, height)
val bitmap4 = Bitmap4(width, height)
val bitmap8 = Bitmap8(width, height)

// You can specify a palette with:
val bitmap = Bitmap4(width, height, palette = RgbaArray(16))

// For Bitmap8 you can use a value provider when constructing
val bitmap = Bitmap8(width, height) { x, y ->
    (x + y).toByte()
}
```

### Setting a grayscale palette:

```kotlin
// This will update the palette with a gradient from pitch black, to clear white
bitmap.setWhitescalePalette()
```

### Convert to String

By specifying a character for each possible color, you can convert a BitmapIndexed into a String like this:

```kotlin
val paletteString = ".*" // 0=. 1=*
println(bitmap1.toLines(paletteString).joinToString("\n")()
```

### Bitmap32 to Bitmap1

You can construct a Bitmap1 by using a Bitmap32 as reference, and providing a function determining if each pixel is going to be 0 (false) or 1 (true).

```kotlin
bitmap32.toBitmap1 { color: RGBA -> color.a >= 0x3F }
```

## NativeImage

Native image is a special type of `Bitmap` that usually represents a Bitmap in a native platform. For example in JS, it would be represented as a `<canvas>` or `<img>`, and in the JVM it would be a `BufferedImage`. Some implementations require for setting and getting the color bits to copy memory from the GPU, and that might be slow to perform pixel by pixel.

This bitmap however, when using the `Context2D`, it uses native operations for vector rendering, which is usually faster.

You can construct an empty NativeImage with:
`NativeImage(width, height)`.

## Bitmap32

The Bitmap32 class is a Bitmap ideal for manipulating the image pixel by pixel. It has 8 bits for red, green, blue and alpha channels.

* The Bitmap32 class has a `data: RgbaArray`, `dataPremult: RgbaPremultipliedArray` and `intData: IntArray` representing the colors as linear array.

### Construct a Bitmap32

You can construct a Bitmap32 with:

```kotlin
val bitmap = Bitmap32(width, height)
val bitmap = Bitmap32(width, height, RgbaArray(width * height), premultiplied = false)
val bitmap = Bitmap32(width, height, Colors.RED)
val bitmap = Bitmap32(width, height) { x, y -> Colors.RED }
```

### Getting and setting Pixels

```kotlin
val color: RGBA = bitmap[x, y]
val color: RGBA = bitmap.getRgba(x, y)
val colorInt: Int = bitmap.getInt(x, y)

bitmap[x, y] = color
bitmap.setRgba(x, y, color)
bitmap.setInt(x, y, colorInt)
```

### Getting historiogram

```kotlin
val redHistoriogram = bitmap32.historiogram(BitmapChannel.RED)
```

### Filling the whole bitmap or a slice with a specific color

```kotlin
bitmap32.fill(Colors.BLACK_TRANSPARENT)
bitmap32.fill(Colors.BLACK_TRANSPARENT, x, y, width, height)
```

### Drawing or copying another Bitmap32

```kotlin
bitmap32.put(bmp32OrSlice, dx, dy) // Replace pixels
bitmap32.draw(bmp32OrSlice, dx, dy) // Blend pixels
```

### Transferring channel (RED, GREEN, BLUE or ALPHA) data individually

```kotlin
bitmap32.writeChannel(BitmapChannel.RED, sourceBitmap, source = BitmapChannel.ALPHA)
bitmap32.writeChannel(BitmapChannel.RED, sourceBitmap8)

val channelData: Bitmap8 = bitmap32.extractChannel(BitmapChannel.BLUE)

// Static variants
Bitmap32.copyChannel(src, srcChannel, dst, dstChannel)
Bitmap32.copyChannel(src, dst, channel)
```

### XOR and INVERT pixels

```kotlin
// Mutating variants
bitmap32.invert()
bitmap32.xor(RGBA(255, 255, 255, 0))

// Copy variants
val newBitmap = bitmap32.inverted()
val newBitmap = bitmap32.xored(RGBA(255, 255, 255, 0))
```

### Handling premultiplied

```kotlin
val newBitmap = bitmap.premultiplied()
val newOrSameBitmap = bitmap.premultipliedIfRequired()

val newBitmap = bitmap.depremultiplied()
val newOrSameBitmap = bitmap.depremultipliedIfRequired()

bitmap.premultiplyInplaceIfRequired()
bitmap.depremultiplyInplace()
```

### Aplying ColorTransform and ColorMatrix

```kotlin
val newBitmap = bmp32.withColorTransform(colorTransform)
val newBitmap = bmp32.withColorTransform(colorTransform, x, y, width, height)

bmp32.applyColorTransform(colorTransform, x, y, width, height)

bmp32.applyColorMatrix(Matrix3D())
```

### Generating a mipmap of the current bitmap

```kotlin
val halfSizeBitmap = bitmap.mipmap(1) // [width,height] / 2
val quarterSizeBitmap = bitmap.mipmap(2) // [width,height] / 4
```

### Generate a scaled image

```kotlin
val scaled = scaleNearest(0.5, 0.5) // half the size nearest neighborhood
val scaled = scaleLinear(0.5, 0.5) // half the size linear interpolation
val scaled = scaled(newWidth, newHeight, smooth = true)
```

### Compare bitmaps

```kotlin
Bitmap32.matches(bitmap1, bitmap2, threshold = 32)
val result: MatchResult = Bitmap32.matchesWithResult(bitmap1, bitmap2)
val diffBitmap32 = Bitmap32.diff(bitmap1, bitmap2)
```

### Generate a new bitmap with the edges expanded

When generating atlases it is useful to extrude the edges of slices so when sampled with OpenGL, there are no artifacts.

```kotlin
bitmap32.expandBorder(RectangleInt(2, 2, 50, 50), border = 2)
```

## BitmapChannel

This is an enum `BitmapChannel.RED`, `BitmapChannel.GREEN`, `BitmapChannel.BLUE`, `BitmapChannel.ALPHA`. It is used to manipulate specific channels usually in Bitmap32,

You can extract and insert each component in a RGBA color with:

```kotlin
val red: Int = BitmapChannel.RED.extract(color)
val newColor: RGBA = BitmapChannel.RED.insert(color, 0x7F)
```

## FloatBitmap32

FloatBitmap32 is like Bitmap32 but stores its components as floats in the range of [0f, 1f].

### Constructing a FloatBitmap32

```kotlin
// A New Bitmap

val floatBitmap32 = FloatBitmap32(width, height)
val floatBitmap32 = FloatBitmap32(width, height, FloatArray(width * height * 4), premultiplied = false)

// From other Bitmap
val floatBitmap32 = bitmap.toFloatBMP32()
```

### Setting / getting pixels

```kotlin
bmp.setRgba(x, y, rgbaColor)
bmp.setRgba(x, y, rf, gf, bf, af)
bmp.setRgbaf(x, y, rgbafColor)

val color: RGBA = bmp.getRgba(x, y)
val color: RGBAf = bmp.getRgbaf(x, y)
```

## DistanceBitmap

A DistanceBitmap is a Bitmap that measures distances to an specific pixel in each pixel. Used for example to compute borders, shadows etc.

### Creating a DistanceBitmap

```kotlin
val dist: DistanceBitmap = bitmap.distanceMap(thresold = 0.5) // alpha thresold
```

### Getting distances

You can get the distance for each position:

```kotlin
val dist: Float = getDist(x, y) // hypot
val x: Int = getPosX(x, y) // nearest absolute position (X)
val y: Int = getPosY(x, y) // nearest absolute position (Y)
val rx: Int = getRPosX(x, y) // nearest relative position (X)
val ry: Int = getRPosY(x, y) // nearest relative position (Y)
```

### Converting into Bitmap8

To visualize the distance map, you can generate a Bitmap8.

```kotlin
val pixels = distanceBitmap.toNormalizedDistanceBitmap8()
```
 
## PSNR

Utils to compute Peak Signal-to-Noise Ratio

```kotlin
// PSNR
val psnr: Double = PSNR(bitmap1, bitmap2)
val psnr: Double = PSNR(bitmap1, bitmap2, BitmapChannel.RED)
val psnr: Double = bitmap1.psnrDiffTo(bitmap2)
val psnr: Double = Bitmap32.computePsnr(bitmap1, bitmap2)
```

## Palette

Palette is mainly a wrapper of `RgbaArray` but supports some optional extra properties, like names for each color.

```kotlin
val palette = Palette(RgbaArray)(16)
val palette = Palette(colors = RgbaArray(16), names = Array(16) { "name$it" })
```

## Bitmap Tracing

You can get an approximate contourn of a Bitmap with the trace methods.

```kotlin
val path: VectorPath = bitmap.trace()
val path: VectorPath = bitmap.trace { rgba -> rgba.a >) 9x3F }
```

## Bitmap Effects
{:#bitmap_effects}

KorIM supports applying Bitmap effects.
It supports adding border to the pixels of images, it supports adding a gaussian blur effect to the bitmap, and support adding drop shadows to the bitmaps.

```kotlin
data class BitmapEffect(
    // Blur
    var blurRadius: Int = 0,
  
    // Drop Shadow
    var dropShadowX: Int = 0,
    var dropShadowY: Int = 0,
    var dropShadowRadius: Int = 0,
    var dropShadowColor: RGBA = Colors.BLACK,
  
    // Border
    var borderSize: Int = 0,
    var borderColor: RGBA = Colors.BLACK
) 
```

### Applying Bitmap Effects

```kotlin
bitmap.applyEffectInline(effect)
val newBitmap = bitmap.applyEffect(effect)
```


## BitmapSlice<T>, BmpSlice and BmpCoords

The `BmpSlice` class is used to declare a region inside a `Bitmap`. There is a typed subtype called `BitmapSlice<TBitmap>`, and there is an interface implementd by them called `BmpCoords` that is used maily for textures.

### BmpCoords

The interface of BmpCoords, defines the following properties:

* `tl_x: Float` and `tl_y: Float`, that specifies the top left coordinates
* `tr_x: Float` and `tr_y: Float`, that specifies the top right coordinates
* `br_x: Float` and `br_y: Float`, that specifies the bottom right coordinates
* `bl_x: Float` and `bl_y: Float`, that specifies the bottom left coordinates

### BmpSlice

* `bmpBase: Bitmap`
* `bounds: RectangleInt`
* `name: String? = null`
* `rotated: Boolean = false`
* `virtFrame: RectangleInt? = null`
* `bmpWidth: Int` and `bmpHeight: Int` specifies the width and height of the original bitmap
* `left: Int`, `top: Int`, `right: Int`, `bottom: Int`, `width: Int` and `height: Int` specifies the integral region of the slice

### Reading pixels from a BmpSlice

```kotlin
// Individual pixels
val color: RGBA = bmpSlice.getRgba(x, y)
bmpSlice.setRgba(x, y, color)

// A region of pixels
val pixels: RgbaArray = bmpSlice.readPixels(x, y, width, height)

// As a bitmap
val pixels: Bitmap = bmpSlice.extract()
```

### Getting a slice from a Bitmap

All method variants have an optional `name: String? = null` parameter, no set a name for the slice.

```kotlin
val bmpSlice: BitmapSlice<T> = bitmap.slice() // A slice covering the whole region
val bmpSlice = bitmap.slice(bounds = RectangleInt(x, y, width, height), name = "name of the slice")
val bmpSlice = bitmap.sliceWithBounds(left, top, right, bottom)
val bmpSlice = bitmap.sliceWithSize(x, y, width, height)
```

### Sub-slicing BmpSlice

Similar to slicing a Bitmap, you can also sub-slice a BmpSlice:

```kotlin
val subBmpSlice = bmpSlice.slice(Rectangle())
val subBmpSlice = bmpSlice.slice(RectangleInt())
val subBmpSlice = bmpSlice.sliceWithSize(x, y, width, height)
val subBmpSlice = bmpSlice.sliceWithBounds(left, top, right, bottom)
```

### Splitting a BitmapSlice<T> in an array of smaller slices

For example, when we have a bitmap representing TileSet, with regions of a specified size, we want to create an arbitrary number of slices of an specified size.

```kotlin
val slices: List<BitmapSlice<T>> = bmpSlice.splitInRows(16, 16)
```

For example if we have an image of 256x256, and we split in 64x64, we will have a list of 4*4 slices.

It will have the following order:
```
 0  1  2  3
 4  5  6  7
 8  9 10 11
12 13 14 15
```
