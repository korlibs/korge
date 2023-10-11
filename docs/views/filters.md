---
permalink: /views/filters/
group: views
layout: default
title: View Filters
title_short: Filters
description: Views can have filters attached that change how the view and its children are displayed. 
fa-icon: fa-adjust
priority: 70
---

{% include sample.html sample="FiltersScene" %}

```kotlin
var View.filter: Filter? = null
```

## ComposedFilter

You can apply several filters to a view using this:

```kotlin
class ComposedFilter(val filters: List<Filter>) : Filter()
```

## IdentityFilter

This filter can be used to no apply filters at all. But serves for the subtree to be rendered in a texture.
This is useful for example to change the alpha of a complex object as a whole,
instead of affecting each alpha of the descendant views.

```kotlin
object IdentityFilter : Filter()
```

```kotlin
container {
	scale(0.8)
	position(24, 24)
	alpha = 0.25
	filter = IdentityFilter
	image(resourcesVfs["korge.avif"].readBitmap())
	image(resourcesVfs["korge.avif"].readBitmap()) {
		position(64, 64)
	}
}
```

<div style="display:flex;width:100%;max-width:100%;">
<img src="/i/filters/identity_complex_off.avif" style="max-width:50%;" />
<img src="/i/filters/identity_complex_on.avif" style="max-width:50%;" />
</div>

## ColorMatrixFilter

The color matrix filter allows to transform the colors in your views
using a full 4x4 matrix. It already provides a computed matrix to show images as greyscale.

```kotlin
class ColorMatrixFilter(var colorMatrix: Matrix3D, var blendRatio: Double) : Filter() {
    companion object {
        val GRAYSCALE_MATRIX = Matrix3D.fromRows(
            0.33f, 0.33f, 0.33f, 0f,
            0.59f, 0.59f, 0.59f, 0f,
            0.11f, 0.11f, 0.11f, 0f,
            0f, 0f, 0f, 1f
        )
        
        val IDENTITY_MATRIX = Matrix3D.fromRows(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    }
}
```

```kotlin
view.filter = ColorMatrixFilter(ColorMatrixFilter.GRAYSCALE_MATRIX)
```

![](/i/filters/color_matrix.avif)

## Convolute3Filter

Allows to apply 3x3 convolution filters (also called [Kernel](https://en.wikipedia.org/wiki/Kernel_(image_processing))) in realtime to the View.

This filter exposes matrices for making 3x3 gaussian blurs, box blurs, and edge detection.

```kotlin
class Convolute3Filter(var kernel: Matrix3D) : Filter() {
    companion object {
        val KERNEL_GAUSSIAN_BLUR: Matrix3D
        val KERNEL_BOX_BLUR: Matrix3D
        val KERNEL_IDENTITY: Matrix3D
        val KERNEL_EDGE_DETECTION: Matrix3D
    }
}
```

```kotlin
view.filter = Convolute3Filter(Convolute3Filter.KERNEL_EDGE_DETECTION)
```

![](/i/filters/edge_detection.avif)

## BlurFilter

Allows to apply a blur filter with an adjustable radius using the [Kawase](https://software.intel.com/content/www/us/en/develop/videos/improving-real-time-gpu-based-image-blur-algorithms-kawase-blur-and-moving-box-averages.html?language=es)
approximation algorithm.

```kotlin
class BlurFilter(initialRadius: Double = 1.0) : Filter {
    var radius: Double
}
```
```kotlin
view.filter = BlurFilter(8.0)
```

![](/i/filters/blur.avif)

## WaveFilter and PageFilter

Can be used to simulate pages from books:

```kotlin
class PageFilter(
    var hratio: Double = 0.5,
    var hamplitude0: Double = 0.0,
    var hamplitude1: Double = 10.0,
    var hamplitude2: Double = 0.0,
    var vratio: Double = 0.5,
    var vamplitude0: Double = 0.0,
    var vamplitude1: Double = 0.0,
    var vamplitude2: Double = 0.0
) : Filter()
```

```kotlin
view.filter = PageFilter(hamplitude1 = 64.0)
```

![](/i/filters/page.avif)

```kotlin
class WaveFilter(
    var amplitudeX: Int = 10,
    var amplitudeY: Int = 10,
    var crestCountX: Double = 2.0,
    var crestCountY: Double = 2.0,
    var cyclesPerSecondX: Double = 1.0,
    var cyclesPerSecondY: Double = 1.0,
    var time: Double = 0.0
) : Filter()
```

![](/i/filters/wave.avif)

## SwizzleColorsFilter

Serves to do component swizzling (RGBA) per pixel (that's interchanging component colors):

```kotlin
class SwizzleColorsFilter(var swizzle: String = "rgba") : Filter()
```

```kotlin
view.filter = SwizzleColorsFilter("bgra")
```

![](/i/filters/swizzle_color.avif)

## DitheringFilter

It can simulate a dithering from a posterization:

```kotlin
view.filter = DitheringFilter(levels = 2.0)
```

![](/i/filters/dithering_filter.avif)

## Backdrop Filters

It is possible to render filters as backdrop

TODO

## Custom Filters

KorGE supports creating custom filters by providing shader programs.
