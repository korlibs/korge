import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.checkedIf
import com.soywiz.korag.gl.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.KeyEvent
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korgw.EventLoopGameWindow
import com.soywiz.korgw.platform.INativeGL
import com.soywiz.korgw.platform.NativeKgl
import com.soywiz.korgw.sdl2.SDLKeyCode
import com.soywiz.korgw.sdl2.SDL_Keycode_Table
import com.soywiz.korgw.sdl2.jna.ISDL
import com.soywiz.korgw.sdl2.jna.SDL_GLContext
import com.soywiz.korgw.sdl2.jna.SDL_Renderer
import com.soywiz.korgw.sdl2.jna.SDL_Window
import com.soywiz.korgw.sdl2.jna.enums.*
import com.soywiz.korgw.sdl2.jna.events.SDL_Event
import com.soywiz.korgw.sdl2.jna.structs.SDL_DisplayMode
import com.soywiz.korio.util.OS
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import kotlin.system.*

open class SDLKmlGl : NativeKgl(SDL) {
    override val gles: Boolean = true
    override val linux: Boolean = true
}
class SDLAg(window: SdlGameWindowJvm, val checkGl: Boolean, override val gl: KmlGl = SDLKmlGl().checkedIf(checkGl)) :
    AGOpengl() {
    override val nativeComponent: Any = window
}

interface GL : INativeGL, Library

object SDL :
    ISDL by Native.load(System.getenv("SDL2_PATH") ?: "SDL2", ISDL::class.java),
    GL by Native.load(
        System.getenv("GLLIB_PATH") ?: (when {
            OS.isMac -> "OpenGL"
            else -> "libGL"
        }),
        GL::class.java
    )

const val SDL_SUCCESS = 0
val NULLPTR = Pointer(0)

class SdlGameWindowJvm(checkGl: Boolean) : EventLoopGameWindow() {
    override val ag: AGOpengl by lazy { SDLAg(this, checkGl) }

    override var title: String = "Korgw"
        set(value) {
            field = value
            if (w != NULLPTR) {
                SDL.SDL_SetWindowTitle(w, value)
            }
        }
    override var width: Int = 200; private set
    override var height: Int = 200; private set

    var w: SDL_Window = NULLPTR
    var r: SDL_Renderer = NULLPTR
    var ctx: SDL_GLContext = NULLPTR

    var lastMouseX = 0
    var lastMouseY = 0
    var winX = 0
    var winY = 0

    override fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        dispatchReshapeEvent(0, 0, width, height)
    }

    override fun doInitialize() {
        if (SDL.SDL_Init(SDL_Init.EVERYTHING) != SDL_SUCCESS) {
            error("Couldn't initialize SDL: ${SDL.SDL_GetError()}")
        }

        val displayMode = SDL_DisplayMode.Ref()
        if (SDL.SDL_GetDesktopDisplayMode(0, displayMode) != SDL_SUCCESS) {
            error("Couldn't get desktop display mode: ${SDL.SDL_GetError()}")
        }
        winX = displayMode.w / 2
        winY = displayMode.h / 2

        w = SDL.SDL_CreateWindow(title, winX, winY, width, height, SDL_WindowFlags.OPENGL or SDL_WindowFlags.SHOWN)
            ?: error("Couldn't create SDL window: ${SDL.SDL_GetError()}")

        r = SDL.SDL_CreateRenderer(w, -1, SDL_RendererFlags.ACCELERATED)
            ?: error("Couldn't create SDL renderer: ${SDL.SDL_GetError()}")

        ctx = SDL.SDL_GL_CreateContext(w)
        SDL.SDL_GL_MakeCurrent(w, ctx)
        SDL.SDL_GL_SetSwapInterval(1)
    }

    override fun doInitRender() {
        SDL.SDL_GL_MakeCurrent(w, ctx)
    }

    override fun doSwapBuffers() {
        SDL.SDL_GL_SwapWindow(w)
    }

    override fun doHandleEvents() {
        val event = SDL_Event.Ref()
        SDL.SDL_PollEvent(event)
        event.read()

        when (val eventType = SDL_EventType.fromInt(event.type)) {
            SDL_EventType.QUIT -> close()
            SDL_EventType.WINDOWEVENT -> {
                when (val windowEventType = SDL_WindowEventID.fromInt(event.window.event.toInt())) {
                    SDL_WindowEventID.EXPOSED -> render(doUpdate = false)
                    SDL_WindowEventID.MOVED,
                    SDL_WindowEventID.RESIZED,
                    SDL_WindowEventID.SIZE_CHANGED -> {
                        if (windowEventType == SDL_WindowEventID.MOVED) {
                            winX = event.window.data1
                            winY = event.window.data2
                        } else if (windowEventType == SDL_WindowEventID.RESIZED) {
                            width = event.window.data1
                            height = event.window.data2
                        }
                        dispatchReshapeEvent(0, 0, width, height)
                        render(doUpdate = false)
                    }
                    SDL_WindowEventID.CLOSE -> close()
                    else -> {
                    }
                }
            }
            SDL_EventType.KEYDOWN,
            SDL_EventType.KEYUP -> {
                val evType = when (eventType) {
                    SDL_EventType.KEYDOWN -> KeyEvent.Type.DOWN
                    else -> KeyEvent.Type.UP
                }
                val keyKey = SDL_Keycode_Table[SDLKeyCode.fromInt(event.key.keysym.sym)] ?: Key.UNKNOWN
                val keyCode = event.key.keysym.sym
                dispatchKeyEvent(evType, 0, keyCode.toChar(), keyKey, keyCode)
            }
            SDL_EventType.MOUSEMOTION -> {
                dispatchMouseEvent(
                    MouseEvent.Type.MOVE, 0,
                    event.motion.x, event.motion.y,
                    MouseButton.NONE, 0,
                    0.0, 0.0, 0.0,
                    isShiftDown = false, isCtrlDown = false, isAltDown = false, isMetaDown = false,
                    scaleCoords = false, simulateClickOnUp = false
                )
            }
            SDL_EventType.MOUSEBUTTONDOWN,
            SDL_EventType.MOUSEBUTTONUP -> {
                val evType = when (eventType) {
                    SDL_EventType.MOUSEBUTTONDOWN -> MouseEvent.Type.DOWN
                    else -> MouseEvent.Type.UP
                }
                val button = when (event.button.button.toInt()) {
                    SDL_MouseButton.LEFT -> MouseButton.LEFT
                    SDL_MouseButton.RIGHT -> MouseButton.RIGHT
                    SDL_MouseButton.MIDDLE -> MouseButton.MIDDLE
                    SDL_MouseButton.X1 -> MouseButton.BUTTON4
                    SDL_MouseButton.X2 -> MouseButton.BUTTON5
                    else -> MouseButton.LEFT
                }
                dispatchMouseEvent(
                    evType, 0,
                    event.button.x, event.button.y,
                    button, 0,
                    0.0, 0.0, 0.0,
                    isShiftDown = false, isCtrlDown = false, isAltDown = false, isMetaDown = false,
                    scaleCoords = false, simulateClickOnUp = true
                )
            }
            SDL_EventType.MOUSEWHEEL -> {
                dispatchMouseEvent(
                    MouseEvent.Type.SCROLL, 0,
                    lastMouseX, lastMouseY,
                    MouseButton.BUTTON_WHEEL, 0,
                    event.wheel.x.toDouble(),
                    event.wheel.y.toDouble(),
                    0.0,
                    isShiftDown = false, isCtrlDown = false, isAltDown = false, isMetaDown = false,
                    scaleCoords = false, simulateClickOnUp = true
                )
            }
            SDL_EventType.JOYAXISMOTION -> event.jaxis
            SDL_EventType.JOYBALLMOTION -> event.jball
            SDL_EventType.JOYHATMOTION -> event.jhat
            SDL_EventType.JOYBUTTONDOWN,
            SDL_EventType.JOYBUTTONUP -> event.jbutton
            SDL_EventType.JOYDEVICEADDED,
            SDL_EventType.JOYDEVICEREMOVED -> event.jdevice
            SDL_EventType.CONTROLLERAXISMOTION -> event.caxis
            SDL_EventType.CONTROLLERBUTTONDOWN,
            SDL_EventType.CONTROLLERBUTTONUP -> event.cbutton
            SDL_EventType.CONTROLLERDEVICEADDED,
            SDL_EventType.CONTROLLERDEVICEREMOVED,
            SDL_EventType.CONTROLLERDEVICEREMAPPED -> event.cdevice
            SDL_EventType.CONTROLLERTOUCHPADDOWN,
            SDL_EventType.CONTROLLERTOUCHPADMOTION,
            SDL_EventType.CONTROLLERTOUCHPADUP -> event.ctouchpad
            SDL_EventType.CONTROLLERSENSORUPDATE -> event.csensor
            SDL_EventType.FINGERDOWN,
            SDL_EventType.FINGERUP,
            SDL_EventType.FINGERMOTION -> event.tfinger
            SDL_EventType.DOLLARGESTURE,
            SDL_EventType.DOLLARRECORD -> event.dgesture
            SDL_EventType.MULTIGESTURE -> event.mgesture
            SDL_EventType.DROPCOMPLETE -> event.drop
            SDL_EventType.AUDIODEVICEADDED,
            SDL_EventType.AUDIODEVICEREMOVED -> event.adevice
            SDL_EventType.SENSORUPDATE -> event.sensor
            else -> {
                if (event.type >= SDL_EventType.USEREVENT.value) {
                    event.user
                } else {
                    event.common
                }
            }
        }
    }

    override fun doDestroy() {
        SDL.SDL_GL_DeleteContext(ctx)
        SDL.SDL_DestroyRenderer(r)
        SDL.SDL_DestroyWindow(w)
        SDL.SDL_Quit()
        if (exitProcessOnExit) {
            exitProcess(this.exitCode)
        }
    }
}
