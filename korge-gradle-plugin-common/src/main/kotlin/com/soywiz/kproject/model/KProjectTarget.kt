package com.soywiz.kproject.model

enum class KProjectTarget(
    val bname: String,
) {
    JVM("jvm"),
    JS("js"),
    ANDROID("android"),
    //DESKTOP("desktop"),
    MOBILE("mobile"),
    WASM_JS("wasmJs"),
    ;

    //val isKotlinNative: Boolean get() = this == DESKTOP || this == MOBILE
    val isKotlinNative: Boolean get() = this == MOBILE
    val isJvm: Boolean get() = this == JVM
    val isWasm: Boolean get() = this == WASM_JS
    val isWasmJs: Boolean get() = this == WASM_JS
    val isAndroid: Boolean get() = this == ANDROID
    val isJvmOrAndroid: Boolean get() = this == JVM || this == ANDROID
    val isJs: Boolean get() = this == JS

    companion object {
        val VALUES = values()
        val BY_NAME = VALUES.associateBy { it.bname }

        operator fun get(name: String): KProjectTarget? = BY_NAME[name.lowercase()]
    }
}
