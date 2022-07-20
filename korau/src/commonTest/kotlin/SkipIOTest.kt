import com.soywiz.korio.util.OS

val skipIOTest: Boolean get() = OS.isJs || OS.isAndroid
val doIOTest: Boolean get() = !skipIOTest

