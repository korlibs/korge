package com.soywiz.korgw.sdl2.jna

import com.soywiz.korgw.sdl2.jna.events.SDL_Event
import com.soywiz.korgw.sdl2.jna.structs.SDL_DisplayMode
import com.sun.jna.Library
import com.sun.jna.Pointer

typealias Uint8 = Byte
typealias Uint16 = Short
typealias Uint32 = Int
typealias Sint16 = Int
typealias Sint32 = Int
typealias Sint64 = Long
typealias SDL_Window = Pointer
typealias SDL_GLContext = Pointer
typealias SDL_Renderer = Pointer
typealias SDL_TouchID = Sint64
typealias SDL_GestureID = Sint64
typealias SDL_FingerID = Sint64
typealias SDL_Keycode = Sint32

interface ISDL : Library {
    fun SDL_Init(flags: Uint32): Int
    fun SDL_Quit()

    // Events
    fun SDL_PollEvent(event: SDL_Event.Ref?): Int

    // Renderer
    fun SDL_CreateRenderer(window: SDL_Window, index: Int, flags: Uint32): SDL_Renderer?
    fun SDL_DestroyRenderer(renderer: SDL_Renderer)

    // Video
    fun SDL_CreateWindow(title: String, x: Int, y: Int, w: Int, h: Int, flags: Uint32): SDL_Window?
    fun SDL_DestroyWindow(window: SDL_Window)
    fun SDL_SetWindowTitle(window: SDL_Window, title: String)
    fun SDL_SetWindowSize(window: SDL_Window, w: Int, h: Int)
    fun SDL_GL_CreateContext(window: SDL_Window): SDL_GLContext
    fun SDL_GL_DeleteContext(context: SDL_GLContext)
    fun SDL_GL_MakeCurrent(window: SDL_Window, context: SDL_GLContext): Int
    fun SDL_GL_SetSwapInterval(interval: Int): Int
    fun SDL_GL_SwapWindow(window: SDL_Window)
    fun SDL_GetDesktopDisplayMode(displayIndex: Int, mode: SDL_DisplayMode.Ref): Int

    // Error
    fun SDL_GetError(): String?
}
