package korlibs.io.util

import korlibs.io.file.*
import java.net.*

val URL.basename: String get() = PathInfo(this.file).baseName
