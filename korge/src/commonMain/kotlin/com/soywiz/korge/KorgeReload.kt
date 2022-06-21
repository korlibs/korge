package com.soywiz.korge

import com.soywiz.korev.Event
import com.soywiz.korev.EventDispatcher
import kotlin.jvm.JvmStatic

object KorgeReload {
    var eventDispatcher: EventDispatcher? = null

    @JvmStatic
    @Suppress("unused") // This is called from [com.soywiz.korge.reloadagent.KorgeReloadAgent]
    fun triggerReload(classes: List<String>) {
        println("KorgeReloadAgent detected a class change. Reload: $classes")
        eventDispatcher?.dispatch(ReloadEvent::class, ReloadEvent(classes))
    }

    fun registerEventDispatcher(eventDispatcher: EventDispatcher) {
        this.eventDispatcher = eventDispatcher
    }
}

data class ReloadEvent(val classes: List<String>) : Event()
