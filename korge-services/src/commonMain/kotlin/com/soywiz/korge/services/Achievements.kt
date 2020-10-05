package com.soywiz.korge.services

import com.soywiz.korge.view.Views
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

abstract class Achievements(val views: Views) {
    open suspend fun showAll(): Unit = Unit
    open suspend fun unlock(id: AchievementId): Unit = Unit
    open suspend fun reveal(id: AchievementId): Unit = Unit
    open suspend fun increment(id: AchievementId, numSteps: Int): Unit = Unit
    open suspend fun setSteps(id: AchievementId, numSteps: Int): Unit = Unit
    open suspend fun list(forceReload: Boolean = false): Flow<AchievementInfo> = flowOf()
}

enum class AchievementState { HIDDEN, LOCKED, UNLOCKED }

class AchievementInfo(
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
