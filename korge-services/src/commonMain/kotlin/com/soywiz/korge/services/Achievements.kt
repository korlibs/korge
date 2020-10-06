package com.soywiz.korge.services

import com.soywiz.korge.service.ServiceBaseId
import com.soywiz.korge.view.Views
import kotlinx.coroutines.flow.*

abstract class Achievements(val views: Views) {
    open suspend fun showAll(): Unit {
        println("WARNING: Not implemented Achievements.showAll")
    }
    open suspend fun unlock(id: AchievementId): Unit {
        println("WARNING: Not implemented Achievements.unlock($id)")
    }
    open suspend fun reveal(id: AchievementId): Unit {
        println("WARNING: Not implemented Achievements.reveal($id)")
    }
    open suspend fun increment(id: AchievementId, numSteps: Int): Unit {
        println("WARNING: Not implemented Achievements.increment($id, $numSteps)")
    }
    open suspend fun setSteps(id: AchievementId, numSteps: Int): Unit {
        println("WARNING: Not implemented Achievements.setSteps($id, $numSteps)")
    }
    open suspend fun list(forceReload: Boolean = false): Flow<AchievementInfo> = flow {
        println("WARNING: Not implemented Achievements.list(forceReload=$forceReload)")
    }
}

enum class AchievementState { HIDDEN, LOCKED, UNLOCKED }

data class AchievementInfo(
    val id: AchievementId,
    val app: ApplicationId,
    val steps: Pair<Int, Int>,
    val experience: Double,
    val name: String,
    val description: String,
    val unlockedImage: String?,
    val revealedImage: String?,
    val state: AchievementState,
    val incremental: Boolean
)

class AchievementId() : ServiceBaseId()

expect fun CreateAchievements(views: Views): Achievements
