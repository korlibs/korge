package korlibs.sdl

import korlibs.annotations.*
import korlibs.ffi.*

val SDLPath by lazy {
    //"https://github.com/libsdl-org/SDL/releases/download/release-2.30.5/SDL2-2.30.5-win32-x64.zip"
    //"SDL"
    "C:\\temp\\SDL2.dll"
}

@KeepNames
open class SDL : FFILib(SDLPath) {
    companion object {
        const val SDL_WINDOWPOS_UNDEFINED = 0x1FFF0000
        const val SDL_WINDOWPOS_CENTERED = 0x2FFF0000
    }

    val SDL_Init: (flags: Int) -> Int by func()
    val SDL_CreateWindow: (title: String, x: Int, y: Int, w: Int, h: Int, flags: Int) -> FFIPointer by func()
    val SDL_ShowWindow: (window: FFIPointer) -> Unit by func()
    val SDL_RaiseWindow: (window: FFIPointer) -> Unit by func()
    val SDL_PollEvent: (event: FFIPointer) -> Boolean by func()
    val SDL_Quit: () -> Unit by func()
}
