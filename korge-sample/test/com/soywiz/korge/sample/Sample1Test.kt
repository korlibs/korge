package com.soywiz.korge.sample

import com.soywiz.korge.animate.AnMovieClip
import com.soywiz.korge.tests.KorgeTest
import com.soywiz.korge.view.dump
import com.soywiz.korma.geom.IRectangle
import com.soywiz.korma.geom.Size
import org.junit.Assert
import org.junit.Test

class Sample1Test : KorgeTest() {
	@Test
	fun scene1() = testScene(Sample1Module, Sample1Scene::class.java) {
		Assert.assertEquals(Size(32, 32), Size(tileset.width, tileset.height))
		val textField = percent
		Assert.assertEquals("0%", textField.text)
		updateTime(100)
		Assert.assertEquals("0%", textField.text)
		updateTime(100)
		Assert.assertEquals("2%", textField.text)
		updateTime(100)
		Assert.assertEquals("4%", textField.text)
		Assert.assertEquals(0.7, image.alpha, 0.0)
		updateTime(100)
		updateMousePosition(385, 305)
		updateTimeSteps(10000, step = 500)
		Assert.assertEquals(1.0, image.alpha, 0.0)
		updateMousePosition(1000, 1000)
		Assert.assertEquals(0.7, image.alpha, 0.0)

		image.simulateOver()
		Assert.assertEquals(1.0, image.alpha, 0.0)
		image.simulateOut()
		Assert.assertEquals(0.7, image.alpha, 0.0)

		Assert.assertEquals(1.0, percent.alpha, 0.0)
		percent.simulateClick()
		Assert.assertEquals(0.5, percent.alpha, 0.0)
		Assert.assertEquals(IRectangle(111, 3, 71, 32), percent.getGlobalBounds().toInt())
		Assert.assertTrue(percent.isVisibleToUser())
	}

	@Test
	fun scene2() = testScene(Sample1Module, Sample2Scene::class.java) {
		Assert.assertNotNull(this.test4Library)
		Assert.assertTrue(sceneView.children.first() is AnMovieClip)
		views.root.dump()
	}
}
