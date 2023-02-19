import com.soywiz.kmem.*

//val skipIOTest get() = OS.isJsBrowser
val skipIOTest get() = Platform.isJsBrowserOrWorker || Platform.isJsNodeJs
val doIOTest get() = !skipIOTest
