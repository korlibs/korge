package com.soywiz.korio.util

fun String.substringAfterOrNull(delimiter: Char): String? = if (this.contains(delimiter)) this.substringAfter(delimiter) else null
fun String.substringBeforeOrNull(delimiter: Char): String? = if (this.contains(delimiter)) this.substringBefore(delimiter) else null
fun String.substringAfterLastOrNull(delimiter: Char): String? = if (this.contains(delimiter)) this.substringAfterLast(delimiter) else null
fun String.substringBeforeLastOrNull(delimiter: Char): String? = if (this.contains(delimiter)) this.substringBeforeLast(delimiter) else null

fun String.substringAfterOrNull(delimiter: String): String? = if (this.contains(delimiter)) this.substringAfter(delimiter) else null
fun String.substringBeforeOrNull(delimiter: String): String? = if (this.contains(delimiter)) this.substringBefore(delimiter) else null
fun String.substringAfterLastOrNull(delimiter: String): String? = if (this.contains(delimiter)) this.substringAfterLast(delimiter) else null
fun String.substringBeforeLastOrNull(delimiter: String): String? = if (this.contains(delimiter)) this.substringBeforeLast(delimiter) else null
