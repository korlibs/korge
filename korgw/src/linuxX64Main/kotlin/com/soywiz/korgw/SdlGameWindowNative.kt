package com.soywiz.korgw

import SDL2.*
import SDL2.SDL_WindowEventID.*
import cnames.structs.SDL_Renderer
import cnames.structs.SDL_Window
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlNative
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.Key
import com.soywiz.korev.KeyEvent
import com.soywiz.korev.MouseButton
import com.soywiz.korev.MouseEvent
import com.soywiz.korgw.sdl2.SDLKeyCode
import com.soywiz.korgw.sdl2.SDL_Keycode_Table
import kotlinx.cinterop.*

class SDLAg(window: SdlGameWindowNative, override val gl: KmlGl = KmlGlNative()) : AGOpengl() {
    override val gles: Boolean = true
    override val linux: Boolean = true
    override val nativeComponent: Any = window
}

class SdlGameWindowNative : EventLoopGameWindow() {
    override val ag: SDLAg by lazy { SDLAg(this) }

    var w: CPointer<SDL_Window>? = null
    var r: CPointer<SDL_Renderer>? = null
    var ctx: SDL_GLContext? = null

    override var title: String = "Korgw"
        set(value) {
            field = value
            if (w != null) {
                SDL_SetWindowTitle(w, value)
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
        if (SDL_Init(SDL_INIT_EVERYTHING) != 0) {
            throw RuntimeException("Couldn't initialize SDL")
        }

        memScoped {
            val displayMode = alloc<SDL_DisplayMode>()
            if (SDL_GetDesktopDisplayMode(0, displayMode.ptr) != 0) {
                throw RuntimeException("Couldn't get desktop display mode")
            }

            winX = displayMode.w / 2
            winY = displayMode.h / 2
        }

        w = SDL_CreateWindow(title, winX, winY, width, height, SDL_WINDOW_OPENGL)
            ?: throw RuntimeException("Couldn't create SDL window")

        r = SDL_CreateRenderer(w, -1, SDL_RENDERER_ACCELERATED)
            ?: throw RuntimeException("Couldn't create SDL renderer")

        ctx = SDL_GL_CreateContext(w)
        SDL_GL_MakeCurrent(w, ctx)
        SDL_GL_SetSwapInterval(1)
    }

    override fun doInitRender() {
        SDL_GL_MakeCurrent(w, ctx)
    }

    override fun doSwapBuffers() {
        SDL_GL_SwapWindow(w)
    }

    override fun doHandleEvents() {
        memScoped {
            val event = alloc<SDL_Event>()
            while (SDL_PollEvent(event.ptr) > 0) {
                when (event.type) {
                    SDL_QUIT -> {
                        close()
                    }
                    SDL_WINDOWEVENT -> {
                        when (val windowEventType = SDL_WindowEventID.byValue(event.window.event.toUInt())) {
                            SDL_WINDOWEVENT_EXPOSED -> render(doUpdate = false)
                            SDL_WINDOWEVENT_MOVED,
                            SDL_WINDOWEVENT_RESIZED,
                            SDL_WINDOWEVENT_SIZE_CHANGED -> {
                                if (windowEventType == SDL_WINDOWEVENT_MOVED) {
                                    winX = event.window.data1
                                    winY = event.window.data2
                                } else if (windowEventType == SDL_WINDOWEVENT_RESIZED) {
                                    width = event.window.data1
                                    height = event.window.data2
                                }
                                dispatchReshapeEvent(0, 0, width, height)
                                render(doUpdate = false)
                            }
                            SDL_WINDOWEVENT_CLOSE -> close()
                        }
                    }
                    SDL_KEYDOWN,
                    SDL_KEYUP -> {
                        val evType = when (event.type) {
                            SDL_KEYDOWN -> KeyEvent.Type.DOWN
                            else -> KeyEvent.Type.UP
                        }
                        val keyKey = SDL_Keycode_Table[SDLKeyCode.fromInt(event.key.keysym.sym)] ?: Key.UNKNOWN
                        val keyCode = event.key.keysym.sym
                        dispatchKeyEvent(evType, 0, keyCode.toChar(), keyKey, keyCode)
                    }
                    SDL_MOUSEMOTION -> {
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
                    SDL_MOUSEBUTTONDOWN,
                    SDL_MOUSEBUTTONUP -> {
                        val evType = when (event.type) {
                            SDL_MOUSEBUTTONDOWN -> MouseEvent.Type.DOWN
                            else -> MouseEvent.Type.UP
                        }
                        val btn = when (event.button.button.toInt()) {
                            SDL_BUTTON_LEFT -> MouseButton.LEFT
                            SDL_BUTTON_RIGHT -> MouseButton.RIGHT
                            SDL_BUTTON_MIDDLE -> MouseButton.MIDDLE
                            SDL_BUTTON_X1 -> MouseButton.BUTTON4
                            SDL_BUTTON_X2 -> MouseButton.BUTTON5
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
                    SDL_MOUSEWHEEL -> {
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
                    // TODO: joystick, controller
                }
            }
        }
    }

    override fun doDestroy() {
        SDL_GL_DeleteContext(ctx)
        SDL_DestroyRenderer(r)
        SDL_DestroyWindow(w)
        SDL_Quit()
    }
}
