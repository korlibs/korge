---
layout: default
title: "Game Window"
title_prefix: KorGE
fa-icon: far fa-window-maximize
priority: 300
---

## DialogInterface

### `browse` to open an URL in the default internet browser

In order to open a URL with the default user Internet Browser:

```kotlin
views.gameWindow.browse("https://korge.org/")
```

### `alert`/`alertError` to display a message

To display an alert message or exception with a native dialog:

```kotlin
views.gameWindow.alert("My message")
views.gameWindow.alertError(Exception("My error message"))
```

![img.jpg](img.jpg)

![img_4.jpg](img_4.jpg)

### `confirm` to ask for a YES or NO

To ask the user to confirm an action with a native dialog:

```kotlin
val result: Boolean = views.gameWindow.confirm("My message")
```

![img_1.jpg](img_1.jpg)

### `prompt` to ask for a text

You can use `prompt` to ask the user to type/prompt/input a string with a native dialog:

```kotlin
val result: String = views.gameWindow.prompt("My title", "default")
```

![img_2.jpg](img_2.jpg)

### `close` to close the window

This will close the window.

### `openFileDialog` to open a native file dialog

To ask the user to select to open one or more files, both for writing and/or reading with the native file dialog.

```kotlin
val selectedFiles: List<VfsFile> = views.gameWindow.openFileDialog(FileFilter("Images" to listOf("*.jpg", "*.jpg")), write = false, multi = true, currentDir = null)
```

![img_3.jpg](img_3.jpg)
