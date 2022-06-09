package com.soywiz.korge.compose

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.*
import com.soywiz.klock.*
import com.soywiz.korge.view.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

// A function like the following could be created to create a composition provided a root Node.
fun setComposeContent(
    view: View,
    views: Views = view.stage!!.views,
    content: @Composable () -> Unit
): Composition {
    val context = MonotonicClockImpl(views) + views.coroutineContext

    val snapshotManager = GlobalSnapshotManager(views.gameWindow.coroutineDispatcher)
    snapshotManager.ensureStarted()

    val recomposer = Recomposer(context)

    CoroutineScope(context).launch(start = CoroutineStart.UNDISPATCHED) {
        println("runRecomposeAndApplyChanges")
        recomposer.runRecomposeAndApplyChanges()
    }

    //CoroutineScope(context).launch(start = CoroutineStart.UNDISPATCHED) {
    //}

    return ControlledComposition(NodeApplier(view), recomposer).apply {
        setContent(content)
    }
}
