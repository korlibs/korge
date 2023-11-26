package korlibs.korge.audio

import korlibs.audio.sound.*
import korlibs.audio.sound.fade.*
import korlibs.datastructure.*
import korlibs.korge.view.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.reflect.*

fun audioChannel(isLocal: Boolean = false): AudioChannel.Provider = if (isLocal) AudioChannel.ProviderLocal else AudioChannel.ProviderGlobal

class AudioChannel(val name: String, val viewsContainer: ViewsContainer, val isLocal: Boolean = false) {
    interface Provider {
        operator fun getValue(viewsContainer: ViewsContainer, property: KProperty<*>): AudioChannel
    }
    object ProviderLocal : Provider {
        override operator fun getValue(viewsContainer: ViewsContainer, property: KProperty<*>): AudioChannel = viewsContainer.views.extraCache("audio-channel-ref-local-${property.name}") {
            AudioChannel(property.name, viewsContainer, isLocal = true)
        }
    }
    object ProviderGlobal : Provider {
        override operator fun getValue(viewsContainer: ViewsContainer, property: KProperty<*>): AudioChannel = viewsContainer.views.extraCache("audio-channel-ref-global-${property.name}") {
            AudioChannel(property.name, viewsContainer, isLocal = false)
        }
    }
    val views get() = viewsContainer.views
    private val channelName = "sound-channel-$name"
    internal var channel: SoundChannel?
        set(value) {
            views.setExtra(channelName, value)
        }
        get() = views.getExtraTyped(channelName)

    fun play(sound: Sound, params: PlaybackParameters = PlaybackParameters.DEFAULT) {
        channel?.stop()
        channel = sound.play(
            when {
                isLocal -> (viewsContainer as CoroutineScope?)?.coroutineContext ?: views.views.coroutineContext
                else -> views.views.coroutineContext
            }, params)
    }

    fun stop() {
        channel?.stop()
        channel = null
    }

    suspend fun fadeIn(time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) {
        channel?.fadeIn(time, easing)
    }

    suspend fun fadeOut(time: TimeSpan = DEFAULT_FADE_TIME, easing: Easing = DEFAULT_FADE_EASING) {
        channel?.fadeOut(time, easing)
    }
}
