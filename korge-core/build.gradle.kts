description = "Korge Core Libraries"

dependencies {
    configurations.findByName("androidMainApi")?.let {
        //add("androidMainApi", "androidx.javascriptengine:javascriptengine:1.0.0-alpha05")
        //add("androidMainApi", "com.google.guava:guava:31.0.1-android")
    }
    commonMainApi(project(":korlibs-wasm"))
    commonMainApi(project(":korlibs-ffi"))
    commonMainApi(project(":korlibs-image"))
    commonMainApi(project(":korlibs-io"))
    commonMainApi(project(":korlibs-template"))
    commonMainApi(project(":korge-foundation"))
    commonMainApi(project(":korlibs-serialization-yaml"))
    commonMainApi(project(":korlibs-serialization-toml"))
    commonMainApi(project(":korlibs-serialization-csv"))
    commonMainApi(libs.kotlinx.coroutines.core)
    commonMainApi(libs.kotlinx.atomicfu)
    //add("commonMainApi", libs.kotlinx.atomicfu)
    //add("commonTestApi", project(":korge-test"))
    commonTestApi(libs.kotlinx.coroutines.test)
    jvmMainImplementation(libs.asm.core)
    jvmMainImplementation(libs.asm.util)
}

korlibs.NativeTools.configureAndroidDependency(project, libs.kotlinx.coroutines.android)
