---
permalink: /views/ui/
group: views
layout: default
title: "UI"
title_prefix: KorGE
fa-icon: fas fa-toggle-on
priority: 6000
---

KorGE include some UI components, you can use out of the box.



## `UIButton`

UIButton is a basic button, that supports both text and icons.
It automatically includes ellipsis for long texts.

```kotlin
uiButton("Hello World, this is a button!")
```

![UIButton.png](/i/ui/UIButton.png)

```kotlin
uiButton(icon = resourcesVfs["korge.png"].readBitmapSlice())
```

![img.png](/i/ui/img.png)

It is possible to adjust some of the visual properties of the UIButton:

```kotlin
uiButton("Hello World, this is a text!").also {
    it.width = 190f
    it.bgColorOut = MaterialColors.AMBER_500
    it.bgColorOver = MaterialColors.AMBER_800
    it.textColor = MaterialColors.BLUE_900
    it.background.radius = RectCorners(16f, 0f, 12f, 4f)
}
```

![img_2.png](/i/ui/img_2.png)

## `UICheckBox`

For desktop interfaces, where you need a toggleable setting, you can use `UICheckBox`. 

```kotlin
uiCheckBox(text = "My CheckBox", checked = true)
```

![img_3.png](/i/ui/img_3.png)

## `UIRadioButton` / `UIRadioButtonGroup`

For desktop interfaces where you need a set of options where all are displayed but only one is selected,
you can use `UIRadioButton`.

```kotlin
val group = UIRadioButtonGroup()
uiVerticalStack {
    uiRadioButton(text = "Option1", group = group)
    uiRadioButton(text = "Option2", group = group)
}
```

![img_4.png](/i/ui/img_4.png)

## `UIComboBox`

A ComboBox / DropDown / Select that drops a list of elements the user can select.

```kotlin
uiComboBox(size = Size(160f, 32f), items = listOf("Hello", "World", "Options", "among", "a", "long", "list", "of", "options", "where", "scrolling", "appears"), selectedIndex = 2)
```

![img_5.png](/i/ui/img_5.png)

## `TextBlock`

This is a text supporting rich text data, ellipsis, text wrapping, etc.

```kotlin
textBlock(
    RichTextData.fromHTML(
        "hello <b>world</b>, <font color=red>this</font> is a long text that won't fit!",
        RichTextData.Style.DEFAULT.copy(font = DefaultTtfFontAsBitmap)
    ),
    size = Size(100f, 48f)
)
```

![img_11.png](/i/ui/img_11.png)

## `UIText`

This is a UIView wrapping a plain KorGE `Text`. For word wrapping, etc. using `TextBlock` instead.

```kotlin
uiText("Hello World!")
```

![img_12.png](/i/ui/img_12.png)

## `UITextInput`

```kotlin
uiTextInput("Text Input")
```

![img_13.png](/i/ui/img_13.png)

## `UIImage`

`UIImage` allows to render an image using a ScaleMode and a ContentAnchor to fit/cover within the view bounds.

```kotlin
solidRect(Size(120f, 32f), Colors.PURPLE)
uiImage(Size(120f, 32f), KR.korge.read().slice(), scaleMode = ScaleMode.FIT, contentAnchor = Anchor.CENTER)
```

![img_14.png](/i/ui/img_14.png)

## `UIWindow`

A draggable, resizable and closeable window.

![img_24.png](/i/ui/img_24.png)

## `UIProgressBar`

```kotlin
uiProgressBar(size = Size(256, 8), current = 75f, maximum = 100f)
```

![img_17.png](/i/ui/img_17.png)

## `UITreeView`

A bit rusty Tree View to render collapsable trees.

```kotlin
uiTreeView(UITreeViewList(listOf(
    UITreeViewNode("hello", listOf(
        UITreeViewNode("world")
    ))
)), size = Size(100, 100))
```

![img_18.png](/i/ui/img_18.png)

## `UIMaterialLayer`

A Material Layer is a rounded rectangle that can has a border, a drop shadow, rounded corners and a background color.

```kotlin
uiMaterialLayer().also {
    it.size = Size(120f, 60f)
    it.radius = RectCorners(16f, 8f)
    it.shadowColor = Colors.BLUE
    it.shadowRadius = 10f
    it.shadowOffset = Vector2.polar(90.degrees, 10f, Vector2.UP_SCREEN)
    it.bgColor = Colors.GREEN
    it.borderColor = Colors.PURPLE
    it.borderSize = 4f
}
```

![img_16.png](/i/ui/img_16.png)

## `UIBreadCrumb`

```kotlin
uiBreadCrumb(listOf("hello", "world", "this", "is", "a", "path"))
```

![img_6.png](/i/ui/img_6.png)

## `UIEditableNumber`

This allows the user to edit a number, while allowing to drag & drop or click to adjust it.

```kotlin
uiEditableNumber(10.0, min = 0.0, max = 100.0)
```

![img_7.png](/i/ui/img_7.png)

## `UISlider`

A slider to select a number in a range.

```kotlin
uiSlider(value = 50f, min = 0f, max = 100f)
```

![img_21.png](/i/ui/img_21.png)

## `UIVerticalList`

Lazily computed for elements vertical list. It is aware of container bounds,
so it will only generate and display elements that are visible.

```kotlin
uiScrollable(Size(160, 120)) {
    uiVerticalList(object : UIVerticalList.Provider {
        override val fixedHeight: Float? get() = 16f
        override val numItems: Int get() = 1000
        override fun getItemHeight(index: Int): Float = 16f

        override fun getItemView(index: Int, vlist: UIVerticalList): View {
            return TextBlock(RichTextData.fromHTML("Element $index"))
        }
    }, width = 160f)
}
```

![img_19.png](/i/ui/img_19.png)

## `UISpacer`

```kotlin
uiVerticalStack {
    uiButton("")
    uiButton("")
    uiSpacing(Size(0, 10))
    uiButton("")
}
```

![img_22.png](/i/ui/img_22.png)

## Layouts

### `UIScrollableContainer`

```kotlin
uiScrollable(Size(300, 300)) {
    image(resourcesVfs["korge.png"].readBitmapSlice())
}
```

![img_20.png](/i/ui/img_20.png)

### `UIVerticalStack`

To arrange elements vertically.

```kotlin
uiVerticalStack(padding = 4f) {
    uiButton("vertical")
    uiButton("stack")
    uiButton("with padding")
}
```

![img_8.png](/i/ui/img_8.png)

### `UIHorizontalStack`

To arrange elements horizontally.

```kotlin
uiHorizontalStack(padding = 4f) {
    uiButton("horizontal")
    uiButton("stack")
    uiButton("with padding")
}
```

![img_9.png](/i/ui/img_9.png)

### `UIGridFill`

To arrange elements in a table shape.

```kotlin
uiGridFill(cols = 3, rows = 3) {
    for (n in 0 until 9) uiButton("$n")
}
```

![img_10.png](/i/ui/img_10.png)

## Styling

It is possible to style some components. As long as you attach styles to a UIContainer,
all its descendants will use that style.

```kotlin
uiContainer {
    styles {
        this.textColor = Colors.RED
        this.textSize = 32f
    }
    uiText("Hello World, this is a text!")
}
```

![img_1.png](/i/ui/img_1.png)
