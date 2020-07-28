package com.soywiz.korge.intellij

object KorgeResources {
	val classLoader = KorgeResources::class.java.classLoader

	fun getBytes(path: String) = classLoader.getResource(path)?.readBytes() ?: error("Can't find resource '$path'")

	val KORGE_IMAGE get() = getBytes("/com/soywiz/korge/intellij/generator/korge.png")
}