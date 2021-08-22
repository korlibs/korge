@file:Suppress("ClassName")

package com.soywiz.korgw.sdl2.jna.enums

object SDL_WindowFlags {
    const val FULLSCREEN = 0x00000001
    const val OPENGL = 0x00000002
    const val SHOWN = 0x00000004
    const val HIDDEN = 0x00000008
    const val BORDERLESS = 0x00000010
    const val RESIZABLE = 0x00000020
    const val MINIMIZED = 0x00000040
    const val MAXIMIZED = 0x00000080
    const val INPUT_GRABBED = 0x00000100
    const val INPUT_FOCUS = 0x00000200
    const val MOUSE_FOCUS = 0x00000400
    const val FULLSCREEN_DESKTOP = FULLSCREEN or 0x00001000
    const val FOREIGN = 0x00000800
    const val ALLOW_HIGHDPI = 0x00002000
    const val MOUSE_CAPTURE = 0x00004000
    const val ALWAYS_ON_TOP = 0x00008000
    const val SKIP_TASKBAR = 0x00010000
    const val UTILITY = 0x00020000
    const val TOOLTIP = 0x00040000
    const val POPUP_MENU = 0x00080000
    const val VULKAN = 0x10000000
    const val METAL = 0x20000000
}
