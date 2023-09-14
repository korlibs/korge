package korlibs.io.file.std

import korlibs.io.async.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import platform.posix.*

actual open class LocalVfsNative actual constructor(async: Boolean) : LocalVfsNativeBase(async) {
    override suspend fun listFlow(path: String) = flow {
        val dir = memScoped { _wopendir(resolve(path).wcstr) }
        if (dir != null) {
            try {
                while (true) {
                    val dent = _wreaddir(dir) ?: break
                    val name = dent.pointed.d_name.toKString()
                    if (name != "." && name != "..") {
                        emit(file("$path/$name"))
                    }
                }
            } finally {
                _wclosedir(dir)
            }
        }
    }.flowOn(Dispatchers.CIO)
}
