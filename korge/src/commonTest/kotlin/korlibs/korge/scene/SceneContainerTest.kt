package korlibs.korge.scene

import korlibs.image.color.*
import korlibs.korge.tests.*
import korlibs.korge.view.*
import korlibs.korge.view.filter.*
import korlibs.time.*
import kotlin.test.*

class SceneContainerTest : ViewsForTesting() {
	data class SceneInfo(val name: String)

	val mylog = arrayListOf<String>()

	open inner class MyLogScene : LogScene() {
		override fun log(msg: String) {
			mylog += msg
		}
	}

	inner class Scene1(
		val info: SceneInfo
	) : MyLogScene() {
		override val sceneName: String get() = "Scene1"

		override suspend fun sceneAfterInit() {
			super.sceneAfterInit()
			log("$info")
			sceneContainer.changeTo<Scene2>()
		}
	}

	inner class Scene2 : MyLogScene() {
		override val sceneName: String get() = "Scene2"

		override suspend fun SContainer.sceneInit() {
			log("$sceneName.sceneInit")
			sceneView += SolidRect(100, 100, Colors.RED).apply {
				name = "box"
			}
		}
	}

	//@Test
	//fun name() = viewsTest {
	//	val sc = SceneContainer(views)
	//	injector.mapSingleton(ResourcesRoot::class) { ResourcesRoot() }
	//	injector.mapPrototype(Scene1::class) { Scene1(get(SceneInfo::class)) }
	//	injector.mapPrototype(Scene2::class) { Scene2() }
	//	views.stage += sc
	//	sc.changeTo<Scene1>(SceneInfo("hello"), time = 10.milliseconds)
	//	//sc.changeTo<Scene1>(time = 10)
	//	delay(10.milliseconds)
	//	assertNotNull(sc["box"])
	//	assertEquals(
	//		"Scene1.sceneInit, Scene1.sceneAfterDestroy, Scene1.sceneAfterInit, SceneInfo(name=hello), Scene2.sceneInit, Scene1.sceneDestroy, Scene2.sceneAfterDestroy, Scene2.sceneAfterInit",
	//		mylog.joinToString(", ")
	//	)
	//}

    @Test
    //fun testVerifyTransitionIsUsed() = suspendTest {
    fun testVerifyTransitionIsUsed() = viewsTest {
        //val log = ViewsLog(coroutineContext)
        //log.injector.mapSingleton { ResourcesRoot() }
        val sceneContainer = sceneContainer(views)
        val startTime = time
        sceneContainer.changeTo({ EmptyScene() }, time = 0.5.seconds, transition = MaskTransition(TransitionFilter.Transition.HORIZONTAL))
        val transitionView = sceneContainer.firstChild as TransitionView
        assertEquals("MaskTransition", transitionView.transition.toString())
        assertEquals(1f, transitionView.ratio)
        val endTime = time
        assertTrue { endTime - startTime in 0.5.seconds..0.75.seconds }
    }

    @Test
    //fun testVerifyTransitionIsUsed() = suspendTest {
    fun testVerifyTransitionIsUsed2() = viewsTest {
        //val log = ViewsLog(coroutineContext)
        //log.injector.mapSingleton { ResourcesRoot() }
        val sceneContainer = sceneContainer(views)
        views.injector.mapPrototype { EmptyScene() }
        val startTime = time
        sceneContainer.changeTo<EmptyScene>(time = 0.5.seconds, transition = MaskTransition(TransitionFilter.Transition.HORIZONTAL))
        val transitionView = sceneContainer.firstChild as TransitionView
        assertEquals("MaskTransition", transitionView.transition.toString())
        assertEquals(1f, transitionView.ratio)
        val endTime = time
        assertTrue { endTime - startTime in 0.5.seconds..0.75.seconds }
    }
}
