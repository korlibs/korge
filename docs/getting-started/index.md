---
permalink: /getting-started/
group: getting-started
layout: default
title: Welcome to KorGE
title_short: Introduction
description: KorGE Game Engine is a Kotlin Open Source modern Game Engine created in Kotlin designed to be extremely portable and really enjoyable to use.
children: /korge/
priority: -1
fa-icon: fa-gamepad
---

<img alt="Korge" src="/i/logos/korge.svg" width="180" height="180" style="float:left;margin-right:16px;"/>

**KorGE Game Engine** is an [<img alt="Kotlin" src="/i/logos/korge.svg" style="width:1.4em;height:1.4em;margin-top:-0.2em;" />Open Source](https://github.com/korlibs/korge){:target="_blank"} modern Game Engine created in [<img alt="Kotlin" src="/i/logos/kotlin.svg" style="width:1.4em;height:1.4em;margin-top:-0.2em;" />Kotlin](https://kotlinlang.org/){:target="_blank"} designed to be extremely portable and really enjoyable to use.
It works on 
<span title="JVM & K/N: Windows, MacOS Linux">**Desktop**</span>,
<span title="JS & WASM (WIP)">**Web**</span> and 
<span title="Android & iOS">**Mobile**</span>. 
It is fully asynchronous, so it is also nice for the web.

It includes libraries for game development and other areas,
an [asset & library store](https://store.korge.org/),
starter kits,
and an [IntellIJ plugin](https://plugins.jetbrains.com/plugin/9676-korge) to improve the workflow.

You can see a small presentation of KorGE here: <https://korge.org/>{:target="_blank"}

{% include stars.html project="korge" central="com.soywiz.korlibs.korge.plugins/korge-gradle-plugin" %}

<div style="clear:both;"></div>

## Next Steps

Now that you have a working environment, let's explore other concepts.

1. [Install the environment and run the Hello World](/getting-started/install/)
2. Then build a more complex and realistic game with a [step-by-step tutorial to build a chess game](/tutorials/chess/).
3. Finally, [deploy it in one of our supported platforms](/targets/).

You can also explore the documentation or search for specific topics.

## Asking Questions and Solving Doubts

You can ask questions in:

* [Discord Community](https://discord.korge.org/)
* [Github Discussions](https://github.com/korlibs/korge/discussions)

We have a nice and welcoming community! Feel free to drop in.





## Tools:

KorGE uses intelliJ as IDE + Gradle for building. You can check how to [set-up the environment here](/getting-started/install/).

<img alt="Gradle" src="/i/logos/gradle.svg" style="width:128px;height:128px;" />
<img alt="IntelliJ" src="/i/logos/intellij.svg" style="width:128px;height:128px;" />


## Targets:

* You can target JVM Desktop and Android using Kotlin/JVM.
* With JavaScript you can generate Web and PWA applications.
* With Kotlin/Native you can generate native Windows, Linux, macOS executables, as well as native iOS applications.
