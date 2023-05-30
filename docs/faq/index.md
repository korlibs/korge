---
layout: default
title: FAQ
fa-icon: fa-question-circle
priority: 2000
---

{% include toc_include.md %}

## Are these libraries free?
{:#free}

Yes. All these libraries are dual licensed under MIT and Apache 2.0 or CC0 Public Domain, except some libraries that I have ported from other languages and were not licensed as MIT, that propagate their own license. But in any case, all the licenses used are free and permissive.

## Where can I find the libraries?
{:#repos}

* You can find the source code of my libraries on <https://github.com/korlibs> and <https://github.com/soywiz>.
* I publish all my library binaries at maven central too: <https://search.maven.org/search?q=g:com.soywiz.korlibs.*>.
* The libraries are also synchronized to maven central automatically.

## I get an error: unable to find library -lGL on Linux

Since linux doesn't include graphic or audio libraries by default,
you might get this error if you try to compile a korge application
with `./gradlew runNativeDebug`.

```
> Task :linkDebugExecutableLinuxX64 FAILED
e: /home/parallels/.konan/dependencies/clang-llvm-8.0.0-linux-x86-64/bin/ld.lld invocation reported errors

The /home/parallels/.konan/dependencies/clang-llvm-8.0.0-linux-x86-64/bin/ld.lld command returned non-zero exit code: 1.
output:
ld.lld: error: unable to find library -lGL
ld.lld: error: unable to find library -lGLU
ld.lld: error: unable to find library -lglut
ld.lld: error: unable to find library -lopenal
```

If you are using Ubuntu or other Debian-based distro, you can execute
the following command to install the required libraries:

```bash
sudo apt-get -y install freeglut3-dev libopenal-dev libncurses5
```

On Solus, you can use this command:

```bash
sudo eopkg install glfw-devel freeglut openal-soft-devel ncurses libglu-devel
sudo eopkg install -c system.devel
```

On Arch, you can use this command to install [lglut](https://archlinux.org/packages/extra/x86_64/freeglut/) and [lopenal](https://archlinux.org/packages/extra/x86_64/openal/). For the rest install [ncurses5-compat-libs](https://aur.archlinux.org/packages/ncurses5-compat-libs) from the AUR repo.
```bash
pacman -S freeglut openal
```

## How do I include these libraries in my multiplatform projects?
{:#include-multi}

The libraries are multiplatform Kotlin projects that uses GRADLE_METADATA to detect supported platforms.
They require Gradle 5.5.1 or greater. 

### `settings.gradle`
```groovy
enableFeaturePreview('GRADLE_METADATA')
```

### `build.gradle`
```groovy
dependencies {
    implementation("com.soywiz.korlibs.klock:klock:1.6.1")
}
```

## How do I include these libraries in my pure-java projects?
{:#include-java}

### `build.gradle`
```groovy
dependencies {
    // ...
    implementation("com.soywiz.korlibs.klock:klock-jvm:1.6.1")
}
```

You might need to [disambiguate](https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#disambiguating-targets) in some cases:
```
implementation("com.soywiz.korlibs.klock:klock-jvm:1.6.1") {
    attributes {
        attribute(org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.attribute, org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm)
    }
}
```

## Artifacts can't be resolved
{:#unresolved}

### Check that you are using at least Gradle 5.5.1
{:#unresolved-gradle}

```bash
./gradlew --version
```

You can update it with:

```bash
./gradlew wrapper --gradle-version=5.5.1
```

## How do I get the current's device resolution on KorGE?
{:#korge-device-resolution}

KorGE doesn't provide a direct way of getting the device resolution.
This is intended to simplify your code. Instead you use a Virtual Resolution,
and an Extended Virtual Resolution when your virtual Aspect Ratio doesn't match
the one's from the device. Similar to OpenGL and some engines default (-1,+1)
screen coordinates, but with the dimensions defined by you.

## Links

### Slack

* <https://slack.soywiz.com/>

### GitHub

* <https://github.com/korlibs/>

### GitHub Sponsors / Donations
{:#github-sponsors}

* <https://github.com/sponsors/soywiz/>

### OpenCollective

* <https://opencollective.com/korge>

## How are issues prioritized?

In normal circumstances, bugs and sponsored tickets coming from sponsors are done first.

There is a Kanban board at GitHub where you can see the progress and the current prioritizations:
<https://github.com/orgs/korlibs/projects/19>

## How can I close a KorGE Game Window programatically?
{:#close_window}

```korge
views.gameWindow.close()
```

## e: Unable to compile C bridges

```bash
sudo apt install libncurses5
```

## e: This declaration is experimental and its usage must be marked with '@kotlin.time.ExperimentalTime' 

```
/build/platforms/native-desktop/bootstrap.kt: (5, 98): This declaration is experimental and its usage must be marked with '@kotlin.time.ExperimentalTime' or '@OptIn(kotlin.time.ExperimentalTime::class)'
```

If you main entry point of Korge is using `@ExperimentalTime` or other experimental APIs
you have to replace it with `@OptIn(ExperimentalClass::class)` eg. `@OptIn(ExperimentalTime::class)`.

## How is KorGE pronounced
{:#korge-pronunciation}

Since this was not specified from the very beginning, we used up to three different pronunciations for it.

The original pronounciation is like saying Jorge in Spanish but starting with K.

Can you listen it here:

<audio controls="controls">
 <source src="/i/sound/korge.mp3" />
</audio>

## Where's the privacy page

[Here it is](/privacy)