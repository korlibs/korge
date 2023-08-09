description = "Asynchronous Injector for Kotlin"

dependencies {
	add("commonMainApi", libs.bundles.kotlinx.coroutines)
    add("commonTestApi", libs.kotlinx.coroutines.test)
}
