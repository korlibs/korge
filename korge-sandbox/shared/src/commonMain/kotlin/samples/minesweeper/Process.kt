package samples.minesweeper

import findCollision
import korlibs.audio.sound.*
import korlibs.datastructure.*
import korlibs.event.*
import korlibs.image.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.korge.render.*
import korlibs.korge.scene.*
import korlibs.korge.time.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.memory.*
import kotlinx.coroutines.*
import kotlin.collections.LinkedHashMap
import kotlin.collections.fill
import kotlin.collections.set
import kotlin.reflect.*

abstract class Process(parent: Container) : Container() {
    //override val stage: Stage get() = super.stage!!
    val processRoot: SContainer get() = parent.findFirstAscendant { it.name == "process.root" } as SContainer
    val scene: ScaledScene get() = processRoot.getExtraTyped<ScaledScene>("scene")!!
    val views: Views by lazy { stage!!.views }
    var fps: Double = 60.0

    val key: KeyV get() = scene.key
    val mouse: MouseV get() = scene.mouseV
    val audio: AudioV get() = scene.audioV
    protected var job: Job? = null

    init {
        parent.addChild(this)
    }

    suspend fun frame() {
        delayFrame()
    }

    override fun renderInternal(ctx: RenderContext) {
        views
        super.renderInternal(ctx)
    }

    fun action(action: KSuspendFunction0<Unit>) {
        throw ChangeActionException(action)
    }

    override fun onStageSet() {
        //println("onStageSet[$this]: job=$job, stage=$stage")

        if (job == null && stage != null) {
            views
            job = scene.launchAsap {
                //println("STARTED COROUTINE!")
                var action = ::main
                while (true) {
                    try {
                        action()
                        break
                    } catch (e: ChangeActionException) {
                        action = e.action
                    }
                }
            }
        }
        else if (job != null && stage == null) {
            //println("removed: $this, $views")
            job?.cancel()
            job = null
            onDestroy()
        } else {
        }
    }

    abstract suspend fun main()

    //init {
    //    this.onAttachDetach(
    //        onAttach = {
    //            //println("added: $views")
    //        },
    //        onDetach = {
    //            //println("removed: $views")
    //            job.cancel()
    //            onDestroy()
    //        }
    //    )
    //}

    fun destroy() {
        removeFromParent()
    }

    protected open fun onDestroy() {
    }

    class ChangeActionException(val action: KSuspendFunction0<Unit>) : Exception()

    inline fun <reified T : View> collision(): T? = views.stage.findCollision(this)
}

class KeyV(val views: ScaledScene) {
    operator fun get(key: Key): Boolean = views.keysPressed[key] == true
}

class MouseV(val scene: ScaledScene) {
    val left: Boolean get() = pressing[0]
    val right: Boolean get() = pressing[1] || pressing[2]
    val pos: PointInt get() = (scene.sceneView.localMousePos(scene.views)).toInt()
    val x: Int get() = pos.x
    val y: Int get() = pos.y
    val pressing = BooleanArray(8)
    val pressed = BooleanArray(8)
    val released = BooleanArray(8)
    val _pressed = BooleanArray(8)
    val _released = BooleanArray(8)
}

class AudioV(val views: ScaledScene) {
    fun play(sound: Sound, repeat: Int = 0) {
        val times = (1 + repeat).playbackTimes
        sound.play(views.coroutineContext, times)
    }
}

val ScaledScene.keysPressed by Extra.Property { LinkedHashMap<Key, Boolean>() }
val ScaledScene.key by Extra.PropertyThis<ScaledScene, KeyV> { KeyV(this) }

val ScaledScene.mouseV by Extra.PropertyThis<ScaledScene, MouseV> { MouseV(this) }
val ScaledScene.audioV by Extra.PropertyThis<ScaledScene, AudioV> { AudioV(this) }

fun ScaledScene.registerProcessSystem() {
    sceneView.onEvents(*MouseEvent.Type.ALL) { e ->
        when (e.type) {
            MouseEvent.Type.MOVE -> Unit
            MouseEvent.Type.DRAG -> Unit
            MouseEvent.Type.UP -> {
                mouseV.pressing[e.button.id] = false
                mouseV._released[e.button.id] = true
            }

            MouseEvent.Type.DOWN -> {
                mouseV.pressing[e.button.id] = true
                mouseV._pressed[e.button.id] = true
            }

            MouseEvent.Type.CLICK -> Unit
            MouseEvent.Type.ENTER -> Unit
            MouseEvent.Type.EXIT -> Unit
            MouseEvent.Type.SCROLL -> Unit
        }
    }
    sceneView.addUpdater {
        arraycopy(mouseV._released, 0, mouseV.released, 0, 8)
        arraycopy(mouseV._pressed, 0, mouseV.pressed, 0, 8)
        mouseV._released.fill(false)
        mouseV._pressed.fill(false)
    }
    sceneView.onEvents(*KeyEvent.Type.ALL) { e ->
        keysPressed[e.key] = e.type == KeyEvent.Type.DOWN
    }
}

suspend fun readImage(path: String) = resourcesVfs["minesweeper/$path"].readBitmapSlice()
suspend fun readSound(path: String) = resourcesVfs["minesweeper/$path"].readSound()
