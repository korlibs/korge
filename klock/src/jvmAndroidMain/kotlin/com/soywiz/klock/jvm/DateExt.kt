package com.soywiz.klock.jvm

import com.soywiz.klock.DateTime
import java.util.*

fun Date.toDateTime() = DateTime(this.time)
fun DateTime.toDate() = Date(this.unixMillisLong)
