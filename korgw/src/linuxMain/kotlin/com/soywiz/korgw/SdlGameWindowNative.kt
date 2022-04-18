package com.soywiz.korgw

import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlNative
import com.soywiz.korag.gl.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.KeyEvent
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korgw.sdl2.*

private val sdl by lazy { SDL() }

class SDLAg(window: SdlGameWindowNative, override val gl: KmlGl = KmlGlNative()) : AGOpengl() {
    override val nativeComponent: Any = window
}

class SdlGameWindowNative : EventLoopGameWindow() {
    override val ag: SDLAg by lazy { SDLAg(this) }

    var w: SDL.Window? = null
    var r: SDL.Renderer? = null
    var ctx: SDL.GLContext? = null

    override var title: String = "Korgw"
        set(value) {
            field = value
            if (w != null) {
                w?.setTitle(value)
            }
        }

    override var width: Int = 200; private set
    override var height: Int = 200; private set

    var winX = 0
    var winY = 0
    var lastMouseX = 0
    var lastMouseY = 0

    override fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        dispatchReshapeEvent(0, 0, width, height)
    }

    override fun doInitialize() {
        if (sdl.init(SDL_INIT.EVERYTHING) != 0) {
            throw RuntimeException("Couldn't initialize SDL")
        }

        sdl.showCursor(false)

        val displayMode = SDL_DisplayMode()
        if (sdl.getDesktopDisplayMode(0, displayMode) != 0) {
            throw RuntimeException("Couldn't get desktop display mode")
        }

        winX = displayMode.w / 2
        winY = displayMode.h / 2

        w = sdl.createWindow(title, winX, winY, width, height, SDL_WindowFlags.OPENGL)
            ?: throw RuntimeException("Couldn't create SDL window")

        r = w?.createRenderer(flags = SDL_RendererFlags.ACCELERATED)
            ?: throw RuntimeException("Couldn't create SDL renderer")

        ctx = w?.createGLContext()
        ctx?.makeCurrent()
        ctx?.setSwapInterval(1)
    }

    override fun doInitRender() {
        ctx?.makeCurrent()
    }

    override fun doSwapBuffers() {
        ctx?.swapWindow()
    }

    override fun doHandleEvents() {
        val event = SDL_Event()
        while (sdl.pollEvent(event) > 0) {
            when (SDL_EventType.fromInt(event.type)) {
                SDL_EventType.QUIT -> {
                    close()
                }
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
                    }
                }
                SDL_EventType.KEYDOWN,
                SDL_EventType.KEYUP -> {
                    val evType = when (event.type) {
                        SDL_EventType.KEYDOWN.value -> KeyEvent.Type.DOWN
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
                    lastMouseX = event.motion.x
                    lastMouseY = event.motion.y
                }
                SDL_EventType.MOUSEBUTTONDOWN,
                SDL_EventType.MOUSEBUTTONUP -> {
                    val evType = when (event.type) {
                        SDL_EventType.MOUSEBUTTONDOWN.value -> MouseEvent.Type.DOWN
                        else -> MouseEvent.Type.UP
                    }
                    val btn = when (event.button.button.toInt()) {
                        SDL_MouseButton.LEFT -> MouseButton.LEFT
                        SDL_MouseButton.RIGHT -> MouseButton.RIGHT
                        SDL_MouseButton.MIDDLE -> MouseButton.MIDDLE
                        SDL_MouseButton.X1 -> MouseButton.BUTTON4
                        SDL_MouseButton.X2 -> MouseButton.BUTTON5
                        else -> MouseButton.BUTTON_UNKNOWN
                    }
                    dispatchMouseEvent(
                        evType, 0,
                        event.button.x, event.button.y,
                        btn, 0,
                        0.0, 0.0, 0.0,
                        isShiftDown = false, isCtrlDown = false, isAltDown = false, isMetaDown = false,
                        scaleCoords = false, simulateClickOnUp = true
                    )
                    lastMouseX = event.button.x
                    lastMouseY = event.button.y
                }
                SDL_EventType.MOUSEWHEEL -> {
                    dispatchMouseEvent(
                        MouseEvent.Type.SCROLL, 0,
                        lastMouseX, lastMouseY,
                        MouseButton.BUTTON_WHEEL, 0,
                        event.wheel.x.toDouble(), event.wheel.y.toDouble(),
                        0.0,
                        isShiftDown = false, isCtrlDown = false, isAltDown = false, isMetaDown = false,
                        scaleCoords = false, simulateClickOnUp = true
                    )
                }
                SDL_EventType.JOYBUTTONDOWN,
                SDL_EventType.JOYBUTTONUP -> {

                }
                // TODO: joystick, controller
            }
        }
    }

    override fun doDestroy() {
        ctx?.delete()
        r?.destroy()
        w?.destroy()
        sdl.quit()
    }
}
