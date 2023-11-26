package korlibs.io.async

import kotlin.coroutines.*

val CoroutineContext.preferSyncIo: Boolean get() = this[PreferSyncIo]?.preferSyncIo == true

data class PreferSyncIo(val preferSyncIo: Boolean) : CoroutineContext.Element {
    override val key get() = PreferSyncIo
    companion object : CoroutineContext.Key<PreferSyncIo> {
        operator fun invoke(preferSyncIo: Boolean?): PreferSyncIo = if (preferSyncIo == true) SYNC else ASYNC
        val SYNC = PreferSyncIo(preferSyncIo = true)
        val ASYNC = PreferSyncIo(preferSyncIo = false)
    }
}
