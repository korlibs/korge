package korlibs.korge.gradle

object BuildVersions {
    const val GIT = "---"
    const val KOTLIN = "1.9.20-Beta"
    const val NODE_JS = "16.9.1"
    const val JNA = "5.13.0"
    const val COROUTINES = "1.7.2-wasm1"
    const val ANDROID_BUILD = "7.3.1"
    const val KOTLIN_SERIALIZATION = "1.5.1-wasm1"
    const val KORGE_TEST = "999.0.0.999"
    const val KORGE_CORE = "999.0.0.999"
    const val KORGE_FOUNDATION = "999.0.0.999"
    const val KORLIBS = "999.0.0.999"
    const val KRYPTO = "999.0.0.999" // Deprecated
    const val KLOCK = "999.0.0.999" // Deprecated
    const val KDS = "999.0.0.999" // Deprecated
    const val KMEM = "999.0.0.999" // Deprecated
    const val KORMA = "999.0.0.999" // Deprecated
    const val KORIO = "999.0.0.999" // Deprecated
    const val KORIM = "999.0.0.999" // Deprecated
    const val KORAU = "999.0.0.999" // Deprecated
    const val KORGW = "999.0.0.999" // Deprecated
    const val KORTE = "999.0.0.999" // Deprecated
    const val KORGE = "999.0.0.999"

    val ALL_PROPERTIES by lazy { listOf(
        ::GIT, ::KRYPTO, ::KLOCK, ::KDS, ::KMEM, 
        ::KORMA, ::KORIO, ::KORIM, ::KORAU, ::KORGW, ::KORGE,
         ::KOTLIN, ::JNA, ::COROUTINES, ::ANDROID_BUILD, ::KOTLIN_SERIALIZATION,
         ::KORGE_TEST, ::KORGE_CORE, ::KORGE_FOUNDATION,
    ) }
    val ALL by lazy { ALL_PROPERTIES.associate { it.name to it.get() } }
}