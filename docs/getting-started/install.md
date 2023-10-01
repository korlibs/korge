---
permalink: /getting-started/install/
group: getting-started
layout: default
title: Install KorGE
title_short: Install
children: /korge/
priority: 10
fa-icon: fa-gamepad
---

## 1. Install IntelliJ IDEA

### Install JetBrains ToolBox

JetBrains ToolBox supports installing and keeping up-to-date all the JetBrains products. It is free.
And supports installing the free IDEs available including IntelliJ IDEA Communit Edition. 

* <https://www.jetbrains.com/toolbox-app/>

![](/i/jetbrains-toolbox-1.avif)

![](/i/jetbrains-toolbox-2.avif)

### Manually Install IntelliJ IDEA

* <https://www.jetbrains.com/idea/download/> - Community Edition will work, but you can also use the Ultimate edition.

![](/i/install-intellij-idea.avif)

## 2. Install the KorGE **IntelliJ IDEA plugin**:

### Install Button:

You can click here to install the plugin:

<iframe frameborder="none" width="245px" height="48px" src="https://plugins.jetbrains.com/embeddable/install/9676"></iframe>

### Plugin website:

Or download the plugin from the [KorGE Plugin page](https://plugins.jetbrains.com/plugin/9676-korge).

### Plugin settings:

![](/i/korge-marketplace-plugin.avif)

## 3. Create a Project

### Install one of the starter kits or samples available using the `New Project...` Wizard:

You can select one of the `Starter Kits` or one of the `Showcases` with a full game:

![](/i/korge-new-project.avif)

## 4. Access the KorGE Store

In order to access some KorGE features, you can install them via the KorGE Store.

You can access that store via: <https://store.korge.org/>

Or inside the IntelliJ Plugin navigation bar:

![](/i/jitto-korge-store.avif)

Or when opening your `build.gradle.kts` or your `deps.kproject.yml`:

![](/i/jitto-korge-store2.avif)

## 5. Running your code

When creating a new project a new run configuration `runJvmAutoreload` should be available:

![](/i/runJvmAutoreload.avif)

You can also `double click` on the `Gradle` → `Tasks` → `run` → `runJvmAutoreload`
to create a run configuration and execute your program:

![](/i/gradle-panel-runJvmAutoreload.avif)
