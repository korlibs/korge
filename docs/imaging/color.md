---
permalink: /imaging/color/
group: imaging
layout: default
title: "Imaging Colors"
title_short: Colors
description: KorIM support several color formats, packed inline classes and conversion between them as well as mixing, de/premultiplication and other optimized operations.
fa-icon: fa-palette
priority: 10
---

## ColorFormat

All color formats implements the `ColorFormat` interface.

```kotlin
interface ColorFormat {
    val bpp: Int
	fun getR(v: Int): Int
	fun getG(v: Int): Int
	fun getB(v: Int): Int
	fun getA(v: Int): Int
	fun pack(r: Int, g: Int, b: Int, a: Int): Int
}
```

## PaletteColorFormat

```kotlin
class PaletteColorFormat(val palette: RgbaArray) : ColorFormat {
    override val bpp = 8
}
```

## RGBA-based ColorFormat

* `ARGB`, `BGRA`
* `RGB`, `BGR`

## 16-bit RGBA-based ColorFormat

* `RGBA_4444`
* `RGBA_5551`
* `RGB_555`
* `RGB_565`
* `BGRA_4444`
* `BGR_555`
* `BGR_565`
* `BGRA_5551`

## Floating-Point RGBA

Not a direct `ColorFormat`, but a way to store mutable colors
allowing overflowing and special operations.

These clases store each component separately taking a `Float` or `Double` per component.

* `RGBAd`
* `RGBAf`

## CMYK ColorFormat

CMYK is a color format used for printing. Its components are Cyan, Magenta, Yellow and Black.
There is an inline class for packing CMYK colors and a color format to convert from/between
RGBA colors.

* `CMYK`, `RGBA.toCMYK()`, `CMYK.toRGBA()` 

## YCbCr and YUVA ColorFormat

KorIM supports a color format based on Luma and Two chromiance components.
In addition to the companion object ColorFormat, these color formats
provide inline classes wrapping an integer.

* `YCbCr`, `RGBA.toYCbCr()`, `YCbCr.toRGBA()`
* `YUVA`, `RGBA.toYUVA()`, `YUVA.toRGBA()`

## Extended API

### Determine the storage size

To determine the number of bytes a number of pixels would take to store
in a specific format, we can use `numberOfBytes` or `bytesPerPixel` with
a multiplication:

```kotlin
fun ColorFormat.numberOfBytes(pixels: Int): Int
val ColorFormat.bytesPerPixel: Double
```

### Convert into/from RGBA

If we want to convert an Int-packed value into/from RGBA,
we can use these functions: 

```kotlin
fun ColorFormat.toRGBA(v: Int): RGBA
fun ColorFormat.packRGBA(c: RGBA): Int
```

### Getting floating-point RGBA components

If we want to get values between `0-1`
we can use the exntesion methods that convert
the component values between `0-255` to `Float` or `Double`.

```kotlin
fun ColorFormat.getRf(v: Int): Float
fun ColorFormat.getGf(v: Int): Float
fun ColorFormat.getBf(v: Int): Float
fun ColorFormat.getAf(v: Int): Float

fun ColorFormat.getRd(v: Int): Double
fun ColorFormat.getGd(v: Int): Double
fun ColorFormat.getBd(v: Int): Double
fun ColorFormat.getAd(v: Int): Double
```

### Packing and unpacking colors into ByteArray and Bitmap32

```kotlin
fun ColorFormat.unpackToRGBA(packed: Int): RGBA
fun ColorFormat.convertTo(color: Int, target: ColorFormat): Int
fun ColorFormat.decode(data: ByteArray, dataOffset: Int, out: RgbaArray, outOffset: Int, size: Int, littleEndian: Boolean = true)
fun ColorFormat.decode(data: ByteArray, dataOffset: Int = 0, size: Int = (data.size / bytesPerPixel).toInt(), littleEndian: Boolean = true): RgbaArray
fun ColorFormat.decodeToBitmap32(width: Int, height: Int, data: ByteArray, dataOffset: Int = 0, littleEndian: Boolean = true): Bitmap32 
fun ColorFormat.decodeToBitmap32(bmp: Bitmap32, data: ByteArray, dataOffset: Int = 0, littleEndian: Boolean = true): Bitmap32 
fun ColorFormat.encode(colors: RgbaArray, colorsOffset: Int, out: ByteArray, outOffset: Int, size: Int, littleEndian: Boolean = true)
fun ColorFormat.encode(colors: RgbaArray,colorsOffset: Int = 0,size: Int = colors.size,littleEndian: Boolean = true)
fun ColorFormat16.encode(colors: IntArray, colorsOffset: Int, out: ShortArray, outOffset: Int, size: Int): Unit
fun ColorFormat32.encode(colors: IntArray, colorsOffset: Int, out: IntArray, outOffset: Int, size: Int): Unit
```

## `RGBA` and `RGBAPremultiplied`

These Color Formats and color packings (inline classes wrapping Int) represent
32-bit (8-bit per component) Red Green Blue and Alpha colors.
The premultiplied version means that RGB components have been multiplied by its alpha component.
Premultiplied alpha is useful when performing blending since less operations are required. 

### Converting color into string

```kotlin
val hexString: String get() ="#%02x%02x%02x%02x".format(r, g, b, a)
val htmlColor: String get() = "rgba($r, $g, $b, $af)"
val htmlStringSimple: String get() = "#%02x%02x%02x".format(r, g, b)
```

### Mixing two colors

To mix two colors together:

* `RGBA.mix(RGBA)`
* `RGBA.Companion.mix(dst: RGBA, src: RGBA)`

To combine two colors weighting one over another:

* `RGBA.Companion.mix(RGBA, RGBA, factor)`

### Converting between premultiplied and premultiplied

## `RgbaArray` and `RgbaPremultipliedArray`

`RgbaArray` and `RgbaPremultipliedArray` inline-wraps an IntArray and provides ways
to set, get and fill values using `RGBA` and `RGBAPremultiplied`

## ColorTransform

A ColorTransform represents multiplications and additions per component.
You can achieve simple color transformations with this class.

```kotlin
val identity = ColorTransform(
    1, 1, 1, 1, // Multiplicative
    0, 0, 0, 0  // Additive
)
``` 
 
## ColorMatrix

A colorMatrix represents a 5x4 matrix multiplying each component by each other
and providing additions to each component.

```kotlin
val identity = ColorMatrix(
    1, 0, 0, 0,  0,
    0, 1, 0, 0,  0,
    0, 0, 1, 0,  0,
    0, 0, 0, 1,  0,
)
``` 

* `RGBA.transform(colorMatrix)`
