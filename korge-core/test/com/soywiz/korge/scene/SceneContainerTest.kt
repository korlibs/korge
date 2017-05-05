package com.soywiz.korge.scene

import com.soywiz.korge.log.Logger
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.get
import com.soywiz.korim.color.Colors
import org.junit.Assert
import org.junit.Test

class SceneContainerTest : ViewsForTesting() {
	data class SceneInfo(val name: String)

	class Scene1(
		val info: SceneInfo
	) : LogScene() {
		override suspend fun sceneAfterInit() {
			super.sceneAfterInit()
			logger.info("$info")
			sceneContainer.changeTo<Scene2>()
		}
	}

	class Scene2 : LogScene() {
		suspend override fun sceneInit(sceneView: Container) {
			super.sceneInit(sceneView)
			sceneView += views.solidRect(100, 100, Colors.RED).apply {
				name = "box"
			}
		}
	}

	@Test
	fun name() = viewsTest {
		val out = arrayListOf<String>()
		injector.mapTyped<Logger>(Logger { msg -> out += msg })
		val sc = SceneContainer(views)
		views.stage += sc
		sc.changeTo<Scene1>(SceneInfo("hello"), time = 10)
		//sc.changeTo<Scene1>(time = 10)

		sleep(10)

		Assert.assertNotNull(sc["box"])
		Assert.assertEquals(
			"Scene1.sceneInit, Scene1.sceneAfterDestroy, Scene1.sceneAfterInit, SceneInfo(name=hello), Scene2.sceneInit, Scene1.sceneDestroy, Scene2.sceneAfterDestroy, Scene2.sceneAfterInit",
			out.joinToString(", ")
		)
	}
}
