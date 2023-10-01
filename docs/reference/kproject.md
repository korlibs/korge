---
permalink: /kproject/
group: reference
layout: default
title: "KProject"
fa-icon: fa-file-archive
priority: 70
---


{% include stars.html project="kproject" central="com.soywiz.kproject/com.soywiz.kproject.gradle.plugin" %}



[KProject](https://github.com/korlibs/kproject) is a mechanism, and a set of gradle plugins to share kotlin
libraries and modules by source code instead of precompiled libraries for each platform by using a YAML-based
configuration file.
It allows transitive dependencies, dependency resolving to the latest version, etc.

KProject was designed as a mechanism to easily evolve KorGE's ecosystem.
By being able to rapidly release and update libraries, modules and assets with code.
You can access modules designed for the Korlibs ecosystem in <https://store.korge.org/>

## Usage

In order to be able to use KProject, you just have to:
install a `settings.gradle.kts` plugin, create a `deps.kproject.yml`, make your projects depend on that `deps.kproject.yml`.

### `settings.gradle.kts`

```kotlin
// kproject is published to central, also mavenLocal allows for using it while developing
pluginManagement { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }

plugins {
    //id("com.soywiz.kproject.settings") version "0.0.1-SNAPSHOT" // While developing
    id("com.soywiz.kproject.settings") version "0.3.1" // Substitute by the latest version
}

// Here we ensure deps.kproject.yml file is loaded and gradle modules are created
kproject("./deps")
```

### `deps.kproject.yml`

```yaml
dependencies:
- https://github.com/korlibs/korge-luak/tree/v0.1.1/luak##cae4e0c473b5e80820819ccf24ed4e4c4891f307
```

### `build.gradle.kts`

```kotlin
dependencies {
    add("commonMainApi", project(":deps"))
}
```

## `kproject.yml` reference

### The `dependencies` and `testDependencies` section

`kproject.yml` files have a `dependencies` section that is an array of dependencies. For example:

```yaml
dependencies:
# folder-based dependencies
- ./path/to/my/project
# GIT repository dependencies
- https://github.com/korlibs/korge-luak/tree/v0.1.1/luak##cae4e0c473b5e80820819ccf24ed4e4c4891f307
# maven dependencies
- org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4
testDependencies:
- maven::jvm::org.mockito:mockito-core:5.3.1
- io.mockk:mockk-android:1.13.5::jvm
```

### The `plugins` section

Right now, kproject supports the `kotlinx-serialization` plugin:

```yaml
plugins:
- serialization
```

### Specifying targets

By default, kprojects automatically generate kotlin multiplatform gradle projects targeting all the kotlin targets. 
It is possible to specify specific targets like `jvm`, `js`, `android`, `desktop`, `mobile` or `wasm` to be the only
ones to be created when using kproject. For example in the case we are working on a JVM-only project outside korge.

```yaml
targets: [jvm, js]
```

### Specify JVM versions

You can specify JVM and Android JVM versions by setting the following properties
either in `deps.kproject.yml` or in the `gradle.properties` file on your root project.

```prope
kproject.jvm.version=1.8 
kproject.android.jvm.version=11 
```

### Forcing specific maven artifact versions

In the case we depend on libraries that depend on different library versions, we can force
to use a specific version with the `versions` section:

```yaml
versions:
- "com.soywiz.korlibs.korge2:korge": "4.0.6"
```

## Presentation video

<iframe width="560" height="315" src="https://www.youtube.com/embed/avKhNcVJB5I?start=404" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
