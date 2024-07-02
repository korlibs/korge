package korlibs.korge

import korlibs.korge.view.*
import java.util.*

interface ViewsCompleter {
    fun completeViews(views: Views)
}

internal actual fun completeViews(views: Views) {
    for (completer in ServiceLoader.load(ViewsCompleter::class.java).toList()) {
        completer.completeViews(views)
    }
}
