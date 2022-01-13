package com.soywiz.klock

fun DateFormat.parseLong(str: String) = parse(str).local.unixMillisLong
fun DateFormat.parseDouble(str: String) = parse(str).local.unixMillisDouble
fun DateFormat.parseDoubleOrNull(str: String) = tryParse(str)?.local?.unixMillisDouble
