---
permalink: /atlas/
group: reference
layout: default
title: Atlas
title_prefix: KorGE
fa-icon: fas fa-map
priority: 35
---



## Overview

Atlas is a way to pack several images into a single bigger image
while being able to reference smaller parts of that image.

Why would one want to do that? Well, in computer graphics there is
a thing called batch rendering (each batch is one communication with the GPU),
when using fragments of a single big image
you can reduce the number of those batches and making the game to perform much better.

Also atlases usually put some space between images, that's because when doing mipmaping
or linear sampling, sometimes near pixels are interpolated / mixed.

![](/i/atlas.avif)

![](/i/logos.atlas.avif)

```json
{
	"frames": [
		{ "filename": "korge.png", "frame": {"x": 2,"y": 2,"w": 64,"h": 64}, "rotated": false, "sourceSize": {"w": 64,"h": 64}, "spriteSourceSize": {"x": 0,"y": 0,"w": 64,"h": 64}, "trimmed": false },
		{ "filename": "korim.png", "frame": {"x": 2,"y": 70,"w": 64,"h": 64}, "rotated": false, "sourceSize": {"w": 64,"h": 64}, "spriteSourceSize": {"x": 0,"y": 0,"w": 64,"h": 64}, "trimmed": false },
		{ "filename": "korau.png", "frame": {"x": 2,"y": 138,"w": 64,"h": 64}, "rotated": false, "sourceSize": {"w": 64,"h": 64}, "spriteSourceSize": {"x": 0,"y": 0,"w": 64,"h": 64}, "trimmed": false}
	],
	"meta": { "app": "korge", "format": "RGBA8888", "image": "logos.atlas.png", "scale": 1.0, "size": { "w": 68,"h": 204}, "version": "1.0.0" }
}
```

## Loading atlases

KorGE supports `.json`-based (array and hash) and `.xml`-based atlases.
From a `VfsFile`, you can call the `.readAtlas()` extension method.
It will detect the JSON and XML formats automatically and will read it.

So for example:
```kotlin
val atlas: Atlas = resourcesVfs["character.atlas.json"].readAtlas()
```

## Extracting images from the Atlas

You can just use the `operator fun get` to access individual images

```kotlin
val image: BmpSlice = atlas["image.png"]
```

You can access the list of available entries with:

```kotlin
val entries: List<Atlas.Entry> = atlas.entries
```

## Automatic atlas generation

KorGE build plugin supports generating an atlas from
a set of images directly. Just create a file with the `.atlas` extension
in your `resources` folder. In that file you have to place a name of the 
folder whose images will be included in the atlas. Then you need to refer
to the generated `.atlas.json` file.

For example, if you have a `logos` folder in your resources, you can get
the atlas this way:
```kotlin
val atlas: Atlas = resourcesVfs["logos.atlas.json"].readAtlas()
```

## Extract several `SpriteAnimation`s from an Atlas

You can use atlases to store one or several animations.
KorGE allows you to build `SpriteAnimation` from an Atlas easily
either from all the frames in the atlas, or just with a subset.

```kotlin
val animation1 = atlas.getSpriteAnimation() // Includes all the images
val animation2 = atlas.getSpriteAnimation("RunRight") // Includes the images starting with RunRight 
val animation3 = atlas.getSpriteAnimation(Regex("beam\\d+.png")) // Includes the images starting with beam, following a number and ending with .png
```

## Runtime Atlas Generation

```kotlin
val atlas = MutableAtlasUnit()
val slice = resourcesVfs["test.png"].readBitmapSlice(atlas = atlas)
```
