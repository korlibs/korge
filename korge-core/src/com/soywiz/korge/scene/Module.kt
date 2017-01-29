package com.soywiz.korge.scene

open class Module {
	open val mainScene: Class<out Scene> = Scene::class.java
}