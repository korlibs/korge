package com.soywiz.korge.admob

actual suspend fun AdmobCreate(testing: Boolean): Admob = object : Admob() { }
