import com.soywiz.korio.util.OS

val skipIOTest get() = OS.isJs || OS.isAndroid
val doIOTest get() = !skipIOTest

