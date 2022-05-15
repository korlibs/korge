package com.soywiz.korge.scene

import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SolidRect
import com.soywiz.korim.color.Colors

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

		override suspend fun Container.sceneInit() {
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
}
