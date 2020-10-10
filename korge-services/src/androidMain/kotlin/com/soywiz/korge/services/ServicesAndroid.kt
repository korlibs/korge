package com.soywiz.korge.services

import android.content.*
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.games.*
import com.google.android.gms.games.achievement.*
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.android.gms.tasks.*
import com.soywiz.klock.seconds
import com.soywiz.korge.service.*
import com.soywiz.korge.view.*
import com.soywiz.korio.android.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import com.soywiz.korio.async.delay
import com.soywiz.korgw.*

actual fun CreateAchievements(views: Views): Achievements = object : Achievements(views) {
    val context get() = views.coroutineContext.androidContext()

    suspend fun achievementsClient(): AchievementsClient {
        //com.soywiz.korio.async.withTimeout(10.seconds) {
        //    while (GoogleSignIn.getLastSignedInAccount(context) == null) {
        //        println("CreateAchievements.achievementsClient.startingIntent")
        //        val result = context.startActivityWithResult(GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).getSignInIntent())
        //        println("CreateAchievements.achievementsClient: $result")
        //        delay(1.seconds)
        //    }
        //}
        return Games.getAchievementsClient(context, GoogleSignIn.getLastSignedInAccount(context) ?: error("Missing google login"))
    }

    override suspend fun showAll() {
        context.startActivity(achievementsClient().achievementsIntent.await())
    }

    override suspend fun unlock(id: AchievementId) {
        achievementsClient().unlockImmediate(id.android()).await()
    }

    override suspend fun reveal(id: AchievementId) {
        achievementsClient().revealImmediate(id.android()).await()
    }

    override suspend fun setSteps(id: AchievementId, numSteps: Int) {
        achievementsClient().setStepsImmediate(id.android(), numSteps).await()
    }

    override suspend fun increment(id: AchievementId, numSteps: Int) {
        achievementsClient().incrementImmediate(id.android(), numSteps).await()
    }

    override suspend fun list(forceReload: Boolean): Flow<AchievementInfo> = flow {
        val data = achievementsClient().load(forceReload).await()
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

actual fun CreateLeaderboards(views: Views): Leaderboards = object : Leaderboards(views) {
}

actual fun CreateNativeLogin(views: Views): NativeLogin = object : NativeLogin(views) {
    val context get() = views.coroutineContext.androidContext() as KorgwActivity

    override suspend fun login() {
        context.startActivityWithResult(GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).getSignInIntent())
    }
}

private suspend fun <T> Task<T>.await(): T {
    val deferred = CompletableDeferred<T>()
    this.addOnCanceledListener { deferred.cancel() }
    this.addOnFailureListener { deferred.completeExceptionally(it) }
    this.addOnSuccessListener { deferred.complete(it) }
    return deferred.await()
}
