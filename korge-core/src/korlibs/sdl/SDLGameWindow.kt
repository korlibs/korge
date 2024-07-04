package korlibs.sdl

import korlibs.ffi.*
import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.graphics.log.*
import korlibs.kgl.*
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
    val glContext = sdl.SDL_GL_CreateContext(window)

    init {
        sdl.SDL_ShowWindow(window)
    }

    override val ag: AGOpengl = AGOpengl(object : KmlGlContext(window, KmlGlOpenGL()) {
        override fun set() { sdl.SDL_GL_MakeCurrent(window as FFIPointer, glContext) }
        override fun unset() { sdl.SDL_GL_MakeCurrent(window as FFIPointer, null) }
        override fun swap() { sdl.SDL_GL_SwapWindow(window as FFIPointer) }
        override fun close() { sdl.SDL_GL_DeleteContext(glContext) }
    })
    val gl: KmlGl = ag.gl

    override var title: String = ""
        set(value) {
            field = value
            sdl.SDL_SetWindowTitle(window, title)
        }

    override fun setSize(width: Int, height: Int) {
        sdl.SDL_SetWindowSize(window, width, height)
        sdl.SDL_SetWindowPosition(window, SDL.SDL_WINDOWPOS_CENTERED, SDL.SDL_WINDOWPOS_CENTERED)
    }

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

        sdl.SDL_GL_MakeCurrent(window, glContext)
        //gl.viewport(0, 0, 200, 200);
        gl.clearColor(1f, 0f, 0f, 1f)
        gl.clear(OpenGL.GL_COLOR_BUFFER_BIT)
        //gl.flush()
        //sdl.SDL_GL_SwapWindow(window)
        //sdl.SDL_GL_MakeCurrent(window, null)
    }

    fun afterFrame() {
        sdl.SDL_GL_SwapWindow(window)
    }

    override fun close(exitCode: Int) {
        super.close(exitCode)
        sdl.SDL_GL_DeleteContext(glContext)
        sdl.SDL_Quit()
    }
}
