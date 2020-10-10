package com.soywiz.korge.admob

import com.soywiz.korge.view.Views

actual suspend fun AdmobCreate(views: Views, testing: Boolean): Admob = object : Admob(views) { }
