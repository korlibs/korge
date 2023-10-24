package korlibs.io.internal

import korlibs.datastructure.*
import korlibs.io.lang.*

@PublishedApi
internal const val BYTES_TEMP_SIZE = 0x10000

@PublishedApi
internal val bytesTempPool by threadLocal { Pool(preallocate = 1) { ByteArray(BYTES_TEMP_SIZE) } }

@PublishedApi
internal val smallBytesPool by threadLocal { Pool(preallocate = 16) { ByteArray(16) } }
