package com.soywiz.korge.android

import com.soywiz.korge.view.*
import com.soywiz.korgw.*

val Views.androidActivity: KorgwActivity get() = (views.gameWindow as AndroidGameWindow).activity
