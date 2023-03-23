<h1 align="center">KorGE</h1>
<p align="center">Multiplatform Kotlin Game Engine</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/com.soywiz.korlibs.korge.plugins/korge-gradle-plugin?eh="><img alt="Maven Central" src="https://img.shields.io/maven-central/v/com.soywiz.korlibs.korge.plugins/korge-gradle-plugin"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord&label=Discord" /></a>
</p>
<!-- /BADGES -->

<!-- SUPPORT -->
<h2 align="center">Support korge</h2>
<p align="center">
If you like korge, or want your company logo here, please consider <a href="https://github.com/sponsors/soywiz">becoming a GitHub sponsor ★</a>,<br />
in addition to ensure the continuity of the project, you will get exclusive content.
</p>
<!-- /SUPPORT -->

## Info about the project:

KorGE is a modern multiplatform game engine for Kotlin. Features include:

* Visual editor
  * KorGE offers a powerful editor embedded in IntelliJ IDE
* Bundle support
  * Easily add sourcecode and resources via GitHub
* Debugger
  * Live-debug your games
* 100% Kotlin
  * KorGE is fully written in Kotlin and designed from the ground up to embrace modern and easy coding styles
* Real native multiplatform
  * KorGE gradle plugin allows to target each platform natively: JVM for Android, JS for the Web and native code for iOS and Desktop
* Quick installation
  * Install the KorGE IntelliJ Plugin or clone the “Hello World!” project and start making your own game in less than a minute
* Fully productive
  * Since KorGE targets the JVM, you can develop your game, try it, debug it and test it using IntelliJ IDEA
* Tons of features
  * KorGE is just the last layer of a larger stack (Korlibs) for multimedia development
* Small footprint
  * KorGE has a very small footprint. It has no external dependencies and only uses the libraries available on each platform

For more information, visit:

* <https://korge.org/>
* <https://docs.korge.org/>
* <https://forum.korge.org/>

## Usage:

KorGE and all the other korlibs in a single monorepo.

To use this version in other projects,
you have to publish it locally to mavenLocal,
and then use `999.0.0.999` as version: 

```shell script
./gradlew publishToMavenLocal
```

If you want to make changes and easily try things.
You can run the `korge-sandbox` module that runs
the `src/commonMain/kotlin/Main.kt` file;
you can make experiments there:

```shell script
./gradlew :korge-sandbox:runJvm
./gradlew :korge-sandbox:runJs
./gradlew :korge-sandbox:runNativeDebug
./gradlew :korge-sandbox:runNativeRelease
./gradlew :korge-sandbox:runAndroidRelease
./gradlew :korge-sandbox:runIosDeviceRelease
```

## KorGE Samples

* <https://github.com/korlibs/korge-samples>
* <https://github.com/korlibs/korge-samples-ext>

## KorGE Modules

Traditionally all the KorGE modules were published to central and their source code was available here,
now they are available via kproject in these repositories:

### UI

* <https://github.com/korlibs/korge-compose>

### Physics

* <https://github.com/korlibs/korge-box2d>

### ECS

* <https://github.com/korlibs/korge-fleks>

### Animations

* Basic Tweens & animators (Integrated in KorGE)
* <https://github.com/korlibs/korge-swf>

### Skeletal Libraries

* <https://github.com/korlibs/korge-dragonbones>
* <https://github.com/korlibs/korge-spine>

### TileMaps / Level Loading

* Basic TileSet + TileMap functionality (Integrated in KorGE)
* <https://github.com/korlibs/korge-ldtk>
* <https://github.com/korlibs/korge-tiled>

### Image Formats

* PNG, QOI, ASE, PSD, Native Decoders (Integrated in KorGE)
* <https://github.com/korlibs/korge-image-formats/korim-jpeg> (Pure Kotlin JPEG Encoder/Decoder)
* <https://github.com/korlibs/korge-image-formats/korim-qr> (Pure Kotlin QR Generator)

### AudioFormats

* MP3, WAV (Integrated in KorGE)
* <https://github.com/korlibs/korge-audio-formats> (MOD, XM, S3M, MIDI (WIP), OPUS (WIP))

## Scripting

* <https://github.com/korlibs/korge-luak> (LUA)

## Video

* <https://github.com/korlibs/korge-video>

## I18N

* <https://github.com/korlibs/korge-services/tree/main/korge-i18n> (I18N support)

## Algorithms

* <https://github.com/korlibs/korge-services/tree/main/korma-astar> (AStar (A*) for finding paths in 2D grids)

## 3D

* <https://github.com/korlibs/korge-k3d/> (3D support on top of KorGE)

## Other

* <https://github.com/korlibs/korge-lipsync> (Rhubarb LipSync integration with resource processor)
* <https://github.com/korlibs/korge-parallax> (Pseudo 3D Parallax Effect)
* <https://github.com/korlibs/korge-ext/tree/main/korge-ktree> (Old KTree serialization)
* <https://github.com/korlibs/korge-ext/tree/main/korge-masked-view> (Old Masked View (now we can use `view.mask`))
* <https://github.com/korlibs/korge-ext/tree/main/korge-text2> (Old Text engine)
* <https://github.com/korlibs/korge-ext/tree/main/korge-bus> (Bus / SyncBus)
* <https://github.com/korlibs/korge-ext/tree/main/korge-frameblock> (`frameBlock(60.fps) { while (true) frame() }` utility function)
* <https://github.com/korlibs/korge-ext/tree/main/korge-length> (Support physical-based units: cm, inches, percentages, etc.)
* <https://github.com/korlibs/korge-ext/tree/main/korge-shape-ext> (Support triangulation, spatial pathfinding & shape2d operations)
