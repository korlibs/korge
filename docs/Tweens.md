### Tweens:

```kotlin
view.tween(
	(vview::alpha..0.7..1.0).linear(),
	(vview::scale..0.8..1.0).easeOutElastic(),
	time = 300
)
```

```kotlin
view.tween(
	view::scale..0.1..1.2,
	view::rotationDegrees..360.0,
	time = 600, easing = Easings.EASE_OUT_ELASTIC
)
```
