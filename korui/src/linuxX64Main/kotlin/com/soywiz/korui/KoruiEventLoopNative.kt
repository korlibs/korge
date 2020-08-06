package com.soywiz.korui

import GL.*
import com.soywiz.kds.PriorityQueue
import com.soywiz.kds.Queue
import com.soywiz.klock.DateTime
import com.soywiz.klock.milliseconds
import com.soywiz.korag.AG
import com.soywiz.korag.AGConfig
import com.soywiz.korag.AGOpenglFactory
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.DummyCloseable
import com.soywiz.korev.Event
import com.soywiz.korev.EventDispatcher
import com.soywiz.korev.Key
import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.light.LightType
import com.soywiz.korui.light.ag
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.reflect.KClass

@UseExperimental(InternalCoroutinesApi::class)
class MyNativeCoroutineDispatcher() : CoroutineDispatcher(), Delay, Closeable {
    override fun dispatchYield(context: CoroutineContext, block: Runnable): Unit = dispatch(context, block)

    class TimedTask(val ms: DateTime, val continuation: CancellableContinuation<Unit>)

    val tasks = Queue<Runnable>()
    val timedTasks = PriorityQueue<TimedTask>(Comparator<TimedTask> { a, b -> a.ms.compareTo(b.ms) })

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        tasks.enqueue(block)
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>): Unit {
        val task = TimedTask(DateTime.now() + timeMillis.milliseconds, continuation)
        continuation.invokeOnCancellation {
            timedTasks.remove(task)
        }
        timedTasks.add(task)
    }

    fun executeStep() {
        val now = DateTime.now()
        while (timedTasks.isNotEmpty() && now >= timedTasks.head.ms) {
            timedTasks.removeHead().continuation.resume(Unit)
        }

        while (tasks.isNotEmpty()) {
            val task = tasks.dequeue()
            task.run()
        }
    }

    override fun close() {

    }

    override fun toString(): String = "MyNativeCoroutineDispatcher"
}

@ThreadLocal
val myNativeCoroutineDispatcher: MyNativeCoroutineDispatcher = MyNativeCoroutineDispatcher()

actual val KoruiDispatcher: CoroutineDispatcher get() = myNativeCoroutineDispatcher

class NativeKoruiContext(
    val ag: AG,
    val light: LightComponents
    //, val app: NSApplication
) : KoruiContext()

class NativeLightComponents(val nkcAg: AG) : LightComponents() {
    val frameHandle = Any()

    override fun create(type: LightType, config: Any?): LightComponentInfo {
        @Suppress("REDUNDANT_ELSE_IN_WHEN")
        val handle: Any = when (type) {
            LightType.FRAME -> frameHandle
            LightType.CONTAINER -> Any()
            LightType.BUTTON -> Any()
            LightType.IMAGE -> Any()
            LightType.PROGRESS -> Any()
            LightType.LABEL -> Any()
            LightType.TEXT_FIELD -> Any()
            LightType.TEXT_AREA -> Any()
            LightType.CHECK_BOX -> Any()
            LightType.SCROLL_PANE -> Any()
            LightType.AGCANVAS -> nkcAg.nativeComponent
            else -> throw UnsupportedOperationException("Type: $type")
        }
        return LightComponentInfo(handle).apply {
            this.ag = nkcAg
        }
    }

    val eds = arrayListOf<Pair<KClass<*>, EventDispatcher>>()

    fun <T : Event> dispatch(clazz: KClass<T>, e: T) {
        for ((eclazz, ed) in eds) {
            if (eclazz == clazz) {
                ed.dispatch(clazz, e)
            }
        }
    }

    inline fun <reified T : Event> dispatch(e: T) = dispatch(T::class, e)

    override fun <T : Event> registerEventKind(c: Any, clazz: KClass<T>, ed: EventDispatcher): Closeable {
        val pair = Pair(clazz, ed)

        if (c === frameHandle || c === nkcAg.nativeComponent) {
            eds += pair
            return Closeable { eds -= pair }
        }

        return DummyCloseable
    }

    override suspend fun dialogOpenFile(c: Any, filter: String): VfsFile {
        TODO()
    }
}

data class WindowConfig(
    val width: Int = 640,
    val height: Int = 480,
    val title: String = "Korui"
)

@ThreadLocal
val agNativeComponent = Any()

@ThreadLocal
val ag: AG = AGOpenglFactory.create(agNativeComponent).create(agNativeComponent, AGConfig())

@ThreadLocal
val light = NativeLightComponents(ag)

@ThreadLocal
val ctx = NativeKoruiContext(ag, light)

@ThreadLocal
val windowConfig = WindowConfig()

fun glutDisplay() {
    myNativeCoroutineDispatcher.executeStep()
    ag.onRender(ag)
    glutSwapBuffers()
}

@ThreadLocal
val reshapeEvent = com.soywiz.korev.ReshapeEvent()

fun glutReshape(width: Int, height: Int) {
    ag.resized(width, height)
    light.dispatch(reshapeEvent.apply {
        this.width = width
        this.height = height
    })
    glutDisplay()
}

val mevent = com.soywiz.korev.MouseEvent()

private fun mouseEvent(etype: com.soywiz.korev.MouseEvent.Type, ex: Int, ey: Int, ebutton: Int) {
    light.dispatch(mevent.apply {
        this.type = etype
        this.x = ex
        this.y = ey
        this.buttons = 1 shl ebutton
        this.isAltDown = false
        this.isCtrlDown = false
        this.isShiftDown = false
        this.isMetaDown = false
        //this.scaleCoords = false
    })
}

fun glutMouseMove(x: Int, y: Int) {
    mouseEvent(com.soywiz.korev.MouseEvent.Type.MOVE, x, y, 0)
}

fun glutMouse(button: Int, state: Int, x: Int, y: Int) {
    val up = state == GLUT_UP
    val event = if (up) {
        com.soywiz.korev.MouseEvent.Type.UP
    } else {
        com.soywiz.korev.MouseEvent.Type.DOWN
    }
    mouseEvent(event, x, y, button)
    if (up) {
        mouseEvent(com.soywiz.korev.MouseEvent.Type.CLICK, x, y, button)
    }
}

private val keyEvent = com.soywiz.korev.KeyEvent()

val CharToKeys = mapOf(
    'a' to Key.A, 'A' to Key.A,
    'b' to Key.B, 'B' to Key.B,
    'c' to Key.C, 'C' to Key.C,
    'd' to Key.D, 'D' to Key.D,
    'e' to Key.E, 'E' to Key.E,
    'f' to Key.F, 'F' to Key.F,
    'g' to Key.G, 'G' to Key.G,
    'h' to Key.H, 'H' to Key.H,
    'i' to Key.I, 'I' to Key.I,
    'j' to Key.J, 'J' to Key.J,
    'k' to Key.K, 'K' to Key.K,
    'l' to Key.L, 'L' to Key.L,
    'm' to Key.M, 'M' to Key.M,
    'n' to Key.N, 'N' to Key.N,
    'o' to Key.O, 'O' to Key.O,
    'p' to Key.P, 'P' to Key.P,
    'q' to Key.Q, 'Q' to Key.Q,
    'r' to Key.R, 'R' to Key.R,
    's' to Key.S, 'S' to Key.S,
    't' to Key.T, 'T' to Key.T,
    'u' to Key.U, 'U' to Key.U,
    'v' to Key.V, 'V' to Key.V,
    'w' to Key.W, 'W' to Key.W,
    'x' to Key.X, 'X' to Key.X,
    'y' to Key.Y, 'Y' to Key.Y,
    'z' to Key.Z, 'Z' to Key.Z,
    '0' to Key.N0, '1' to Key.N1, '2' to Key.N2, '3' to Key.N3, '4' to Key.N4,
    '5' to Key.N5, '6' to Key.N6, '7' to Key.N7, '8' to Key.N8, '9' to Key.N9
)

private val KeyCodesToKeys = mapOf(
    GLUT_KEY_LEFT to Key.LEFT,
    GLUT_KEY_RIGHT to Key.RIGHT,
    GLUT_KEY_UP to Key.UP,
    GLUT_KEY_DOWN to Key.DOWN,
    32 to Key.ENTER,
    27 to Key.ESCAPE
)

fun glutKeyUpDown(key: UByte, pressed: Boolean) {
    GLUT_KEY_LEFT
    val key = KeyCodesToKeys[key.toInt()] ?: CharToKeys[key.toInt().toChar()] ?: Key.UNKNOWN
    //println("keyDownUp: char=$char, modifiers=$modifiers, keyCode=${keyCode.toInt()}, key=$key, pressed=$pressed")
    light.dispatch(keyEvent.apply {
        this.type =
            if (pressed) com.soywiz.korev.KeyEvent.Type.DOWN else com.soywiz.korev.KeyEvent.Type.UP
        this.id = 0
        this.key = key
        this.keyCode = keyCode
        this.character = char
    })
}

fun glutKeyDown(key: UByte, x: Int, y: Int) {
    glutKeyUpDown(key, true)
}

fun glutKeyUp(key: UByte, x: Int, y: Int) {
    glutKeyUpDown(key, false)
}

internal actual suspend fun KoruiWrap(entry: suspend (KoruiContext) -> Unit) {
    memScoped {
        val argc = alloc<IntVar>().apply { value = 0 }
        glutInit(argc.ptr, null) // TODO: pass real args
    }

    glutInitDisplayMode((GLUT_RGB or GLUT_DOUBLE or GLUT_DEPTH).convert())
    glutInitWindowSize(windowConfig.width, windowConfig.height)
    glutCreateWindow(windowConfig.title)

    glutReshapeFunc(staticCFunction(::glutReshape))
    glutDisplayFunc(staticCFunction(::glutDisplay))
    glutIdleFunc(staticCFunction(::glutDisplay))
    glutMotionFunc(staticCFunction(::glutMouseMove))
    glutPassiveMotionFunc(staticCFunction(::glutMouseMove))
    glutMouseFunc(staticCFunction(::glutMouse))
    glutKeyboardFunc(staticCFunction(::glutKeyDown))
    glutKeyboardFunc(staticCFunction(::glutKeyUp))

    ag.__ready()
    var running = true
    CoroutineScope(coroutineContext).launch(KoruiDispatcher) {
        try {
            entry(ctx)
        } catch (e: Throwable) {
            println(e)
            running = false
        }
    }

    glutMainLoop()
}
