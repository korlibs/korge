package com.soywiz.korge.services

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import com.google.android.gms.games.achievement.Achievement
import com.google.android.gms.tasks.Task
import com.soywiz.korge.service.android
import com.soywiz.korge.view.Views
import com.soywiz.korio.android.androidContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual fun CreateAchievements(views: Views): Achievements = object : Achievements(views) {
    val context get() = views.gameWindow.androidContext()
    val achievementsClient by lazy { Games.getAchievementsClient(context, GoogleSignIn.getLastSignedInAccount(context) ?: error("Missing google login")) }
    override suspend fun showAll() {
        context.startActivity(achievementsClient.achievementsIntent.await())
    }

    override suspend fun unlock(id: AchievementId) {
        achievementsClient.unlockImmediate(id.android()).await()
    }

    override suspend fun reveal(id: AchievementId) {
        achievementsClient.revealImmediate(id.android()).await()
    }

    override suspend fun setSteps(id: AchievementId, numSteps: Int) {
        achievementsClient.setStepsImmediate(id.android(), numSteps).await()
    }

    override suspend fun increment(id: AchievementId, numSteps: Int) {
        achievementsClient.incrementImmediate(id.android(), numSteps).await()
    }

    override suspend fun list(forceReload: Boolean): Flow<AchievementInfo> {
        return flow {
            val data = achievementsClient.load(forceReload).await()
            val buffer = data.get() ?: return@flow
            for (item in buffer) {
                emit(AchievementInfo(
                    id = AchievementId().android(item.achievementId),
                    app = ApplicationId().android(item.applicationId),
                    name = item.name,
                    description = item.description,
                    steps = item.currentSteps to item.totalSteps,
                    experience = item.xpValue.toDouble(),
                    unlockedImage = item.unlockedImageUri?.toString(),
                    revealedImage = item.unlockedImageUri?.toString(),
                    state = when (item.state) {
                        Achievement.STATE_HIDDEN -> AchievementState.HIDDEN
                        Achievement.STATE_REVEALED -> AchievementState.LOCKED
                        Achievement.STATE_UNLOCKED -> AchievementState.UNLOCKED
                        else -> AchievementState.LOCKED
                    },
                    incremental = item.type == Achievement.TYPE_INCREMENTAL,
                ))
            }
        }
    }
}

actual fun CreateLeaderboards(views: Views): Leaderboards = object : Leaderboards(views) {
}

private suspend fun <T> Task<T>.await(): T {
    val deferred = CompletableDeferred<T>()
    this.addOnCanceledListener { deferred.cancel() }
    this.addOnFailureListener { deferred.completeExceptionally(it) }
    this.addOnSuccessListener { deferred.complete(it) }
    return deferred.await()
}
