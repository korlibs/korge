description = "Template Engine for Multiplatform Kotlin"

dependencies {
    //add("commonMainApi", project(":kds"))
    add("commonTestApi", project(":korcoutines"))
    add("commonTestApi", libs.kotlinx.coroutines.test)
}
