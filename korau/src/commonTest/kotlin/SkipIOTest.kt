import com.soywiz.kmem.*

val skipIOTest: Boolean get() = Platform.isJs || Platform.isAndroid
//val skipIOTest: Boolean get() = OS.isAndroid
val doIOTest: Boolean get() = !skipIOTest

