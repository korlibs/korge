package korlibs.inject

import korlibs.inject.util.*
import kotlin.test.*

class AsyncInjectorSuspendContextTest {
    @Test
    fun testWithInjectorStoresInjectorInTheContext() = suspendTest {
        val injector = Injector()
        val string = "hello"
        injector.mapInstance(string)
        val result = withInjector(injector) {
            otherFunction()
        }
        assertEquals(string, result)
    }

    @Test
    fun testWithoutInjector() = suspendTest {
        val injector = Injector()
        val string = "hello"
        injector.mapInstance(string)
        assertFailsWith<IllegalStateException> {
            otherFunction()
        }
    }

    suspend fun otherFunction(): String = injector().get()
}
