---
permalink: /imaging/atlas/
group: imaging
layout: default
title: "Imaging Atlas"
title_short: Atlas
description: KorIM supports creating, loading and saving atlases.
fa-icon: fa-atlas
priority: 50
status: new
---


## Loading atlases

You can read atlases from files with the `VfsFile.readAtlas` extension. It detects and supports three different atlas formats: text-based, json-based and xml-based.

```kotlin
val atlas = resourcesVfs["file.atlas"].readAtlas(asumePremultiplied = false)
```

## General concepts

* `Atlas Page` - an image part of the atlas containing smaller images inside with all the elements. Usually this image is uploaded to the GPU, and reference smaller images in there
* `width` & `height` - the dimensions of the atlas pages (usually power of two 512x512, 2048x2048)
* Slice, subimage or packaged item - A smaller image that is inside the atlas
* Region - the bounds of one of the slice/subimage/packed item
* `border` - usually called extrusion. For each slice/subimage, it is usual to extend some pixels in the edge, because when OpenGL linear-samples that part of the image might end getting extra pixels outside the region. A value of 2 is pretty safe

## MutableAtlas

`MutableAtlas` is a class for generating an atlas that supports adding more slices over the time. Because of that, the packaging might not be optimal.

> You should try to add bigger images first, and sort them descendently for the best results

### Construction

```kotlin
val atlas = MutableAtlas<Unit>()
val atlas = MutableAtlas<Unit>(width = 512, height = 512, border = 2, allowToGrow = true, growMethod = GrowMethod.NEW_IMAGES)
```

> The border determines the number of pixels extruded from the edges, to avoid artifacts when linear sampling in OpenGL. Usually a value of 2 is pretty safe.

### GrowMethod

This `MutableAtlas.GrowMethod` enum serves to indicate the growing strategy, when the image you try to add doesn't fit in the remaining space.

You can either:

* Regenerate a new atlas from scratch but with a bigger size `GROW_IMAGE`
* Generate new images associated to the atlas `NEW_IMAGES`

```kotlin
enum class GrowMethod { GROW_IMAGE, NEW_IMAGES }
```

### Entry

The `MutableAtlas.Entry<T>` class, is a holder containing the image slice inside the atlas and an associated `data` class that might hold extra information associated to that Bitmap and this atlas.

```kotlin
data class Entry<T>(val slice: BitmapSlice<Bitmap32>, val data: T) {
    val name get() = slice.name
}
```

### Adding elements to the atlas

```kotlin
val entry = atlas.add(bitmap32, data)
val entry = atlas.add(bmpSlice, data)

// You can also provide a name for the slice
val entry = atlas.add(bmpOrBmpSlice, data, name = "test")
```

If your generic type is `Unit`, you can omit the data alltoghether:

```kotlin
val entry = atlas.add(bitmap32)
val entry = atlas.add(bmpSlice)
```

### Getting all the atlas bitmaps and entries

* `atlas.width`, `atlas.height` - the size of the atlas pages in pixels
* `atlas.allBitmaps: List<Bitmap32>` - usually one, the atlas pages, each big image contains all the slices/subimages added while constructing
* `atlas.entries` - all the `Entry` classes, referencing the data class + the bitmap slice
* `atlas.entriesByName: Map<String, Entry<T>>` - all the entries associated by name
* `atlas.size` - number of entries in this atlas

### Converting into an immutable atlas

```kotlin
val atlas: Atlas = mutableAtlas.toImmutable()
```

## AtlasLookup, Atlas, AltasInfo

### AtlasLookup

The AtlasLookup interface serves for getting `Atlas.Entry` elements by name.

```kotlin-
interface AtlasLookup {
    // Main interface method
    fun tryGetEntryByName(name: String): Atlas.Entry?

    // Helpers
    fun tryGet(name: String): BmpSlice?
    operator fun get(name: String): BmpSlice
}
```

### Atlas

You can construct an atlas from a list of textures and an optional AtlasInfo instance (you can also read it from a source with `VfsFile.readAtlas`):

```kotlin
val atlas = Atlas(listOf(bmpSlice1, bmpSlice2))
val atlas = Atlas(texture, atlasInfo)
val atlas = Atlas(mapOf("sliceName1" to bmpSlice1, ...), atlasInfo)
```

### AtlasInfo

This class is in charge of containing all the possible information and meta information defined in standard atlas formats:

* AtlasInfo
    * Meta
        * Layer
        * Slice
            * Key
        * FrameTag
    * Rect
    * Slice
    * Page
        * Region
    
## AtlasPacker

An immutable variant of MutablePacker. It is an object defining a pack method.

```kotlin
// Pack a few Bitmap Slices
val result: AtlasPacker.Result<BmpSlice> = AtlasPacker.pack(listOf(bmpSlice1, bmpSlice2), maxSide = 2048, maxTextures = 1, borderSize = 2, fileName = "atlas.png")

// You can also pack with associated data
val result = AtlasPacker.pack(listOf(myInfoClass1 to bmpSlice1, myInfoClass2 to bmpSlice2, ...), ...)
```

```kotlin
data class Result<T>(val atlases: List<AtlasResult<T>>) : AtlasLookup 

data class AtlasResult<T>(val tex: Bitmap32, val atlas: Atlas, val packedItems: List<Entry<T>>) : AtlasLookup

data class Entry<T>(val item: T, val originalSlice: BmpSlice, val slice: BitmapSlice<Bitmap32>, val rectWithBorder: Rectangle, val rect: Rectangle)
```

## BinPacker (KorMA)

KorMA provides a BinPacker class that serves for organizing 2d rectangles in a space without overlapping, and trying to be as space efficient as possible.
