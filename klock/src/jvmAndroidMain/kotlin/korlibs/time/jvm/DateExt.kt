package korlibs.time.jvm

import korlibs.time.DateTime
import java.util.*

fun Date.toDateTime() = DateTime(this.time)
fun DateTime.toDate() = Date(this.unixMillisLong)