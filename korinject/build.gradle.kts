description = "Asynchronous Injector for Kotlin"

dependencies {
	add("commonMainApi", project(":korcoutines"))
    add("commonTestApi", libs.kotlinx.coroutines.test)
}
