---
permalink: /resolutions/
group: reference
layout: default
title: Resolutions
title_prefix: KorGE
fa-icon: fas fa-expand-arrows-alt
priority: 35
---



## Overview

<!-- https://docs.google.com/drawings/d/1Zoql1LIfBDdp44reYey5dfPyThOd89rFyCeRw9GUMfc/edit?usp=sharing -->
![](/i/virtual-size.avif)

## Video-tutorial

{% include youtube.html video_id="81IG0ld5w-8" %}

## Resolution

In KorGE you don't have to deal with different resolutions. That's something handled by the engine.

When initializing your game, you define a `virtualWidth` and a `virtualHeight` (and a `width` and `height`).
In windowed platforms, the `width` and `height` will be the size of the window, while on full screen platforms
or the browser where javascript can't resize the window, those values would be ignored.

The `virtualWidth` and `virtualHeight` define your in-game dimensions. So for example,
if the size of the window is 1920x1080 but you defined a virtual size of 1280x720, and you place an object
at 1279x719, the object will be placed at the 1918x1078 screen dimensions.

### Setting the resolution using DSL

```kotlin
suspend fun main() = Korge(width = 1280, height = 720, virtualWidth = 640, virtualHeight = 480) {
    // ...
}
```

### Setting the resolution using the Module System

```kotlin
suspend fun main() = Korge(Korge.Config(module = MyModule)) {
    // ...
}
object MyModule : Module() {
	override val size = SizeInt(640, 480) // Virtual Size
	override val windowSize = SizeInt(1280, 720) // Window Size
}
```

## Aspect Ratio

So, if you are defining a `virtualWidth` and a `virtualHeight` that by itself defines a fixed aspect ratio,
how do you handle different aspect ratios or windows resizes gracefully?

### Black bars

With the default configuration KorGE will place "black bars" (clip your content) to a container that is centered,
filling the window keeping the aspect ratio.

This behaviour is defined by the `scaleAnchor`, `scaleMode` and `clipBorders` parameters in the game initialization:

```kotlin
suspend fun main() = Korge(scaleAnchor = Anchor.MIDDLE_CENTER, scaleMode = ScaleMode.SHOW_ALL, clipBorders = true) {
}
```

### `clipBorders = false` & Dealing with the extra space & dockedTo

With the default `scaleMode = ScaleMode.SHOW_ALL` but setting `clipBorders = false`, your get your content centered
keeping the aspect ratio, and filling all the available space. But now, pixels that would go in the border are being
displayed.

You can use the `dockedTo` component / view decorator to automatically update the view position when a resize
or rotation happens.

```kotlin
val container = container {
    solidRect(32, 32, Colors.RED).xy(0, 0).anchor(1.0, 0.0)
    solidRect(32, 32, Colors.BLUE).xy(-32, 0).anchor(1.0, 0.0)
}

// This will keep your container 0,0 coordinate at the top-right of the window independently to the virtual aspect ratio
container.dockedTo(anchor = Anchor.TOP_RIGHT, scaleMode = ScaleMode.NO_SCALE)
```

## Dealing with resize events

Whenever the window is moved or resized, a `ReshapeEvent` is triggered to the stage.

```kotlin
stage.addEventListener<ReshapeEvent> { e -> // e.x, e.y, e.width, e.height
}
```

But normally you would want to attach it to a view, so when the view is not attached to the stage and not referenced,
you won't have leaks.

```
view.addComponent(object : ResizeComponent {
    override fun resized(views: Views, width: Int, height: Int) {
        // ...
    }
})
```

Note, that if you use the `dockedTo` component, you might not need this at all.

### Actual/Extended Virtual Size

```kotlin
// The Window Size
val Views.nativeWidth: Int
val Views.nativeHeight: Int

// Your defined Virtual Size
val Views.virtualWidth: Int
val Views.virtualHeight: Int

// When scaleAnchor != Anchro.TOP_LEFT, the left and top values here will contain the "border" gap between the top-left of the window and your actual content in the virtual space
val Views.actualVirtualLeft: Int
val Views.actualVirtualTop: Int

// Greater or equal that the virtual size. This pair will have the aspect ratio of the window (not the virtual size aspect ratio)
val Views.actualVirtualWidth: Int
val Views.actualVirtualHeight: Int

val Views.virtualLeft: Int
val Views.virtualTop: Int
val Views.virtualRight: Int
val Views.virtualBottom: Int
```
