---
permalink: /views/sprites/
group: views
layout: default
title: "Sprites"
title_prefix: KorGE
fa-icon: fas fa-walking
priority: 30
---



## Overview

Sprites are simple views that can play simple sequence-based animations.
Instead of using Skeletal-based or tween-based animation,
sprite animations are just a sequence of images, and the sprites
just play them.

When using along a single atlas, you can have lots of these sprites visible
at the same time at a decent framerate.

This page covers `Sprite` and `SpriteAnimation` as well as `Atlas`

## Creating a `SpriteAnimation`

There are two ways of creating a `SpriteAnimation`:
manually or selecting some frames from an `Atlas`.

### Manually creating a `SpriteAnimation`

```kotlin
val spriteMap = resourcesVfs["explosion.png"].readBitmap()
val explosionAnimation = SpriteAnimation(
    spriteMap = spriteMap,
    spriteWidth = 128,
    spriteHeight = 128,
    marginTop = 0,
    marginLeft = 0,
    columns = 8,
    rows = 8,
    offsetBetweenColumns = 0,  
    offsetBetweenRows = 0,  
)

val explosion = sprite(explosionAnimation)
explosion.playAnimationLooped()
```

### Creating a `SpriteAnimation` using an `Atlas`

```kotlin
val adventurerSprite = resourcesVfs["adventurer.xml"].readAtlas()

val runAnimation = adventurerSprites.getSpriteAnimation(prefix = "run")
val jumpAnimation = adventurerSprites.getSpriteAnimation(prefix = "jump")
val standAnimation = adventurerSprites.getSpriteAnimation(prefix = "stand")

val adventurer = sprite(standAnimation)
adventurer.playAnimationLooped()
```

## API

### Sprite

DSL:

```kotlin
fun Container.sprite(initialAnimation: SpriteAnimation, anchorX: Double , anchorY: Double)
```

Sprite class:

```kotlin
open class Sprite(
    bitmap: Bitmap /*| BmpSlice | SpriteAnimation*/,
    anchorX: Double = 0.0,
    anchorY: Double = anchorX,
    hitShape: VectorPath? = null,
    smoothing: Boolean = true
) : Image(bitmap, anchorX, anchorY, hitShape, smoothing) {
    val onAnimationCompleted : Signal<SpriteAnimation>
    val onAnimationStopped : Signal<SpriteAnimation>
    val onAnimationStarted : Signal<SpriteAnimation>
    var spriteDisplayTime: TimeSpan = 50.milliseconds

    fun playAnimation(times: Int, spriteAnimation: SpriteAnimation?, spriteDisplayTime: TimeSpan, startFrame: Int, reversed: Boolean)
    fun playAnimation(spriteAnimation: SpriteAnimation?, spriteDisplayTime: TimeSpan, startFrame: Int, reversed: Boolean)
    fun playAnimationForDuration(duration: TimeSpan, spriteAnimation: SpriteAnimation?, spriteDisplayTime: TimeSpan, startFrame: Int, reversed: Boolean)
    fun playAnimationLooped(spriteAnimation: SpriteAnimation?, spriteDisplayTime: TimeSpan, startFrame: Int, reversed: Boolean)

    fun stopAnimation()
    fun setFrame(index: Int)
}
```

SpriteAnimation class:

```kotlin
fun SpriteAnimation(sprites: List<BmpSlice>, defaultTimePerFrame: TimeSpan): SpriteAnimation
fun SpriteAnimation(
    spriteMap: Bitmap,
    spriteWidth: Int, spriteHeight: Int,
    marginTop: Int, marginLeft: Int,
    columns: Int, rows: Int,
    offsetBetweenColumns: Int, offsetBetweenRows: Int
): SpriteAnimation

class SpriteAnimation {
    val sprites: List<BmpSlice>
    val defaultTimePerFrame: TimeSpan
    val spriteStackSize: Int
    val size: Int
    val firstSprite: BmpSlice
    fun getSprite(index: Int): BmpSlice
    operator fun get(index: Int): BmpSlice
}
```

`SpriteAnimation` from Atlas:

```kotlin
fun Atlas.getSpriteAnimation(prefix: String, defaultTimePerFrame: TimeSpan): SpriteAnimation
fun Atlas.getSpriteAnimation(regex: Regex, defaultTimePerFrame: TimeSpan): SpriteAnimation
```

## Video-tutorials

{% include youtube.html video_id="fY7a2xrHL9g" %}
{% include youtube.html video_id="atElzA2jYkQ" %}

