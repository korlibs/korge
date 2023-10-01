---
permalink: /physics/
group: reference
layout: default
title: Physics
title_prefix: KorGE
fa-icon: fa-car-crash
priority: 200
status: outdated
---

Some kind of games require some physics to work.
KorGE provides a set of hit-testing and collision detection utilities
and a port of Box2D out of the box:

## Hit Testing + Collision/Intersection Detection

You can define vectorial shapes associated to views to define
its shape for standard out of the box collision detection and hitTesting.

The [`Graphics`](/views/standard/#graphics) view uses
its shapes for collision detecting with other shapes and views.
For the rest of the views you can use the `hitShape` property defining

```kotlin
var View.hitShape: VectorPath?
```

### Hit Testing

You can call the hitTest method with global coordinates
to determine if a point is inside a View.

```kotlin
val View.hitTest(globalX: Double, globalY: Double)
```

### Collision/Intersection Detection

There are two supported collision kinds/methods: GLOBAL_RECT and SHAPE.
The GLOBAL_RECT just computes the global bounding box of the views
and check if they are colliding.
This method is the fastest available, but most of the times it is not enough.
And the SHAPE method just uses the shape set in `View.hitShape: VectorPath?`
and precisely checks if the two shapes are intersecting.

Manual checking (for example inside an `addUpdater`):

```kotlin
enum class CollisionKind { GLOBAL_RECT, SHAPE }
fun View.collidesWith(other: View, kind: CollisionKind = CollisionKind.GLOBAL_RECT): Boolean
fun View.collidesWith(otherList: List<View>, kind: CollisionKind = CollisionKind.GLOBAL_RECT): Boolean
```

Event-based methods:

```kotlin
fun View.onCollision(filter: (View) -> Boolean = { true }, root: View? = null, kind: CollisionKind = CollisionKind.GLOBAL_RECT, callback: View.(View) -> Unit): Cancellable
fun View.onDescendantCollision(root: View = this, filterSrc: (View) -> Boolean = { true }, filterDst: (View) -> Boolean = { true }, kind: CollisionKind = CollisionKind.GLOBAL_RECT, callback: View.(View) -> Unit): Cancellable
```

Be sure to filter out collision with the stage.
```
view.onCollision(filter = { it != this }) { // filters out collisions with the stage itself
	circle2.color = Colors.RED
}
```

If you use `onCollision` the collision will be triggered by the rectangular bounding box of the corresponding view.

{% include autoplay_video.html src="/i/onCollision.webm" %}

Handy extensions for checking with shape precision:

```kotlin
fun View.collidesWithShape(other: View): Boolean
fun View.collidesWithShape(otherList: List<View>): Boolean
fun View.onCollisionShape(filter: (View) -> Boolean = { true }, root: View? = null, callback: View.(View) -> Unit): Cancellable
```

Using `onCollisionShape` a collision will be triggered using the `hitShape` of a view. 

{% include autoplay_video.html src="/i/onCollisionShape.webm" %}

You can define your own `hitShapes` for costum views. Notice in the following sample animation, that the image view would originally have a rectangular boundingbox.

```
val planet = image(resourcesVfs["planet.png"].readBitmap())
	planet.hitShape {
		circle(planet.width/2, planet.width/2, 500.0)
	}
```

{% include autoplay_video.html src="/i/onCollisionShapeImage.webm" %}

### Video-tutorials

{% include youtube.html video_id="F1AXdD5bLjA" %}

## Box2D

You can use a port to Kotlin of the Box2D library:

```kotlin
korge {
	...
	supportBox2d()
}
```
[Example Project](https://github.com/korlibs/korge-samples/blob/master/samples/box2d)

### Sample

{% include sample.html sample="SimpleBox2dScene" %}

```kotlin
suspend fun main() = Korge(quality = GameWindow.Quality.PERFORMANCE, title = "My Awesome Box2D Game!") {
	views.clearColor = Colors.DARKGREEN
	solidRect(300, 200, Colors.DARKCYAN)
	graphics {
		fill(Colors.DARKCYAN) {
			rect(-100, -100, 300, 200)
		}
		fill(Colors.AQUAMARINE) {
			circle(0, 0, 100)
		}
		fill(Colors.AQUAMARINE) {
			circle(100, 0, 100)
		}
		position(100, 100)
	}.interactive()
	worldView {
		position(400, 400).scale(20)

		createBody {
			setPosition(0, -10)
		}.fixture {
			shape = BoxShape(100, 20)
			density = 0f
		}.setViewWithContainer(solidRect(100, 20, Colors.RED).position(-50, -10).interactive())

		// Dynamic Body
		createBody {
			type = BodyType.DYNAMIC
			setPosition(0, 7)
		}.fixture {
			shape = BoxShape(2f, 2f)
			density = 0.5f
			friction = 0.2f
		}.setView(solidRect(2f, 2f, Colors.GREEN).anchor(.5, .5).interactive())

		createBody {
			type = BodyType.DYNAMIC
			setPosition(0.75, 13)
		}.fixture {
			shape = BoxShape(2f, 2f)
			density = 1f
			friction = 0.2f
		}.setView(graphics {
			fill(Colors.BLUE) {
				rect(-1f, -1f, 2f, 2f)
			}
		}.interactive())

		createBody {
			type = BodyType.DYNAMIC
			setPosition(0.5, 15)
		}.fixture {
			shape = CircleShape().apply { m_radius = 2f }
			density = 22f
			friction = 3f
		}.setView(graphics {
			fill(Colors.BLUE) {
				circle(0, 0, 200)
			}
			fill(Colors.DARKCYAN) {
				circle(100, 100, 20)
			}
			scale(1f / 100f)
		}.interactive())
	}
}

fun <T : View> T.interactive(): T = this.apply {
	alpha = 0.5
	onOver { alpha = 1.0 }
	onOut { alpha = 0.5 }
}
```
