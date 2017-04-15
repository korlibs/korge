package com.soywiz.korge.sample

import com.soywiz.korge.tests.KorgeTest
import org.junit.Test

class Sample1Test : KorgeTest() {
	@Test
	fun scene2() = testScene(Sample1Module, Sample2Scene::class.java) {
	}
}
