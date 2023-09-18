package korlibs.platform

enum class Runtime {
    JS, JVM, ANDROID, NATIVE, WASM;

    val isJs: Boolean get() = this == JS
    val isJvm: Boolean get() = this == JVM
    val isAndroid: Boolean get() = this == ANDROID
    val isNative: Boolean get() = this == NATIVE
    val isJvmOrAndroid: Boolean get() = isJvm || isAndroid
    val isWasm: Boolean get() = this == WASM

    companion object {
        val CURRENT: Runtime get() = currentRuntime
    }
}
