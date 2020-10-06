package com.soywiz.korge.services

import com.soywiz.korge.view.Views

actual fun CreateAchievements(views: Views): Achievements = object : Achievements(views) {
}

actual fun CreateLeaderboards(views: Views): Leaderboards = object : Leaderboards(views) {
}

actual fun CreateNativeLogin(views: Views): NativeLogin = object : NativeLogin(views) {
}
