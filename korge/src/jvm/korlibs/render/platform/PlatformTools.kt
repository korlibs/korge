package korlibs.render.platform

import com.sun.jna.*

@Deprecated("")
typealias NativeName = korlibs.memory.dyn.osx.NativeName
@Deprecated("", ReplaceWith("korlibs.memory.dyn.osx.NativeLoad<T>(name)", "korlibs"))
inline fun <reified T : Library> NativeLoad(name: String) = korlibs.memory.dyn.osx.NativeLoad<T>(name)
