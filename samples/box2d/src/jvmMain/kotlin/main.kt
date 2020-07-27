import kotlinx.coroutines.*

// -XstartOnFirstThread
fun main(args: Array<String>) {
	runBlocking {
		main()
	}
}
