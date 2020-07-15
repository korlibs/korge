package com.soywiz.klock

@Deprecated("", replaceWith = ReplaceWith("", "com.soywiz.klock.jvm.toDateTime"), level = DeprecationLevel.HIDDEN)
fun java.util.Date.toDateTime() = DateTime(this.time)

@Deprecated("", replaceWith = ReplaceWith("", "com.soywiz.klock.jvm.toDate"), level = DeprecationLevel.HIDDEN)
fun DateTime.toDate() = java.util.Date(this.unixMillisLong)
