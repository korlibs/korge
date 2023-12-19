package korlibs.io.hash

import korlibs.crypto.*
import korlibs.io.internal.*
import java.io.*

fun InputStream.hash(algo: HasherFactory): Hash = bytesTempPool.alloc { temp -> algo.digest(temp) { read(it) } }
