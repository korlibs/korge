package com.soywiz.korgw.sdl2.jna.enums

object SDL_Init {
    const val TIMER = 0x0000001
    const val AUDIO = 0x0000010
    const val VIDEO = 0x0000020
    const val JOYSTICK = 0x0000200
    const val HAPTIC = 0x00001000
    const val GAMECONTROLLER = 0x00002000
    const val EVENTS = 0x00004000
    const val SENSOR = 0x00008000
    const val NOPARACHUTE = 0x00100000
    const val EVERYTHING =
        TIMER or AUDIO or VIDEO or
            EVENTS or
            JOYSTICK or HAPTIC or GAMECONTROLLER or
            SENSOR
}
