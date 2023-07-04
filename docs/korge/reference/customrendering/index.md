---
layout: default
title: "Custom Rendering"
title_prefix: KorGE
fa-icon: far fa-paint-roller
priority: 300
---

While provided views are usually enough for most cases,
some time people want to render their own views, or not rely on
the view tree for rendering, and render everything manually.
In those cases, it is possible to create custom views or render things manually
directly drawing geometry, or by using a batcher or a shape builder.

## Overriding a `View`

In the case you want a custom view where you can specify rendering code, you can use this template.
You need to provide a `renderInternal` where you render things.

```kotlin
class MyCustonView : View() {
    override fun renderInternal(ctx: RenderContext) {
        // Custom Render Code here
        ctx.useBatcher { batcher ->
            batcher.drawQuad(ctx.getTex(Bitmaps.white), x = 0f, y = 0f, width = 100f, height = 100f, m = globalMatrix, filtering = false, colorMul = Colors.RED, blendMode = renderBlendMode, program = null)
        }
        ctx.useLineBatcher { 
            it.line(Point(0, 0), Point(100, 100), Colors.RED, m = globalMatrix)
        }
    }
    
    override fun getLocalBoundsInternal(): Rectangle {
        return Rectangle(0, 0, 100, 100)
    }
}
```

## `RenderableView` to create a view for custom rendering

Since KorGE requires at least one view to work, you can use this predefined view that only requires a callback to start rendering.
For example:

```kotlin
fun main() = Korge {
    renderableView(Size(100, 100)) {
        ctx.useBatcher {
            // ...
        }
        ctx.useLineBatcher {
            // ...
        }
        // Or using RenderContext2D
        ctx2d.drawText("Hello", DefaultTtfFontAsBitmap)
        ctx2d.simplePath(buildVectorPath { 
            circle(Point(0, 0), 100f)
        })
    }
}
```

## `BatchBuilder2D`

`BatchBuilder2D` is designed to buffer geometry, typically quads, to later emit a command to the GPU to render
all the geometries at once.  With a `RenderContext` that is provided to the render function, you can call:

```kotlin
ctx.useBatcher { batcher ->
    // Your code here...
}
```

Methods here require a `Texture` or `TextureCoords`.

### Getting textures from `Bitmap` or `BitmapSlice`/`BmpSlice`

`Bitmap` are pixels stored typically on the CPU. But the GPU requires textures.
KorGE provides a managed way to use textures. In the rendering code, you can get a managed Texture auto GCed texture
by calling the `getTex(bitmap)` method like:

```kotlin
val texture: Texture = ctx.getTex(bitmap)
val textureCoords: TextureCoords = ctx.getTex(bitmapSlice)
```

### Rendering a solid rect square

We need a white texture, and then we tint it. We provide the globalMatrix of our view, so all the transformations are taken into account,
and use the default program for rendering (vertex + fragment).
To get a white texture, we use `Bitmaps.white: BmpSlice` and convert it into a `TextureCoords`:

```kotlin
ctx.useBatcher { batcher ->
    batcher.drawQuad(ctx.getTex(Bitmaps.white), x = 0f, y = 0f, width = 100f, height = 100f, m = globalMatrix, filtering = false, colorMul = Colors.RED, blendMode = renderBlendMode, program = null)
}
```

### Render a full TexturedVertexArray

If we want to precompute a set of vertices and later render them, we can use a `TexturedVertexArray` and the `drawVertices` method:

```kotlin
ctx.useBatcher {
    it.drawVertices(
        TexturedVertexArray.fromPointArrayList(pointArrayListOf(Point(0, 0), Point(100, 0), Point(100, 100)), Colors.RED),
        globalMatrix
    )
}
```

## `RenderContext2D`

If we need a higher level API supporting local transformations, vector rendering, etc. we can use `RenderContext2D`,
it is like a `Context2D` but a bit simpler and more limited. Also, this API is not anti-aliased.
For that, you can check the `Graphics` view.

`View.renderCtx2d` sets the view matrix, blending mode, size, multiplication color, etc.

```kotlin
view.renderCtx2d(ctx) {
    keep {
        translate(100.0, 100.0)
        scale(2.0)
        
        drawText("Hello World", DefaultTtfFontAsBitmap)
        simplePath(buildVectorPath { 
             circle(Point(0, 0), 100f)
        }, Colors.RED)
    }
}
```

When using a `RenderableView` that already sets everything, you can just use `this.ctx2d`.

## `AG`

AG provides the lowest possible interface for rendering.
It supports rendering geometry by providing vertices with attributes, indices, shader programs, etc.
Since this is a complex API it is discouraged for most cases, and only if you know what you are doing.
You can search for AG.draw calls in the code to figure out how it the API is used.

```kotlin
ctx.flush(RenderContext.FlushKind.FULL)
val frameBuffer = ctx.currentFrameBufferOrMain
ctx.ag.draw(frameBuffer.base, frameBuffer.info, vertexData = ...)
```
