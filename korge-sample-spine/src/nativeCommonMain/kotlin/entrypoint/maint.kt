package entrypoint

import kotlinx.coroutines.*

fun main(args: Array<String>) {
    runBlocking {
        common.main()
    }
}
