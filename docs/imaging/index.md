---
permalink: /imaging/
group: imaging
layout: default
title: "Image"
title_short: "Introduction"
fa-icon: fa-image
priority: 0
artifact: 'com.soywiz.korge:korge-core'
package: korlibs.image
---

<img alt="KorIM" src="/i/logos/korim.svg" style="float:left;width:128px;height:128px;" />

**[KorIM](https://github.com/soywiz/korim)** is a library for image loading, writing and processing. It allows to convert between image and color formats. Also includes tools for vector images and fonts.

This library is able to load PNG, JPEGs, BMPs, TGAs, PSDs and SVG vector images.
Also allows rendering vectorial images using native rasterizers. 
Also allows to use native image loaders for the fastest performance.

{% include stars.html project="korge" central="com.soywiz.korlibs.korim/korim" %}



## Pages

{% include toc.html context="/korim/" %}

### Bitmap classes

Bitmap base class + Bitmap8 and Bitmap32.
And other fancy bitmaps: BitmapIndexed as base + Bitmap1, Bitmap2, Bitmap4
Ad BitmapChannel

### Image Formats

Korim provides utilities for reading and writing some image formats without any kind of additional dependency.

PNG, JPG, TGA, BMP, ICO, PSD and DDS (DXT1, DXT2, DXT3, DXT4 and DXT5).

### Color Formats

Korim provides color formats to convert easily and fast and to perform, mixing, de/premultiplication and other operations quickly.

### Vectorial Image Formats

Korim supports loading, rasterizing and drawing vector SVG files.

### Native vectorial rendering

It provides a single interface for vector rendering.
So you can use a single interface and leverage JavaScript Canvas,
AWT's Graphics2D and Android Canvas.
It allows converting shapes into SVG.
Also allows to draw shapes with fills in contact without artifacts in a portable way.

### AWT Utilities

Korim provides AWT utilities to convert bitmaps into AWT BufferedImages, and to display them.
These are just extensions so not referenced from the main code.

### Native Fonts

Korim provides native font rendering. You can rasterize glyph fonts on all targets.

### Korio integration

Korim provides korio integration adding `VfsFile.readBitmap()` that allows Bitmap reading easily
and faster (with native implementations) in some targets like browsers.
