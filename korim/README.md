<p align="center"><img alt="Korim" src="https://raw.githubusercontent.com/korlibs/korlibs-logos/master/128/korim.png" /></p>

<h1 align="center">Korim</h1>

<p align="center">Kotlin cORoutines IMaging utilities for Multiplatform Kotlin</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/com.soywiz.korlibs.korim/korim"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/com.soywiz.korlibs.korim/korim"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: <https://docs.korge.org/korim/>

Use with gradle:

```
repositories {
    mavenCentral()
}

dependencies {
    compile "com.soywiz.korlibs.korim:korim:$korimVersion"
}
```

### Bitmap classes

Bitmap base class + Bitmap8 and Bitmap32.
And other fancy bitmaps: BitmapIndexed as base + Bitmap1, Bitmap2, Bitmap4 and BitmapChannel.

### Image Formats

Korim provides utilities for reading and writing some image formats without any kind of additional dependency.

PNG, JPG, TGA, BMP, ICO, PSD(WIP) and DDS (DXT1, DXT2, DXT3, DXT4 and DXT5).

### Native Image Formats

Korim also allows to use native image readers from your device for maximum performance for standard image formats.

### Color Formats

Korim provides color formats to convert easily and fast and to perform mixing, de/premultiplication and other operations quickly.

### Vectorial Image Formats

Korim supports loading, rasterizing and drawing vectorial SVG files.

### Native vectorial rendering

Korim provides a single interface for vector rendering so you can use a single interface
and leverage JavaScript Canvas, AWT's Graphics2D, Android Canvas or any other rasterizer exposed by korim implementations.
It also allows to convert shapes into SVG.
Korim includes a feature to draw shapes with fills in contact without artifacts in a portable way by multisampling.
Useful for offline rasterizers.

### AWT Utilities

Korim provides AWT utilities to convert bitmaps into AWT BufferedImages, and to display them.
These are just extensions so they are not referenced from the main code.
And if you use native image loading, you can display those images as fast as possible without any conversion at all.

### Native Fonts

Korim provides native font rendering. You can rasterize glyph fonts on all targets without
actually including any font, using device fonts.

### TTF Reading and rendering

Korim provides a pure Kotlin-Common TTF reader, and using native vectorial rendering allows you to
render glyphs, texts and to get advanced font metrics.

### Korio integration

Korim provides korio integration adding `VfsFile.readBitmap()` that allows Bitmap reading easily
and faster (with native implementations) in some targets like browsers.
