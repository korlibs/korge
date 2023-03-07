package com.soywiz.korev


open class TypedEvent<T : BEvent>(open override var type: EventType<T>) : Event(), TEvent<T>

open class Event {
    var target: Any? = null
    var _stopPropagation = false
    fun stopPropagation() {
        _stopPropagation = true
    }
}

fun Event.preventDefault(reason: Any? = null): Nothing = throw PreventDefaultException(reason)
fun preventDefault(reason: Any? = null): Nothing = throw PreventDefaultException(reason)

class PreventDefaultException(val reason: Any? = null) : Exception()
