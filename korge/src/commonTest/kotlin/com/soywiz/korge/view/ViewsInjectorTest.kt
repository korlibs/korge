package com.soywiz.korge.view

import com.soywiz.korge.scene.*
import com.soywiz.korge.tests.*
import com.soywiz.korinject.*
import kotlin.test.*

class ViewsInjectorTest : ViewsForTesting() {
    @Test
    fun testViewsInjector() = viewsTest {
        assertEquals(virtualSize.width, injector().get<Views>().virtualWidth)
    }

    @Test
    fun testScene() = viewsTest {
        val log = arrayListOf<String>()

        class MyScene(val str: String) : Scene() {
            override suspend fun Container.sceneInit() {  log += "sinit:$str"; log += "init:${injector().getOrNull<String>()}" }
            override suspend fun Container.sceneMain() { log += "main:${injector().getOrNull<String>()}" }
            override suspend fun sceneAfterInit() { log += "ainit:${injector().getOrNull<String>()}" }
            override suspend fun sceneBeforeLeaving() { log += "bleav:${injector().getOrNull<String>()}" }
            override suspend fun sceneDestroy() { log += "destroy:${injector().getOrNull<String>()}" }
            override suspend fun sceneAfterDestroy() { log += "adestroy:${injector().getOrNull<String>()}" }
        }

        injector.mapPrototype { MyScene(get()) }
        val mySceneContainer = sceneContainer(views)
        mySceneContainer.changeTo<MyScene>("hello")
        mySceneContainer.changeTo<EmptyScene>()
        assertEquals("sinit:hello,init:hello,main:hello,ainit:hello,bleav:hello,destroy:hello,adestroy:hello", log.joinToString(","))
    }
}
