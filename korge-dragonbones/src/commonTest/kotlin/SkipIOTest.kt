import com.soywiz.korio.util.*

//val skipIOTest get() = OS.isJsBrowser
val skipIOTest get() = OS.isJs
val doIOTest get() = !skipIOTest
