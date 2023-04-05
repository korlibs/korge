package korlibs.korge.view

import korlibs.inject.*
import korlibs.korge.scene.*
import korlibs.korge.tests.*
import kotlin.test.*

class ViewsInjectorTest : ViewsForTesting() {
    @Test
    fun testViewsInjector() = viewsTest {
        assertEquals(virtualSize.width.toInt(), injector().get<Views>().virtualWidth)
    }

    @Test
    fun testScene() = viewsTest {
        val log = arrayListOf<String>()

        class MyScene(val str: String) : Scene() {
            override suspend fun SContainer.sceneInit() {  log += "sinit:$str"; log += "init:${injector().getOrNull<String>()}" }
            override suspend fun SContainer.sceneMain() { log += "main:${injector().getOrNull<String>()}" }
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
