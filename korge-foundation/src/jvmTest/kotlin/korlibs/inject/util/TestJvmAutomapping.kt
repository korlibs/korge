package korlibs.inject.util

import korlibs.inject.*
import kotlinx.coroutines.*
import org.junit.Test
import java.io.*
import kotlin.test.*

@Suppress("RemoveExplicitTypeArguments")
class JvmAutomappingTest {
    @Test
    fun test() = runBlocking {
        val injector = Injector().jvmAutomapping()
        injector.mapInstance(Folders(File(".")))
        injector.get<ConfigService>()
        assertSame(injector.get<ConfigService>(), injector.get<ConfigService>())
        assertNotSame(injector.get<MyPrototype>(), injector.get<MyPrototype>())
    }

    class Folders(val a: File)
    @Singleton
    class ConfigService(val folders: Folders)
    @Prototype
    class MyPrototype(val folders: Folders)
}
