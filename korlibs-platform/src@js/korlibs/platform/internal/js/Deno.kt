package korlibs.platform.internal.js

import kotlin.js.*

external object Deno {
    fun open(path: String, options: dynamic): Promise<DenoFsFile>
    val build: DenoBuild

}

external interface DenoFsFile {
    fun close()
}


external interface DenoBuild {
    /** darwin, linux, windows, freebsd, netbsd, aix, solaris, illumos */
    val os: String

}



