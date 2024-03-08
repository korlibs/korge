package korlibs.io.util.encoding

import korlibs.io.lang.Charset
import korlibs.io.lang.toByteArray
import korlibs.encoding.Base64

fun Base64.encode(src: String, charset: Charset): String = encode(src.toByteArray(charset))
