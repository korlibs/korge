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

![UIButton.png](/i/ui/UIButton.avif)

```kotlin
uiButton(icon = resourcesVfs["korge.png"].readBitmapSlice())
```

![img.png](/i/ui/img.avif)

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

![img_2.png](/i/ui/img_2.avif)

## `UICheckBox`

For desktop interfaces, where you need a toggleable setting, you can use `UICheckBox`. 

```kotlin
uiCheckBox(text = "My CheckBox", checked = true)
```

![img_3.png](/i/ui/img_3.avif)

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

![img_4.png](/i/ui/img_4.avif)

## `UIComboBox`

A ComboBox / DropDown / Select that drops a list of elements the user can select.

```kotlin
uiComboBox(size = Size(160f, 32f), items = listOf("Hello", "World", "Options", "among", "a", "long", "list", "of", "options", "where", "scrolling", "appears"), selectedIndex = 2)
```

![img_5.png](/i/ui/img_5.avif)

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

![img_11.png](/i/ui/img_11.avif)

## `UIText`

This is a UIView wrapping a plain KorGE `Text`. For word wrapping, etc. using `TextBlock` instead.

```kotlin
uiText("Hello World!")
```

![img_12.png](/i/ui/img_12.avif)

## `UITextInput`

```kotlin
uiTextInput("Text Input")
```

![img_13.png](/i/ui/img_13.avif)

## `UIImage`

`UIImage` allows to render an image using a ScaleMode and a ContentAnchor to fit/cover within the view bounds.

```kotlin
solidRect(Size(120f, 32f), Colors.PURPLE)
uiImage(Size(120f, 32f), KR.korge.read().slice(), scaleMode = ScaleMode.FIT, contentAnchor = Anchor.CENTER)
```

![img_14.png](/i/ui/img_14.avif)

## `UIWindow`

A draggable, resizable and closeable window.

![img_24.png](/i/ui/img_24.avif)

## `UIProgressBar`

```kotlin
uiProgressBar(size = Size(256, 8), current = 75f, maximum = 100f)
```

![img_17.png](/i/ui/img_17.avif)

## `UITreeView`

A bit rusty Tree View to render collapsable trees.

```kotlin
uiTreeView(UITreeViewList(listOf(
    UITreeViewNode("hello", listOf(
        UITreeViewNode("world")
    ))
)), size = Size(100, 100))
```

![img_18.png](/i/ui/img_18.avif)

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

![img_16.png](/i/ui/img_16.avif)

## `UIBreadCrumb`

```kotlin
uiBreadCrumb(listOf("hello", "world", "this", "is", "a", "path"))
```

![img_6.png](/i/ui/img_6.avif)

## `UIEditableNumber`

This allows the user to edit a number, while allowing to drag & drop or click to adjust it.

```kotlin
uiEditableNumber(10.0, min = 0.0, max = 100.0)
```

![img_7.png](/i/ui/img_7.avif)

## `UISlider`

A slider to select a number in a range.

![img_21.png](/i/ui/img_21.avif)

```kotlin
uiSlider(value = 50.0, min = -50.0, max = +50.0, step = 1.0) {
    showTooltip = true
    marks = true
    textTransformer = { "${it}" }
    decimalPlaces = 2
    styles {
        uiSelectedColor = MaterialColors.RED_600
        uiBackgroundColor = MaterialColors.BLUE_50
    }
    changed {
        println("SLIDER changed to $it")
    }
}
```

It is possible to show or disable marks with `slider.marks = true/false`.

**Configuring slider tooltips**

You can configure tooltips with `slider.showTooltips`:

```kotlin
slider.showTooltips = true // always show tooltips
slider.showTooltips = false // never show tooltips
slider.showTooltips = null // the default: show only while dragging
```

**Configuring slider marks**

Some small dots can be configured to visually display steps inside the slider.

```kotlin
slider.marks = true // show slider marks
slider.marks = false // hides slider marks
```

**Configuring tooltip text**

It is possible to configure the tooltip text transforming the value:

```kotlin
slider.textTransformer = { "${it}º" }
```

**Tooltip decimal places**

It is possible to configure the tooltip decimal places with:

```kotlin
slider.decimalPlaces = null // auto-configured based on the step
slider.decimalPlaces = 2 // 2 decimal places X.YY
```

**Subscribing to tooltip changes**

To subscribe to changes to the slider:

```kotlin
slider.onChange.add { println("Slider changed to $it") }
slider.changed { newValue -> println("Slider changed to $newValue") }
```

**Styling**

It is possible to adjust the color of the slider:

```kotlin
slider.styles {
    uiSelectedColor = MaterialColors.RED_600
    uiBackgroundColor = MaterialColors.BLUE_50
}
```

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

![img_19.png](/i/ui/img_19.avif)

## `UISpacer`

```kotlin
uiVerticalStack {
    uiButton("")
    uiButton("")
    uiSpacing(Size(0, 10))
    uiButton("")
}
```

![img_22.png](/i/ui/img_22.avif)

## Layouts

### `UIScrollableContainer`

```kotlin
uiScrollable(Size(300, 300)) {
    image(resourcesVfs["korge.png"].readBitmapSlice())
}
```

![img_20.png](/i/ui/img_20.avif)

### `UIVerticalStack`

To arrange elements vertically.

```kotlin
uiVerticalStack(padding = 4f) {
    uiButton("vertical")
    uiButton("stack")
    uiButton("with padding")
}
```

![img_8.png](/i/ui/img_8.avif)

### `UIHorizontalStack`

To arrange elements horizontally.

```kotlin
uiHorizontalStack(padding = 4f) {
    uiButton("horizontal")
    uiButton("stack")
    uiButton("with padding")
}
```

![img_9.png](/i/ui/img_9.avif)

### `UIGridFill`

To arrange elements in a table shape.

```kotlin
uiGridFill(cols = 3, rows = 3) {
    for (n in 0 until 9) uiButton("$n")
}
```

![img_10.png](/i/ui/img_10.avif)

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

![img_1.png](/i/ui/img_1.avif)
