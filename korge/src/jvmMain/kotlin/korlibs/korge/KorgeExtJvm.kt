package korlibs.korge

import korlibs.korge.view.*
import kotlinx.coroutines.*
import java.util.*

interface ViewsCompleter {
    fun completeViews(views: Views)
}

internal actual fun completeViews(views: Views) {
    for (completer in ServiceLoader.load(ViewsCompleter::class.java).toList()) {
        completer.completeViews(views)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal actual fun beforeStartingKorge(config: Korge) {
    if (config.realDebugCoroutines) {
        kotlinx.coroutines.debug.DebugProbes.enableCreationStackTraces = true
        kotlinx.coroutines.debug.DebugProbes.install()
    }
}
