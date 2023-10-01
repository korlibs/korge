---
layout: default
title: "Setup"
fa-icon: fa-check-square-o
priority: 0
---

<img src="/i/logos/intellij.svg" width="160" height="160" />
<img src="/i/logos/gradle.svg" width="160" height="160" />

KorGE game engine uses [intelliJ IDEA Community or Ultimate](https://www.jetbrains.com/idea/download/) as IDE and [Gradle](https://gradle.org/) as building tool. Korge provides [plugins for both](/plugin) in order to generate all the required preprocessed resources.

### Setup intelliJ

> You have to visit [https://www.jetbrains.com/idea/download/](https://www.jetbrains.com/idea/download/), and download an intelliJ IDEA IDE, either Community or Ultimate will work. Community is free, but Ultimate has tons of additional features, and allows you to use it as a really polyglot IDE.

![](/korge/setup/download.avif)

> After installing and launching, you have to go to `Configure -> Plugins`

![](/korge/setup/plugins.avif)

> Then click the `Browse repositories...` button.

![](/korge/setup/browse_repositories.avif)

> Then search for `Korge` and click the `Install` green button. That will suggest you to restart the IDE. Do so.

![](/korge/setup/korge_plugin.avif)

> After that, you should see the Korge menu when opening a project.
> Depending on the version you will see `Korge` it in the main menu or in the `Tools -> Korge` menu.

![](/korge/setup/korge_plugin_menu.avif)

> Korge will work on projects that include `korge-core` artifact.

### Supported Artifacts

* `com.soywiz:korge:$korVersion`
* `com.soywiz:korge-ext-swf:$korVersion`
* `com.soywiz:korge-ext-particle:$korVersion`
* `com.soywiz:korge-ext-spriter:$korVersion`
* `com.soywiz:korge-ext-tiled:$korVersion`
* `com.soywiz:korge-ext-lipsync:$korVersion`
