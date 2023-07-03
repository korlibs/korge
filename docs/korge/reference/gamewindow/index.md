---
layout: default
title: "Game Window"
title_prefix: KorGE
fa-icon: far fa-window-maximize
priority: 300
---

## DialogInterface

### browse

In order to open a URL with the default user Internet Browser:

```kotlin
views.gameWindow.browse("https://korge.org/")
```

### alert/alertError

To display an alert message or exception with a native dialog:

```kotlin
views.gameWindow.alert("My message")
views.gameWindow.alertError(Exception("My error message"))
```

![img.jpg](img.jpg)

![img_4.jpg](img_4.jpg)

### confirm

To ask the user to confirm an action with a native dialog:

```kotlin
val result: Boolean = views.gameWindow.confirm("My message")
```

![img_1.jpg](img_1.jpg)

### prompt

To ask the user to input a string:

```kotlin
val result: String = views.gameWindow.prompt("My title", "default")
```

![img_2.jpg](img_2.jpg)

### close

This will close the window.

### openFileDialog

To ask the user to select one or more files, for writing or reading.

```kotlin
val selectedFiles: List<VfsFile> = views.gameWindow.openFileDialog(FileFilter("Images" to listOf("*.jpg", "*.jpg")), write = false, multi = true, currentDir = null)
```

![img_3.jpg](img_3.jpg)
