package com.soywiz.klock.internal

import com.soywiz.klock.DateTime
import kotlin.js.Date

@Deprecated("", replaceWith = ReplaceWith("", "com.soywiz.klock.js.toDateTime"), level = DeprecationLevel.HIDDEN)
fun Date.toDateTime() = DateTime(this.getTime())

@Deprecated("", replaceWith = ReplaceWith("", "com.soywiz.klock.js.toDate"), level = DeprecationLevel.HIDDEN)
fun DateTime.toDate() = Date(this.unixMillisDouble)
