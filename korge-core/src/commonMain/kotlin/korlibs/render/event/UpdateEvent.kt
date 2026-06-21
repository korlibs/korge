package korlibs.render.event

import korlibs.event.*
import korlibs.time.*
import kotlin.time.*

class UpdateEvent(var fastDeltaTime: FastDuration = FastDuration.ZERO) : Event(), TEvent<UpdateEvent> {
    constructor(deltaTime: Duration) : this(deltaTime.fast)
    var deltaTime: Duration
        set(value) { fastDeltaTime = value.fast }
        get() = fastDeltaTime.toDuration()

    companion object : EventType<UpdateEvent>
    override val type: EventType<UpdateEvent> get() = UpdateEvent

    fun copyFrom(other: UpdateEvent) {
        this.fastDeltaTime = other.fastDeltaTime
    }

    override fun toString(): String = "UpdateEvent(time=$fastDeltaTime)"
}
