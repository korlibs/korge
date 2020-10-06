package com.soywiz.korge.services

import com.soywiz.korge.service.ServiceBaseId
import com.soywiz.korge.view.Views

abstract class Leaderboards(val views: Views) {

}

class LeaderboardId() : ServiceBaseId()

expect fun CreateLeaderboards(views: Views): Leaderboards
