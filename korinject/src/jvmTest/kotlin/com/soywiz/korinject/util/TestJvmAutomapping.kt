package com.soywiz.korinject.util

import com.soywiz.korinject.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Test
import java.io.*
import kotlin.test.*

@Suppress("RemoveExplicitTypeArguments")
class JvmAutomappingTest {
    @Test
    fun test() = runBlocking {
        val injector = AsyncInjector().jvmAutomapping()
        injector.mapInstance(Folders(File(".")))
        injector.get<ConfigService>()
        assertSame(injector.get<ConfigService>(), injector.get<ConfigService>())
        assertNotSame(injector.get<MyPrototype>(), injector.get<MyPrototype>())
        Unit
    }

    class Folders(val a: File)
    @Singleton
    class ConfigService(val folders: Folders)
    @Prototype
    class MyPrototype(val folders: Folders)
}
