package korlibs.sdl

import korlibs.ffi.*
import korlibs.graphics.*
import korlibs.graphics.log.*
import korlibs.math.geom.*
import korlibs.render.*

abstract class SDLGameWindow(
    var size: Size = Size(640, 480),
    val config: GameWindowCreationConfig = GameWindowCreationConfig.DEFAULT,
) : GameWindow() {
    val sdl: SDL = SDL()
    val window = sdl.SDL_CreateWindow(
        config.title,
        SDL.SDL_WINDOWPOS_CENTERED, SDL.SDL_WINDOWPOS_CENTERED,
        size.width.toInt(), size.height.toInt(),
        SDL.SDL_WINDOW_RESIZABLE or SDL.SDL_WINDOW_OPENGL
    )
    init {
        sdl.SDL_ShowWindow(window)
    }

    override val ag: AG = AGDummy()

    //override var title: String = ""
    //    set(value) {
    //        field = value
    //        sdl.SDL_SetWindowTitle(window, title)
    //    }
//
    //override fun setSize(width: Int, height: Int) {
    //    sdl.SDL_SetWindowSize(window, width, height)
    //}

    fun init() {
        //SDL.SDL_WINDOWPOS_CENTERED
    }

    val event = CreateFFIMemory(1024)

    fun updateSDLEvents() {
        event.usePointer {
            while (sdl.SDL_PollEvent(it)) {
                val type = it.getS32(0)
                when (type) {
                    SDL.SDL_QUIT -> {
                        close()
                    }
                }
                println("EVENT: type=$type")
            }
        }
    }

    override fun close(exitCode: Int) {
        super.close(exitCode)
        sdl.SDL_Quit()
    }
}
