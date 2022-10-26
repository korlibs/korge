<h1 align="center">KorGE</h1>
<p align="center">Multiplatform Kotlin Game Engine</p>

<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/CI/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/com.soywiz.korlibs.korge.plugins/korge-gradle-plugin"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/com.soywiz.korlibs.korge.plugins/korge-gradle-plugin"></a>
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

* <https://korge.org/>
* <https://docs.korge.org/>
* <https://forum.korge.org/>

## Usage:

KorGE and all the other korlibs in a single monorepo.

To use this version in other projects,
you have to publish it locally to mavenLocal,
and then use `2.0.0.999` as version: 

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

## About KorGE Modules

Traditionally all the KorGE modules were published to central and their source code was available here,
now they are available via kproject in these repositories:

### UI

* <https://github.com/korlibs/korge-compose>

### Physics

* <https://github.com/korlibs/korge-box2d>

### ECS

* <https://github.com/korlibs/korge-fleks>

### Animations

* <https://github.com/korlibs/korge-swf>

### Skeletal Libraries

* <https://github.com/korlibs/korge-dragonbones>
* <https://github.com/korlibs/korge-spine>

### Level Loading

* <https://github.com/korlibs/korge-ldtk>
