import com.soywiz.korio.util.*

val skipIOTest get() = OS.isJs || OS.isAndroid
val doIOTest get() = !skipIOTest

