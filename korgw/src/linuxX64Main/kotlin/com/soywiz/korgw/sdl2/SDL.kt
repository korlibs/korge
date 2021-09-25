package com.soywiz.korgw.sdl2

import kotlinx.cinterop.*
import platform.posix.dlopen
import platform.posix.dlsym

internal typealias Uint8 = Byte
internal typealias Uint16 = Short
internal typealias Uint32 = Int
internal typealias Sint16 = Short
internal typealias Sint32 = Int
internal typealias Sint64 = Long
internal typealias SDL_Window = COpaquePointer
internal typealias SDL_GLContext = COpaquePointer
internal typealias SDL_Renderer = COpaquePointer
internal typealias SDL_TouchID = Sint64
internal typealias SDL_GestureID = Sint64
internal typealias SDL_FingerID = Sint64
internal typealias SDL_Keycode = Sint32

internal val SDL_SO by lazy { dlopen("libSDL2.so", platform.posix.RTLD_LAZY) }

internal inline fun <T : Function<*>> sdlFunc(name: String): CPointer<CFunction<T>> =
    dlsym(SDL_SO, name)!!.reinterpret()

internal val SDL_Init by lazy { sdlFunc<(Uint32) -> Int>("SDL_Init") }
internal val SDL_Quit by lazy { sdlFunc<() -> Unit>("SDL_Quit") }

internal val SDL_PollEvent by lazy { sdlFunc<(CValuesRef<*>) -> Int>("SDL_PollEvent") }

internal val SDL_CreateRenderer by lazy { sdlFunc<(SDL_Window, Int, Uint32) -> SDL_Renderer?>("SDL_CreateRenderer") }
internal val SDL_DestroyRenderer by lazy { sdlFunc<(SDL_Renderer) -> Unit>("SDL_DestroyRenderer") }

internal val SDL_CreateWindow by lazy { sdlFunc<(CValues<ByteVar>, Int, Int, Int, Int, Uint32) -> SDL_Window?>("SDL_CreateWindow") }
internal val SDL_DestroyWindow by lazy { sdlFunc<(SDL_Window) -> Unit>("SDL_DestroyWindow") }
internal val SDL_SetWindowTitle by lazy { sdlFunc<(SDL_Window, CValues<ByteVar>) -> Unit>("SDL_SetWindowTitle") }
internal val SDL_SetWindowSize by lazy { sdlFunc<(SDL_Window, Int, Int) -> Unit>("SDL_SetWindowSize") }
internal val SDL_GetDesktopDisplayMode by lazy { sdlFunc<(Int, CValuesRef<*>) -> Int>("SDL_GetDesktopDisplayMode") }
internal val SDL_ShowCursor by lazy { sdlFunc<(Int) -> Int>("SDL_ShowCursor") }

internal val SDL_GL_CreateContext by lazy { sdlFunc<(SDL_Window) -> SDL_GLContext>("SDL_GL_CreateContext") }
internal val SDL_GL_DeleteContext by lazy { sdlFunc<(SDL_GLContext) -> Unit>("SDL_GL_DeleteContext") }
internal val SDL_GL_MakeCurrent by lazy { sdlFunc<(SDL_Window, SDL_GLContext) -> Int>("SDL_GL_MakeCurrent") }
internal val SDL_GL_SetSwapInterval by lazy { sdlFunc<(Int) -> Int>("SDL_GL_SetSwapInterval") }
internal val SDL_GL_SwapWindow by lazy { sdlFunc<(SDL_Window) -> Unit>("SDL_GL_SwapWindow") }

internal val SDL_GetError by lazy { sdlFunc<() -> CPointer<*>>("SDL_GetError") }

class SDL {
    fun init(mode: SDL_INIT): Int =
        SDL_Init(mode.value)

    fun init(mode: Int) =
        SDL_Init(mode)

    fun quit() {
        SDL_Quit()
    }

    fun pollEvent(event: SDL_Event): Int {
        var retval = 0
        memScoped {
            val data = allocArray<ByteVar>(SDL_Event.SIZE)
            retval = SDL_PollEvent(data)
            if (retval > 0) {
                event.write(data)
            }
        }
        return retval
    }

    fun createWindow(title: String, x: Int, y: Int, w: Int, h: Int, flags: Uint32) =
        Window.create(title, x, y, w, h, flags)

    fun getDesktopDisplayMode(index: Int, mode: SDL_DisplayMode): Int {
        var retval = 0
        memScoped {
            val data = allocArray<ByteVar>(SDL_DisplayMode.SIZE)
            retval = SDL_GetDesktopDisplayMode(index, data)
            mode.write(data)
        }
        return retval
    }

    fun getError(): String? =
        SDL_GetError().takeIf { it.rawValue != nativeNullPtr }?.reinterpret<ByteVar>()?.toKString()

    fun showCursor(visible: Boolean) {
        SDL_ShowCursor(if (visible) 1 else 0)
    }

    class Window(val w: SDL_Window) {
        companion object {
            fun create(title: String, x: Int, y: Int, w: Int, h: Int, flags: Uint32): Window? {
                return SDL_CreateWindow(title.cstr, x, y, w, h, flags)?.let { Window(it) }
            }
        }

        fun destroy() {
            SDL_DestroyWindow(w)
        }

        fun setTitle(title: String) {
            SDL_SetWindowTitle(w, title.cstr)
        }

        fun setSize(width: Int, height: Int) {
            SDL_SetWindowSize(w, width, height)
        }

        fun createRenderer(index: Int = -1, flags: Uint32 = 0) = Renderer.create(w, index, flags)

        fun createGLContext() = GLContext(w)
    }

    class Renderer(val r: SDL_Renderer) {
        companion object {
            fun create(w: SDL_Window, index: Int, flags: Uint32) =
                SDL_CreateRenderer(w, index, flags)?.let { Renderer(it) }
        }

        fun destroy() {
            SDL_DestroyRenderer(r)
        }
    }

    class GLContext(val w: SDL_Window) {
        private val ctx = SDL_GL_CreateContext(w)

        fun delete() {
            SDL_GL_DeleteContext(ctx)
        }

        fun makeCurrent() = SDL_GL_MakeCurrent(w, ctx)

        fun setSwapInterval(interval: Int) = SDL_GL_SetSwapInterval(interval)

        fun swapWindow() {
            SDL_GL_SwapWindow(w)
        }
    }
}
