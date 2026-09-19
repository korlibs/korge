<h1 align="center">KorGE</h1>
<p align="center">Multiplatform Kotlin Game Engine</p>
<!-- BADGES -->
<p align="center">
	<a href="https://github.com/korlibs/korge/actions"><img alt="Build Status" src="https://github.com/korlibs/korge/workflows/TEST/badge.svg" /></a>
    <a href="https://search.maven.org/artifact/org.korge.gradleplugins/korge-gradle-plugin"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/org.korge.gradleplugins/korge-gradle-plugin"></a>
	<a href="https://discord.korge.org/"><img alt="Discord" src="https://img.shields.io/discord/728582275884908604?logo=discord&label=Discord" /></a>
</p>
<!-- /BADGES -->

<!--
Accepting Donations/Sponsorship via Bitcoin: `bc1qfmnd2jazh6czsuvvvy5rc3fxwsfvj6e8zwesdg`
-->

<!-- SUPPORT -->
<!--
<h2 align="center">Support korge</h2>
<p align="center">
If you like korge, or want your company logo here, please consider <a href="https://github.com/sponsors/soywiz">becoming a GitHub sponsor ★</a>,<br />
</p>
-->
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

## News:
### 08-Sep-2026:
  Korge was updated to Gradle 9 and Android Gradle Plugin 9. This brings changes in the application
  setup. For a starting point take a look at the `korge-sandbox` module in this repo or check the
  `korlibs/korge-hello-world` repo on GitHub.

### 02-May-2026:
  We are moving Korge to a new namespace on maven central, from `com.soywiz.korge` to `org.korge`.
  This a breaking change. You will need to change your dependencies to use the new namespace in the configuration of
  your project. Change your `gradle/libs.versions.toml` to:

```toml
[plugins]
#korge = { id = "com.soywiz.korge", version = "6.0.0" }   <-- Old namespace, latest official version
korge = { id = "org.korge.engine", version = "7.0.0-SNAPSHOT" }   # <-- New namespace, use latest snapshot version
```

## Usage:

- The latest official release on Maven Central is `6.0.0`
- The current snapshot release on Maven Central is `7.0.0-SNAPSHOT`

### Publish to Maven Local

For checking out local changes in Korge and test them in your own applications you can publish
Korge to maven local:

```shell script
./gradlew publishToMavenLocal
```

If you want to make changes and easily try things out without releasing locally, you can check
the `korge-sandbox` module that runs `shared/src/commonMain/kotlin/org/korge/application/Main.kt`
file. Make your experiments there:

```shell script
./gradlew :korge-sandbox:runJvm
./gradlew :korge-sandbox:runJs
./gradlew :korge-sandbox:runAndroidRelease
./gradlew :korge-sandbox:runIosDeviceRelease
```

## KorGE Store

[Deprecated] Korge Store will be discontinued in Korge release 7. Source code additions to Korge can
be added as Gradle submodule in the context of Kotlin Multiplatform.

Traditionally all the KorGE modules were published to central and their source code was available here,
now they are available via kproject in separate repositories.
You can find a catalog of all the published extensions here:

<https://store.korge.org/>
