description = "Korge Core Libraries"

dependencies {
    //add("androidMainApi", "androidx.javascriptengine:javascriptengine:1.0.0-alpha05")
    //add("androidMainApi", "com.google.guava:guava:31.0.1-android")
    //add("androidMainApi", libs.kotlinx.coroutines.android)

    add("commonMainApi", project(":korge-foundation"))
    //add("commonTestApi", project(":korge-test"))
    add("commonTestApi", libs.kotlinx.coroutines.test)
    add("jvmMainApi", libs.asm.core)
    add("jvmMainApi", libs.asm.util)
}
