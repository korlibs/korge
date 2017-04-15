package com.soywiz.korge.sample

import com.soywiz.korge.animate.AnMovieClip
import com.soywiz.korge.animate.AnTextField
import com.soywiz.korge.tests.KorgeTest
import com.soywiz.korge.view.dump
import com.soywiz.korge.view.findFirstWithName
import com.soywiz.korma.geom.Size
import org.junit.Assert
import org.junit.Test

class Sample1Test : KorgeTest() {
	@Test
	fun scene1() = testScene(Sample1Module, Sample1Scene::class.java) {
		Assert.assertEquals(Size(32, 32), Size(tileset.width, tileset.height))
		val textField = sceneView.findFirstWithName("percent") as AnTextField
		Assert.assertEquals("0%", textField.text)
		updateTime(100)
		Assert.assertEquals("0%", textField.text)
		updateTime(100)
		Assert.assertEquals("2%", textField.text)
		updateTime(100)
		Assert.assertEquals("4%", textField.text)
	}

	@Test
	fun scene2() = testScene(Sample1Module, Sample2Scene::class.java) {
		Assert.assertNotNull(this.test4Library)
		Assert.assertTrue(sceneView.children.first() is AnMovieClip)
		views.root.dump()
	}
}
