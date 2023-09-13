<p align="center"><img alt="Korau" src="https://raw.githubusercontent.com/korlibs/korlibs-logos/master/128/korau.png" /></p>

<h1 align="center">Korau</h1>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korau/actions"><img alt="Build Status" src="https://github.com/korlibs/korau/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/korlibs.korau/korau"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/korlibs.korau/korau"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: <https://docs.korge.org/korau/>



<p align="center">
    <img alt="Korio" src="https://raw.githubusercontent.com/korlibs/korlibs-logos/master/128/korio.png" />
</p>

<h2 align="center">Korio</h2>

<p align="center">
    Kotlin I/O : Streams + TCP Client/Server + VFS for Multiplatform Kotlin
</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/korlibs.korio/korio"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/korlibs.korio/korio"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: <https://docs.korge.org/korio/>

Use with gradle:

```
repositories {
    mavenCentral()
}

dependencies {
    implementation "korlibs.korio:korio-jvm:$korioVersion"
}
```

-------

Korim:

<p align="center"><img alt="Korim" src="https://raw.githubusercontent.com/korlibs/korlibs-logos/master/128/korim.png" /></p>

<h1 align="center">Korim</h1>

<p align="center">Kotlin cORoutines IMaging utilities for Multiplatform Kotlin</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/korlibs.korim/korim"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/korlibs.korim/korim"></a>
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
    compile "korlibs.korim:korim:$korimVersion"
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




<p align="center"><img alt="Korte" src="https://raw.githubusercontent.com/korlibs/korlibs-logos/master/128/korte.png" /></p>

<h1 align="center">Korte</h1>
<p align="center">Template Engine for Multiplatform Kotlin</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/korlibs.korte/korte"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/korlibs.korte/korte"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord" /></a>
</p>
<!-- /BADGES -->

### Full Documentation: <https://docs.korge.org/korte/>

## Info:

KorTE is an asynchronous templating engine for Multiplatform Kotlin 1.3+.

It is a non-strict super set of twig / django / atpl.js template engines and can support liquid templating engine as well with frontmatter.

It has out of the box support for [ktor](https://ktor.io/) and [vert.x](https://vertx.io/).

It works on JVM and JS out of the box. And on Native with untyped model data or by making the models implement the [DynamicType](https://github.com/korlibs/korte/blob/7461aa4b7dc496ff1c0e986cdb2c7843891ba325/korte/src/commonMain/kotlin/korlibs/template/dynamic/DynamicType.kt#L61) interface.

Because asynchrony is in its name and soul, it allows to call *suspend*ing methods from within your templates.

## Documentation:

* <https://korlibs.soywiz.com/korte/>

## Live demo

* ACE: <https://korlibs.github.io/korte-samples/korte-sample-browser/web/>
* OLD: <https://korlibs.github.io/kor_samples/korte1/>

## Example

### `resources/views/_base.html`
```liquid
<html><head></head><body>
{% block content %}default content{% endblock %}
</body></html>
```

### `resources/views/_two_columns.html`
```liquid
{% extends "_base.html" %}
{% block content %}
    <div>{% block left %}default left column{% endblock %}</div>
    <div>{% block right %}default right column{% endblock %}</div>
{% endblock %}
```

### `resources/views/index.html`
```liquid
{% extends "_two_columns.html" %}
{% block left %}
    My left column. Hello {{ name|upper }}
{% endblock %}
{% block right %}
    My prefix {{ parent() }} with additional content
{% endblock %}
```

### `code.kt`

```kotlin
val renderer = Templates(ResourceTemplateProvider("views"), cache = true)
val output = templates.render("index.html", mapOf("name" to "world"))
println(output)

class ResourceTemplateProvider(private val basePath: String) : TemplateProvider {
     override suspend fun get(template: String): String? {
         return this::class.java.classLoader.getResource(Paths.get(basePath, template).toString()).readText()
     }
 }

```
