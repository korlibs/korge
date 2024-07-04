package korlibs.sdl

import korlibs.concurrent.thread.*
import korlibs.ffi.*
import korlibs.io.async.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlin.test.*

class SDLTest {
    @Ignore
    @Test
    fun test() = suspendTest {
        val sdl = SDL()
        try {
            sdl.SDL_Init(0)
            val window = sdl.SDL_CreateWindow("hello world", SDL.SDL_WINDOWPOS_CENTERED, SDL.SDL_WINDOWPOS_CENTERED, 300, 300, 2)
            sdl.SDL_ShowWindow(window)
            sdl.SDL_RaiseWindow(window)

            val event = CreateFFIMemory(1024)
            for (n in 0 until 1000) {
                event.usePointer {
                    while (sdl.SDL_PollEvent(it)) {
                        println(it.getS32(0))
                    }
                    //NativeThread.sleep(1.milliseconds)
                    delay(1.milliseconds)
                }
            }
            sdl.SDL_Quit()
        } finally {
            //sdl.close() // @TODO: enable after korlibs -alpha8
        }
    }
}
