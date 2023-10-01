---
permalink: /views/animation/
group: views
layout: default
title: Animating
title_prefix: KorGE
fa-icon: fa-play
priority: 31
---

![](/i/animation.avif)

## while (true) + delay

One way of animation in KorGE is to just make a loop and place a delay.
This method allows you to define complex logic inside the loop and
define state machines just by code. 
(Have in mind that this approach is likely to have some kind of stuttering.)

```kotlin
launchImmediately {
    while (true) {
        view.x++
        delay(16.milliseconds) // suspending
    }
}
```

## addFixedUpdater and addHrUpdater

One way of performing animations with KorGE is by attaching an updater component to
a view. While the view is visible that updater will be executed from time to time.

If you want a code to be executed a number of times per second, you can use addFixedUpdater.
(Have in mind that this approach is likely to have some kind of stuttering.)

```kotlin
view.addFixedUpdater(60.timesPerSecond) {
    x++
}
```

And if your code is designed to support arbitrary time deltas, you can use an updater:
 
```kotlin
view.addHrUpdater { dt -> // dt contains the delta time using as a HRTimeSpan inline class instance
    val scale = dt / 16.66666.hrMilliseconds
    x += 2.0 * scale
}
```

## Tweens

Korge integrates tweens and easings, and it is fully integrated with coroutines for your coding pleasure.

Games require tweening visual properties in order to be appealing.
Korge provides a simple, yet powerful interface for creating tweens.

### Simple interface

![](/i/animation.avif)

`View` has an extension method called `View.tween` that allows you to do the magic. And has the following definition:

```
suspend fun View?.tween(vararg vs: V2<*>, time: Int, easing: Easing = Easing.LINEAR, callback: (Double) -> Unit = { })
```

You have to use [bound callable references](https://kotlinlang.org/docs/reference/whatsnew11.html#bound-callable-references) to define properties that will change. And Korge provides some extension methods to bound callable references to generate tween parameters.

If you want to linearly interpolate `view.x` from `10.0` to `100.0` in one second you would write:
```
view.tween(view::x[10.0, 100.0], time = 1000.milliseconds)
```
Tip: import `com.soywiz.korge.tween.get` for this to compile. In IntelliJ place the caret in the \[ and press ALT+enter.

### delay + duration + easing

You can control the start time, duration and easing per interpolated property by chaining them in the parameter call, using these V2 extensions:

`V2.delay(timeMs:TimeSpan):V2`, `V2.duration(timeMs:TimeSpan):V2`, `V2.easing(easing:Easing):V2`

```
view.tween(
    view::x[100.0].delay(100.milliseconds).duration(500.milliseconds).easing(Easing.EASE_IN_OUT_QUAD),
    view::y[0.0, 200.0].delay(50.milliseconds),
    time = 1000.milliseconds
)
```

If you want to apply one easing for all subtweens defined in the paramters, just add an extra easing parameter to the `tween` call:

```
view.tween(
    //...
    easing = Easing.EASE_IN
)
```

### Implementation details

The tween execution will be attached as a component to the receiver View that holds the tween method. That means that the view has to be in the stage or be manually updated. Also means that any `View.speed` changes in that view or ancestors will affect the tween.

*PRO Tip:* You can even interpolate the `View.speed` property to get some cool time effects.

## Animator

You can also use an animator, which is almost as potent as the tweens. Check the [animate sample here](https://github.com/korlibs/korge-samples/blob/master/samples/animations/src/commonMain/kotlin/main.kt)
```
animate {
    parallel {
        view.moveToWithSpeed(500.0, 500.0, 300.0, Easing.EASE_IN_OUT)
        view.scaleTo(5.0, 5.0)
    }
    parallel {
        view.moveTo(0.0, 0.0)
    }
    block {
        rotateTo(10.degrees)
    }
}
```

### Easings

Korge provides an Easing class with the most common easings. And allows
you to create your own easings.

{% include sample.html sample="EasingsScene" %}

|                             |                             |                          |                            |
|-----------------------------|-----------------------------|--------------------------|----------------------------|
| Easings.EASE_IN_ELASTIC     | Easings.EASE_OUT_ELASTIC    | Easings.EASE_OUT_BOUNCE  | Easings.LINEAR             |
| Easings.EASE_IN             | Easings.EASE_OUT            | Easings.EASE_IN_OUT      | Easings.EASE_OUT_IN        |
| Easings.EASE_IN_BACK        | Easings.EASE_OUT_BACK       | Easings.EASE_IN_OUT_BACK | Easings.EASE_OUT_IN_BACK   |
| Easings.EASE_IN_OUT_ELASTIC | Easings.EASE_OUT_IN_ELASTIC | Easings.EASE_IN_BOUNCE   | Easings.EASE_IN_OUT_BOUNCE |
| Easings.EASE_OUT_IN_BOUNCE  | Easings.EASE_IN_QUAD        | Easings.EASE_OUT_QUAD    | Easings.EASE_IN_OUT_QUAD   |
{:.small}

## ANI/SWF Files

Korge supports [SWF Adobe Flash/Animate files](/store_proxy/?url=/module/korge-swf/) and can support
other custom formats through extensions/plugins.
For example it would be possible to create an exporter for *After Effects* or *Apple Motion*.

Korge defines a custom ANI file format for animations.
Atlas based with sound support.

You can preview ANI/SWF files (as they will look in runtime) right in intelliJ, using Korge's intelliJ plugin (*that uses Korge itself for rendering!*):

## Video-tutorial

{% include youtube.html video_id="ebW4Hr97h_I" %}

