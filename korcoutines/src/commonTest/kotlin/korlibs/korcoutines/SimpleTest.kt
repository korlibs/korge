package korlibs.korcoutines

import kotlin.coroutines.*
import kotlin.test.*

class SimpleTest {
    @Test
    fun test() {
        kotlinx.coroutines.flow.flow<Int> {
            emit(0)
        }
    }
}
