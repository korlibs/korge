package com.soywiz.korge

import com.soywiz.korev.Event
import com.soywiz.korev.EventDispatcher
import kotlin.jvm.JvmStatic
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
private var KorgeReload_eventDispatcher: EventDispatcher? = null

object KorgeReload {
    @JvmStatic
    @Suppress("unused") // This is called from [com.soywiz.korge.reloadagent.KorgeReloadAgent]
    fun triggerReload(classes: List<String>) {
        println("KorgeReloadAgent detected a class change. Reload: $classes")
        KorgeReload_eventDispatcher?.dispatch(ReloadEvent::class, ReloadEvent(classes))
    }

    fun registerEventDispatcher(eventDispatcher: EventDispatcher) {
        KorgeReload_eventDispatcher = eventDispatcher
    }
}

data class ReloadEvent(val classes: List<String>) : Event()
