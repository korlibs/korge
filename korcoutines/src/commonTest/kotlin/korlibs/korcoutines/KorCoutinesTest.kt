package korlibs.korcoutines

import kotlin.test.*

class KorCoutinesTest {
    @Test
    fun test() {
        kotlinx.coroutines.flow.flow<Int> {
            emit(0)
            KorCoroutines
        }
    }
}
