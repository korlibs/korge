@file:Suppress("PackageDirectoryMismatch")

package korlibs.time.js

import korlibs.time.*
import kotlin.js.Date

fun Date.toDateTime(): DateTime = DateTime(this.getTime())
fun DateTime.toDate(): Date = Date(this.unixMillisDouble)
