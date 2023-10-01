---
permalink: /imaging/fonts/
group: imaging
layout: default
title: "Fonts"
title_prefix: KorGE
fa-icon: fa-font
priority: 70
---

KorGE supports Device, Bitmap and TTF fonts on all the targets.

BitmapFonts are handled by KorIM.



## Bitmap fonts

You can read a BitmapFont by using the `readBitmapFont` from KorIM.
It supports the standard XML and FNT bitmap font formats.

```
suspend fun VfsFile.readBitmapFont(imageFormat: ImageFormat = RegisteredImageFormats): BitmapFont
```

You can use BitmapFonts with the [Text view](/views/standard/#text):

```
text("Hello World", textSize = 32.0, font = myBitmapFont)
```

## Device fonts

You can construct BitmapFonts, automatically building atlases, by constructing the BitmapFont like this:

```kotlin
fun BitmapFont(fontName: String, fontSize: Int, chars: String = BitmapFontGenerator.LATIN_BASIC, mipmaps: kotlin.Boolean = true)
```

## TTF fonts

You can render TTF fonts to bitmaps to later using them with:

```kotlin
val font = TtfFont(resourcesVfs["myfont.ttf"].readAll().openSync())
val bitmap = NativeImage(512, 128).apply {
    getContext2d().fillText(font, "HELLO WORLD", size = 32.0, x = 0.0, y = 0.0, color = Colors.RED, origin = TtfFont.Origin.TOP)
}
```
