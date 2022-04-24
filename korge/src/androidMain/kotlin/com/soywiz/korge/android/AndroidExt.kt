package com.soywiz.korge.android

import android.content.Context
import com.soywiz.korge.view.*
import com.soywiz.korgw.*

val Views.androidContext: Context get() = (views.gameWindow as BaseAndroidGameWindow).context
val Views.androidActivityOrNull: KorgwActivity? get() = (views.gameWindow as? AndroidGameWindow?)?.activity
val Views.androidActivity: KorgwActivity get() = androidActivityOrNull ?: error("Couldn't find KorgwActivity")
