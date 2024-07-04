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

        // https://wiki.libsdl.org/SDL2/SDL_WindowFlags
        // SDL_WindowFlags
        const val SDL_WINDOW_OPENGL = 0x2
        const val SDL_WINDOW_RESIZABLE = 0x20

        // https://wiki.libsdl.org/SDL2/SDL_Event
        // SDL_Event
        const val SDL_QUIT = 0x100
    }

    val SDL_Init by func<(flags: Int) -> Int>()
    val SDL_CreateWindow by func<(title: String, x: Int, y: Int, w: Int, h: Int, flags: Int) -> FFIPointer>()
    val SDL_SetWindowTitle by func<(window: FFIPointer, title: String) -> Unit>()
    val SDL_SetWindowSize by func<(window: FFIPointer, w: Int, h: Int) -> Unit>()
    val SDL_ShowWindow by func<(window: FFIPointer) -> Unit>()
    val SDL_RaiseWindow by func<(window: FFIPointer) -> Unit>()
    val SDL_PollEvent by func<(event: FFIPointer) -> Boolean>()
    val SDL_Quit by func<() -> Unit>()
}
