---
layout: default
title: "Desktop (Native)"
title_prefix: KorGE Targets
fa-icon: fa-laptop
priority: 1
#status: new
---

This should be the preferred target when publishing applications for desktop platforms.

This generates an almost dependency-less executable for each of the three supported platforms:
**Linux**, **MacOS** and **Windows**.
The linux target requires OpenAL and OpenGL.
While MacOS and Windows targets uses libraries already included in the operating system.

This target features no Virtual Machines, smaller executable, less memory footprint,
fast startup times, low GC pause times, but very slow compilation times,
and at this point a not as great debugging experience comparing to the JVM target.

{% include toc_include.md %}

## Executing

For running, use the gradle task:

```bash
./gradlew runNativeDebug
./gradlew runNativeRelease
```

## Testing

For testing, use the gradle tasks:

```bash
./gradlew linuxX64Test
./gradlew macosX64Test
./gradlew mingwX64Test
```

## Building

To generate debug builds without running, use the gradle task:

```bash
./gradlew linkDebugExecutableLinuxX64
./gradlew linkDebugExecutableMacosX64
./gradlew linkDebugExecutableMingwX64
```

To generate release builds without running:

```bash
./gradlew linkReleaseExecutableLinuxX64
./gradlew linkReleaseExecutableMacosX64
./gradlew linkReleaseExecutableMingwX64
```

## Creating MacOS `.app` bundles

Outputs to `/build/unnamed-debug.app`:

```bash
./gradlew packageMacosX64AppDebug
```

Outputs to `/build/unnamed-release.app`:

```bash
./gradlew packageMacosX64AppRelease
```


## Installing Linux Dependencies

### Ubuntu and Debian-based distros

#### Developing

```bash
sudo apt-get -y install freeglut3-dev libopenal-dev
```

#### Running

Probably already included on most desktop distributions:

```bash
sudo apt-get -y install freeglut3 libopenal1
```

