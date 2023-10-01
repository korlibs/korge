---
permalink: /targets/desktop/
group: targets
layout: default
title: "Desktop (JVM)"
title_short: Desktop
fa-icon: fa-laptop
priority: 10
#status: new
---

It supports **Java 8** and greater. This should be the preferred target while developing.

Uses the Kotlin JVM backend, generating JVM bytecode and executing the code
on the Java Virtual Machine.

This integrates pretty well on the IDEs, have fast building and startup times,
and provides a great tools like a better debugging experience. All these features
makes it the best target for debugging and trying things fast.



## Entrypoint

While other targets use `korge { entrypoint = "main" }`, the JVM target
uses its own `jvmMainClassName` property indicating the class
that will contain the main entry point.
It defaults to `MainKt` referencing the package-less root `main.kt` file:

<!-- @TODO: https://github.com/korlibs/korge-plugins/issues/9 -->

```kotlin
korge {
	jvmMainClassName = "MainKt"
}
```

### Multiple entry points

In some cases, like editors and so, we might want to have multiple entry points to share a code base with a single module
but being able to execute different scenarios, without mixing code. To do so, we can define multiple entry points for the JVM
target, that will create one gradle task for each entry point.

```kotlin
korge {
    entrypoint("Editor", "com.my.project.MyEditor") // ./gradlew runJvmEditor
	entrypoint("Debug", "com.my.project.Debug") // ./gradlew runJvmDebug
}
```

Extra mains:

```kotlin
package com.my.project

object Debug {
    @JvmStatic fun main(args: Array<String>) = runBlocking { Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"]) {
		// ...
    } }
}

object MyEditor {
	@JvmStatic fun main(args: Array<String>) = runBlocking { Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"]) {
			// ...
    } }
}
```

Gradlew tasks:

```bash
./gradlew runJvmEditor
./gradlew runJvmDebug
```
## Executing

For running, use the gradle task:

```bash
./gradlew runJvm
```

## Testing

For executing tests on the JVM only, use the gradle task:

```bash
./gradlew jvmTest
```

## Packaging

For creating a FatJAR, use the gradle task:

```bash
./gradlew packageJvmFatJar
```

For creating a FatJAR using proguard for thinner sizes, use the gradle task:

```bash
./gradlew packageJvmFatJarProguard
```

You can find the jar files in the `build/libs` folder.
Full jar files should have the `-all.jar` suffix,
while the proguarded ones, `-all-proguard.jar`.

## Reducing pause times

Starting with Java 14, all major targets support ZGC, that should
make GC pauses to be less than 10ms.

<https://wiki.openjdk.java.net/display/zgc/Main>

You can enable it with `-XX:+UnlockExperimentalVMOptions -XX:+UseZGC`.

<!-- TODO -->

Future KorGE versions will enable this by default on supported Java versions.
