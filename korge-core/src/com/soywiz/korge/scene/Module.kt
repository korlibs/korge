package com.soywiz.korge.scene

import com.soywiz.korim.color.Colors

open class Module {
    open val bgcolor: Int = Colors.BLACK
    open val title: String = "Game"
    open val icon: String? = null
    open val width: Int = 640
    open val height: Int = 480
    open val mainScene: Class<out Scene> = Scene::class.java
}