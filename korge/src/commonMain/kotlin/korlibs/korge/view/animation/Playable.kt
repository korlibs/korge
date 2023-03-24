package korlibs.korge.view.animation

interface Playable {
    fun play(): Unit
    fun stop(): Unit
    fun rewind(): Unit = Unit
}