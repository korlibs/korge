package com.soywiz.klock.js

import com.soywiz.klock.DateTime
import kotlin.js.Date

fun Date.toDateTime() = DateTime(this.getTime())
fun DateTime.toDate() = Date(this.unixMillisDouble)
