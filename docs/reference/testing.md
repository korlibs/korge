---
permalink: /testing/
group: reference
layout: default
title: "Testing"
title_prefix: KorGE
fa-icon: fa-vial
priority: 7500
support_diagram: true
#status: new
---

KorGE provide mechanisms for testing views, scenes and suspending functions.



## Basics

For simple tests not depending on views, you can use the `suspendTest` function exposed by KorIO:

```kotlin
fun suspendTest(callback: suspend () -> Unit)
```

In your test:

```kotlin
class MyTestClass {
    @Test fun test() = suspendTest {
        assertEquals("world", resourcesVfs["hello.txt"].readString())
    }
}
```

## Views

When testing views, scenes or transitions, KorGE exposes the `ViewsForTesting` base class.
When using this class, tweens and everything that happens in time will be executed almost immediately and in order,
so the tests can run super fast without having to wait for animations, so it is pretty convenient.

### Declaration

```kotlin
open class ViewsForTesting(val frameTime: TimeSpan = 10.milliseconds, val size: SizeInt = SizeInt(640, 480)) {
    val elapsed get() = time - startTime
    
    // Methods to call in our tests
    fun viewsTest(block: suspend Stage.() -> Unit): Unit
    inline fun <reified S : Scene> sceneTest(module: Module? = null,
        timeout: TimeSpan? = DEFAULT_SUSPEND_TEST_TIMEOUT,
        frameTime: TimeSpan = this.frameTime,
        crossinline block: suspend S.() -> Unit): Unit
    
    // Simulate mouse actions
    suspend fun mouseMoveTo(x: Number, y: Number)
    suspend fun mouseDown()
    suspend fun mouseUp()
    
    // Simulate actions on the views
    suspend fun View.simulateClick()
    suspend fun View.simulateOver()
    suspend fun View.simulateOut()
    
    // Check if the view is visible to the user
    suspend fun View.isVisibleToUser(): Boolean    
}
```

### Example with views

```kotlin
class MyViewsTest : ViewsForTesting() {
    @Test
    fun test() = viewsTest {
        val log = arrayListOf<String>()
        val rect = solidRect(100, 100, Colors.RED)
        rect.onClick {
            log += "clicked"
        }
        assertEquals(1, views.stage.children.size)
        rect.simulateClick()
        assertEquals(true, rect.isVisibleToUser())
        tween(rect::x[-102], time = 10.seconds)
        assertEquals(Rectangle(x=-102, y=0, width=100, height=100), rect.globalBounds)
        assertEquals(false, rect.isVisibleToUser())
        assertEquals(listOf("clicked"), log)
    }
}
```

### Example with a scene

```kotlin
class MySceneTest : ViewsForTesting() {
    object DummyModule : Module() {
        override suspend fun Injector.configure() {
            mapSingleton {
                MyDependency()
            }
            
            mapPrototype {
                MyScene(get())
            }
        }
    }

    class MyScene(private val myDependency: MyDependency) : Scene() {
        lateinit var textView : Text

        override suspend fun Container.sceneInit() {
            textView = text("Hello World!")
        }
    }

    @Test
    fun sceneTestRunsScene() = sceneTest<MyScene>(DummyModule) {
        assertTrue(textView.isVisibleToUser())
    }

    @Test
    fun sceneTestCanOverrideBindingsForTesting() = sceneTest<MyScene>(DummyModule, {
        mapSingleton<MyDependency> {
            MyMockDependency()
        }
    }) {
        assertTrue(textView.isVisibleToUser())
    }
}
```

## Separating logic from presentation

While KorGE allows to do headless, fast testing of views, it is recommended to separate
your game logic from your presentation logic.

When possible, your game should be playable from code using plain models and you should
try to test as much as possible without using views at all.

If you represent your game as states and state transitions, you can then display those
actions with animations and tweens. And you can convert user interactions into actions.

```nomnoml
[States]
[User Interactions] -> [User Actions]
[User Actions] -> [State Transitions]
[State Transitions] -> [View Animations]
```

For example:

```kotlin
data class State(...)
sealed class Operation {
    data class Create(...) : Operation()
    data class Move(...) : Operation()
    data class Compact(...) : Operation()
    // ...
}
data class Transition(
    val prev: State,
    val next: State,
    val operations: List<Operation>
)
sealed class UserAction {
    data class RequestMove(...) : UserAction()
    // ...
}

fun applyUserAction(state: State, action: UserAction): Transition {
    // ... Here we compute the action, generating a new state and a set of internal operations
}

suspend fun animateTransition(transition: Transition) {
    // ... Here we perform view animations, based on the options ...
}

// Testing the logic
@Test
fun testMoveRequest() {
    val state = State()
    val action = UserAction.RequestMove(...)
    // ...
    val transition = applyUserAction(state, action)
    // ...
    veritfy(transition)
}

// Testing animations and views
@Test
suspend fun testTransitions() = viewsTest {
    // Here we actually test that the animations work as expected
}
```
