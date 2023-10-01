---
permalink: /views/input/
group: views
layout: default
title: Input
title_prefix: KorGE
fa-icon: fa-gamepad
priority: 30
---

## Accessing input state

The `Input` singleton contains the stage of the input values at a time.
It is updated once per frame. You can get it from the `Views` or the `Stage` singletons. 

```kotlin
val input = views.input
```

### Mouse

```kotlin
view.addUpdater {
    val xy: Point = input.mouse
    val buttons: Int = input.mouseButtons // flags with the pressed buttons
}
``` 

### Multi-touch

```kotlin
view.addUpdater {
    val ntouches: Int = input.activeTouches.size
    val touches: List<Touch> = input.activeTouches
    val rawTouch0: Touch = input.touches[0]
}
``` 

### Keys

```kotlin

import com.soywiz.klock.milliseconds

import com.soywiz.klogger.Console

import com.soywiz.korev.Key

view.addUpdater { timespan: TimeSpan ->
    val scale = timespan / 16.milliseconds
    if (input.keys[Key.LEFT]) x -= 2.0 * scale  // Same as `input.keys.pressing(Key.LEFT)`
    if (input.keys.pressing(Key.RIGHT)) x += 2.0 * scale
    if (input.keys.justPressed(Key.ESCAPE)) views.gameWindow.close(0)
    if (input.keys.justReleased(Key.ENTER)) Console.info("I'm working!")
}
```

### Gamepads

```kotlin
view.addUpdater {
    val gamepads = input.connectedGamepads
    val rawGamepad0 = input.gamepads[0]
    val pressedStart: Boolean = rawGamepad0[GameButton.START]
    val pos: Point = rawGamepad0[GameStick.LEFT]
}
```

## Event-based High-level API

KorGE provides a high level API to handle events attaching events to a View.
The events are only triggered when the associated View is attached to the stage.

### Mouse/Touch Events

You can handle `click`, `over` (hover), `out`, `down`, `downFromOutside`, `up`, `upOutside`, `upAnywhere`, `move`, `moveAnywhere`, `moveOutside` and `exit` mouse events.

For example:

```kotlin
view.mouse {
    click { /*...*/ }
    over { /*...*/ } // hover
    out { /*...*/ }
    down { /*...*/ }
    downFromOutside { /*...*/ }
    up { /*...*/ }
    upOutside { /*...*/ }
    upAnywhere { /*...*/ }
    move { /*...*/ }
    moveAnywhere { /*...*/ }
    moveOutside { /*...*/ }
    exit { /*...*/ }
}
```
or
```kotlin
view.onClick { /*...*/ } // suspending block
```

### Keys Events

For example:

```kotlin
// Matches any key
view.keys {
    down { e -> /*...*/ }
    up { e -> /*...*/ }
}

// Matches just one key
view.keys {
    // Executes when the key is down. Depending on the platform this might trigger multiple events. Use justDown to trigger it only once.
    down(Key.LEFT) { e -> /*...*/ }
    
    // Executes when the key is up
    up(Key.LEFT) { e -> /*...*/ }

    // Executes on every frame (be aware that fps might vary)
    downFrame(Key.LEFT) { e -> /*...*/ }
    // Executes every 16 milliseconds, when the key is down    
    downFrame(Key.LEFT, 16.milliseconds) { e -> /*...*/ }
    
    // Executes only when the key was pressed once, then it won't be triggered again until released and pressed again
    justDown(Key.LEFT) { e -> /*...*/ }
    
    // Useful for UIs, the code is executed every half a second at first, and then every 100 milliseconds doing an acceleration.
    downRepeating(Key.LEFT) { e -> /*...*/ }
}
view.onKeyDown { e -> /*...*/ } // suspending block
```

### Gamepad Events

```kotlin
view.gamepad {
    val playerId = 0
    connected { playerId -> /*...*/ }
    disconnected { playerId -> /*...*/ }
    stick(playerId, GameStick.LEFT) { x, y -> /*...*/ }
    button(playerId) { pressed, button, value -> /*...*/ }
    down(playerId, GameButton.BUTTON0) { /*...*/ }
}
```

## Handling RAW events via Components

You can handle RAW input by creating components that handle events
and attaching to a view.

```kotlin
// Executes once per frame
interface UpdateComponent : Component {
    fun update(ms: Double)
}

// New version of UpdateComponent
interface UpdateComponentV2 : UpdateComponent {
    override fun update(dt: HRTimeSpan)
}

// Handling all the Events
interface EventComponent : Component {
    fun onEvent(event: Event)
}

// Handling just Input Events
interface TouchComponent : Component {
    fun onTouchEvent(views: Views, e: TouchEvent)
}

interface MouseComponent : Component {
    fun onMouseEvent(views: Views, event: MouseEvent)
}

interface KeyComponent : Component {
    fun onKeyEvent(views: Views, event: KeyEvent)
}

interface GamepadComponent : Component {
    fun onGamepadEvent(views: Views, event: GamePadButtonEvent)
    fun onGamepadEvent(views: Views, event: GamePadStickEvent)
    fun onGamepadEvent(views: Views, event: GamePadConnectionEvent)
}
```

## Stage event dispatching

The stage singleton receives raw events.
You can call `stage.addEventListener`.

NOTE: Remember to remove the event listener once finished with it.
Since you are adding it to the root node, it won't be collected automatically.

Example:

```kotlin
stage.addEventListener<ReshapeEvent> { e ->
}
``` 

Available event types:

* `MouseEvent`
* `TouchEvent`
* `ReshapeEvent`
* `KeyEvent`
* `GamePadConnectionEvent`
* `GamePadUpdateEvent`
* `GamePadButtonEvent`
* `GamePadStickEvent`

## APIs

### Mouse

```kotlin
// Configuring MouseEvents
val View.mouse: MouseEvents
inline fun <T> View.mouse(callback: MouseEvents.() -> T): T = mouse.run(callback)

// Shortcuts
inline fun <T : View?> T.onClick(noinline handler: suspend (MouseEvents) -> Unit): T
inline fun <T : View?> T.onOver(noinline handler: suspend (MouseEvents) -> Unit): T
inline fun <T : View?> T.onOut(noinline handler: suspend (MouseEvents) -> Unit): T
inline fun <T : View?> T.onDown(noinline handler: suspend (MouseEvents) -> Unit): T
inline fun <T : View?> T.onDownFromOutside(noinline handler: suspend (MouseEvents) -> Unit): T
inline fun <T : View?> T.onUp(noinline handler: suspend (MouseEvents) -> Unit): T
inline fun <T : View?> T.onUpOutside(noinline handler: suspend (MouseEvents) -> Unit): T
inline fun <T : View?> T.onUpAnywhere(noinline handler: suspend (MouseEvents) -> Unit): T
inline fun <T : View?> T.onMove(noinline handler: suspend (MouseEvents) -> Unit): T

class MouseEvents(override val view: View) : MouseComponent, UpdateComponentWithViews {
    val click = Signal<MouseEvents>()
    val over = Signal<MouseEvents>()
    val out = Signal<MouseEvents>()
    val down = Signal<MouseEvents>()
    val downFromOutside = Signal<MouseEvents>()
    val up = Signal<MouseEvents>()
    val upOutside = Signal<MouseEvents>()
    val upAnywhere = Signal<MouseEvents>()
    val move = Signal<MouseEvents>()
    val moveAnywhere = Signal<MouseEvents>()
    val moveOutside = Signal<MouseEvents>()
    val exit = Signal<MouseEvents>()
    
    val startedPos = Point()
    val lastPos = Point()
    val currentPos = Point()
    
    val hitTest: View?
    
    var downPos = Point()
    var upPos = Point()
    var clickedCount = 0
    val isOver: Boolean
}
```

### Keys


```kotlin
// Configuring KeysEvents
val View.keys: KeysEvents
inline fun <T> View.keys(callback: KeysEvents.() -> T): T

// Shortcuts
inline fun <T : View?> T?.onKeyDown(noinline handler: suspend (KeyEvent) -> Unit)
inline fun <T : View?> T?.onKeyUp(noinline handler: suspend (KeyEvent) -> Unit)
inline fun <T : View?> T?.onKeyTyped(noinline handler: suspend (KeyEvent) -> Unit)

class KeysEvents(override val view: View) : KeyComponent {
    val onKeyDown = AsyncSignal<KeyEvent>()
    val onKeyUp = AsyncSignal<KeyEvent>()
    val onKeyTyped = AsyncSignal<KeyEvent>()
    
    // Handlers for a specific Key 
    fun down(key: Key, callback: (key: Key) -> Unit): Closeable
    fun up(key: Key, callback: (key: Key) -> Unit): Closeable
    fun typed(key: Key, callback: (key: Key) -> Unit): Closeable
    
    // Handlers for any keys
    fun down(callback: (key: Key) -> Unit): Closeable
    fun up(callback: (key: Key) -> Unit): Closeable
    fun typed(callback: (key: Key) -> Unit): Closeable
}
```

### Gamepad

```kotlin
// Configuring GamePadEvents
val View.gamepad: GamePadEvents
inline fun <T> View.gamepad(callback: GamePadEvents.() -> T): T

class GamePadEvents(override val view: View) : GamepadComponent {
	val stick = Signal<GamePadStickEvent>()
	val button = Signal<GamePadButtonEvent>()
	val connection = Signal<GamePadConnectionEvent>()

	fun stick(playerId: Int, stick: GameStick, callback: (x: Double, y: Double) -> Unit)
	fun down(playerId: Int, button: GameButton, callback: () -> Unit)
	fun up(playerId: Int, button: GameButton, callback: () -> Unit)
	fun connected(playerId: Int, callback: () -> Unit)
	fun disconnected(playerId: Int, callback: () -> Unit)
	override fun onGamepadEvent(views: Views, event: GamePadButtonEvent)
	override fun onGamepadEvent(views: Views, event: GamePadStickEvent)
	override fun onGamepadEvent(views: Views, event: GamePadConnectionEvent)
}

```

## Handling Drag & Drop File Events

It is possible to detect and react to drag & drop events inside KorGE.
To do so, you can handle events of the type `DropFileEvent`.
For simplicity, there is a method you can call from a view to register to DropFileEvent:

```kotlin
val dropFileRect = solidRect(Size(width, height), Colors.RED)
    .visible(false)
onDropFile {
    when (it.type) {
        DropFileEvent.Type.START -> dropFileRect.visible = true
        DropFileEvent.Type.END -> dropFileRect.visible = false
        DropFileEvent.Type.DROP -> {
            launchImmediately {
                it.files?.firstOrNull()?.let {
                    image(it.readBitmap()).size(Size(width, height))
                }
            }
        }
    }
}
```

{% include autoplay_video.html src="/i/file_drag_and_drop.webm" %}

Or if you want to register events directly:

```kotlin
onEvents(DropFileEvent.Type.START) { println("A file is being dragged into the window") }
onEvents(DropFileEvent.Type.DROP) { println("A file has been successfully dropped in the window. Files: ${it.files}") }
onEvents(DropFileEvent.Type.END) { println("The drag&drop finished either with or without drop") }
```

or:

```kotlin
onEvents(*DropFileEvent.Type.ALL) {
    println("${it.type}")
}
```

## Dragging Views

In the case you want to make a view draggable. There is a `View.draggable` and `View.draggableCloseable` extensions:

```kotlin
val solidRect = solidRect(Size(100, 100), Colors.RED)
val closeable = solidRect.draggableCloseable()
```

The Closeable version returns a Closeable instance allowing you to stop accepting the dragging after the close.

{% include autoplay_video.html src="/i/view_dragging.webm" %}

### Configure how dragging works

It is possible to configure the dragging View mediator, like this:

For example if you want only the dragging to work on the X or the Y you can set `autoMove = false`:

```kotlin
val closeable = solidRect.draggableCloseable(selector = solidRect, autoMove = false) { info: DraggableInfo ->
    //info.view.pos = info.viewNextXY
    info.view.x = info.viewNextXY.x
}
```
