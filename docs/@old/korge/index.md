---
layout: default
title: KorGE Game Engine
children: /korge/
priority: -1
fa-icon: fa-gamepad
---

<img src="/i/logos/korge.svg" width="180" height="180" style="float:left;margin-right:16px;"/>

**KorGE Game Engine** is an [Open Source](https://github.com/soywiz/korge) modern Game Engine created in [<img src="/i/logos/kotlin.svg" style="width:1.4em;height:1.4em;margin-top:-0.2em;" />Kotlin programming language](https://kotlinlang.org/) designed to be extremely portable and really enjoyable to use.

It works for **Desktop**, **Web** and **Mobile**. But allows to create other targets easily. And it is full asynchronous so it is nice to web.

<div style="clear:both;"></div>

## Tools:

KorGE uses intelliJ as IDE + Gradle for building. You can check how to [set-up the environment here](/setup).

<img src="/i/logos/gradle.svg" style="width:128px;height:128px;" />
<img src="/i/logos/intellij.svg" style="width:128px;height:128px;" />

## Dependencies:

KorGE uses [Kotlin](https://kotlinlang.org/) several other [soywiz's Korlibs libraries](https://github.com/soywiz/korlibs) and optionally [JTransc](https://github.com/jtransc/jtransc) to work:

<img src="/i/logos/kotlin.svg" width="128" height="128" style="padding:4px;" />
<img src="/i/logos/korlibs.svg" width="128" height="128" style="padding:4px;" />
<img src="/i/logos/korio.svg" width="128" height="128" style="padding:4px;" />
<img src="/i/logos/korma.svg" width="128" height="128" style="padding:4px;" />
<img src="/i/logos/korag.svg" width="128" height="128" style="padding:4px;" />
<img src="/i/logos/korim.svg" width="128" height="128" style="padding:4px;" />
<img src="/i/logos/korau.svg" width="128" height="128" style="padding:4px;" />
<img src="/i/logos/korui.svg" width="128" height="128" style="padding:4px;" />
<img src="/i/logos/klock.svg" width="128" height="128" style="padding:4px;" />
<img src="/i/logos/jtransc.svg" width="128" height="128" style="padding:4px;" />

## Targets:

* Right now with Korge you can target JVM, Android and JavaScript.

### Kotlin.JS and Kotlin.Native

At this point KorGE uses JTransc for generating other targets than normal Java/Kotlin targets.
When Kotlin supports reflection in JavaScript and Native, KorGE will target that instead of using JTransc. While keeping JTransc for other targets like C#/AS3 or when mixing Java libraries with Kotlin code.

### Future:

* It is planned a target for AS3 Adobe AIR (iOS and Android).
* A C# target for UWP applications.
* And native C++ targets for iOS, Android and other platforms.

Soywiz's korlibs are very portable so they can adapt to every major platform.

## Where to start:

You can start reading about the [setup](/setup), [the basics](/basics), [reading tutorials](/tutorials) or [watching video tutorials](/tutorials).

## Coming from other engines?

<a href="/migration/as3"><img src="/migration/as3/air_to_korge.png" width="30%" height="30%" /></a>
<a href="/migration/unity"><img src="/migration/unity/unity_to_korge.png" width="30%" height="30%" /></a>
