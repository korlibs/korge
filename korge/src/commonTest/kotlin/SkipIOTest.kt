import com.soywiz.korio.util.*

//val skipIOTest get() = OS.isJsBrowser
val skipIOTest get() = OS.isJsBrowserOrWorker || OS.isJsNodeJs
val doIOTest get() = !skipIOTest
