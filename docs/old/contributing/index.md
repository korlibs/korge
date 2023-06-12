---
layout: default
title: Contributing
css-icon: fab fa-github
priority: 1000
#status: new
---

{% include toc_include.md %}

## Understanding versions and the `.m2` folder

Korlibs use Gradle and Kotlin-Multiplatform.
All the libraries check and use the Maven Local `.m2` folder first, then check Maven Central.
All the projects have a `gradle.properties` file describing the versions.

In order to try changes locally you have to compile the libraries via gradle and publish them
into your `.m2` folder.
You can either change your library or project to use the `-SNAPSHOT` version,
or change the `version` property of the project your are compiling to match the required one.
Since the other libraries and projects will use maven local, it will get a modified release
version from the Maven Local repository.

### Example

Suppose we have these `gradle.properties` files:

### `korim/gradle.properties`

```properties
# sytleguide
kotlin.code.style=official

# version
group=com.soywiz.korlibs.korim
version=1.9.7-SNAPSHOT

# kotlinx
korioVersion=1.9.8
kormaVersion=1.9.1
npmCanvasVersion=2.6.0
```

### `korio/gradle.properties`

```properties
# sytleguide
kotlin.code.style=official

# version
group=com.soywiz.korlibs.korio
version=1.9.9-SNAPSHOT

# kotlinx
coroutinesVersion=1.3.3
```

Now consider you want to make a change as part of korio that affects korim
and want to try it locally.

You can either:

* Change the `korim/gradle.properties` with `korioVersion=1.9.9-SNAPSHOT`.
* Change the `korio/gradle.properties` with `version=1.9.8`

In the end both work. I usually go for the second option and publish a patched
non-snapshot version in my `.m2` folder.

## Building and publishing into the Maven Local `.m2` folder

### Publish all the libraries into maven local

This will compile all the targets and publish it to the `.m2` folder.
Since it compiles Kotlin/Native too, it will take some time.

```bash
./gradlew publishToMavenLocal
```

### Publish the JVM and JS artifacts into maven local

For simple portable Kotlin/Common stuff, you can compile only JVM and maybe JS too
and try in your own projects without compiling the Kotlin/Native targets.

In order to do so, you can execute the following command:

```bash
./gradlew publishJvmPublicationToMavenLocal publishJsPublicationToMavenLocal publishMetadataPublicationToMavenLocal publishKotlinMultiplatformPublicationToMavenLocal
```

* `publishJvmPublicationToMavenLocal` - The JVM artifact
* `publishJsPublicationToMavenLocal` - The JS artifact
* `publishMetadataPublicationToMavenLocal` - The Kotlin/Common definitions artifact
* `publishKotlinMultiplatformPublicationToMavenLocal` - The project without `-suffix` that points to per-platform artifacts

## Using changes in Korge games

KorGE is split in three repositories:

* <https://github.com/korlibs/korge> - The library repository
* <https://github.com/korlibs/korge-plugins> - The Gradle Plugin
* <https://github.com/korlibs/korge-intellij-plugin> - The IntelliJ Plugin

KorGE games use versions in the [gradle plugin repository](https://github.com/korlibs/korge-plugins/blob/master/gradle.properties).
In fact, when making a patch in libraries other than korge itself, just a version bump + publishing there is enough.

You can publish the `korge-plugins` gradle plugin by calling:

```bash
./gradlew publishToMavenLocal
```

Alternatively you can check the dependency versions your project is using
and change the `specific-korlib/gradle.properties` you are building with a `version`
matching your project.

Alternatively the `korge-gradle-plugin` checks your project `gradle.properties`
searching for `korimVersion`, `korgeVersion` and so on. So you can force a specific korlib
dependency version in your own project including a -SNAPSHOT version.

## Unit testing using kotlin-test

To test just the JVM tests, you can call:

```bash
./gradlew jvmTest
```

**NOTE:** On IntelliJ as for this writing, there is a bug with the play icon gutter
in multi-module multiplatform kotlin projects that doesn't put the right submodule
to the generated configuration. 
You can solve it [as described here](https://youtrack.jetbrains.com/issue/KT-35771)
so you can easily debug and put breakpoints when running your tests.
