description = "Asynchronous Injector for Kotlin"

val coroutinesVersion: String by project

dependencies {
	add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
}
