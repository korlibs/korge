<h1 align="center">KorGE</h1>
<p align="center">Multiplatform Kotlin Game Engine</p>
<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/TEST/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/com.soywiz.korlibs.korge.plugins/korge-gradle-plugin"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/com.soywiz.korlibs.korge.plugins/korge-gradle-plugin"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord&label=Discord" /></a>
</p>
<!-- /BADGES -->

<!--
Accepting Donations/Sponsorship via Bitcoin: `bc1qfmnd2jazh6czsuvvvy5rc3fxwsfvj6e8zwesdg`
-->

<!-- SUPPORT -->
<h2 align="center">Support korge</h2>
<p align="center">
If you like korge, or want your company logo here, please consider <a href="https://github.com/sponsors/soywiz">becoming a GitHub sponsor ★</a>,<br />
</p>
<!-- /SUPPORT -->

## Info about the project:

KorGE is a modern multiplatform game engine for Kotlin. Features include:

* Hot Reloading
  * KorGE supports HotReloading to see changes immediately without having to restart the application
* KProject support
  * Share & re-use source code and resources via GitHub
* Debugger
  * Live-debug your games
* 100% Kotlin
  * KorGE is fully written in Kotlin and designed from the ground up to embrace modern and easy coding styles
* Multiplatform
  * KorGE gradle plugin allows to target the following platforms: JVM for Android, JS & WASM for the Web, native code for iOS, and JVM/JS for Desktop
* Quick installation
  * Install [KorGE Forge](https://forge.korge.org/) or clone the “Hello World!” project and start making your own game in less than a minute
* Fully productive
  * Since KorGE targets the JVM, you can develop your game, try it, debug it and test it using IntelliJ IDEA
* Tons of features
  * KorGE is just the last layer of a larger stack (Korlibs) for multimedia development
* Small footprint
  * KorGE has a very small footprint. It has no external dependencies and only uses the libraries available on each platform

For more information, visit:

* <https://korge.org/>
* <https://docs.korge.org/>
* <https://discord.korge.org/>

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
./gradlew :korge-sandbox:runAndroidRelease
./gradlew :korge-sandbox:runIosDeviceRelease
```

## KorGE Store

Traditionally all the KorGE modules were published to central and their source code was available here,
now they are available via kproject in separate repositories.
You can find a catalog of all the published extensions here:

<https://store.korge.org/>
