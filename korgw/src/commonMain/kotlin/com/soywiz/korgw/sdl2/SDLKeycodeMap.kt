package com.soywiz.korgw.sdl2

import com.soywiz.korev.Key
import com.soywiz.korgw.sdl2.SDLKeyCode.*

val SDL_Keycode_Table: Map<SDLKeyCode, Key> = mapOf(
    UNKNOWN to Key.UNKNOWN,

    RETURN to Key.RETURN,
    ESCAPE to Key.ESCAPE,
    BACKSPACE to Key.BACKSPACE,
    TAB to Key.TAB,
    SPACE to Key.SPACE,
//    EXCLAIM
    QUOTEDBL to Key.QUOTE,
    HASH to Key.POUND,
//    PERCENT
//    DOLLAR
//    AMPERSAND
    QUOTE to Key.APOSTROPHE,
    LEFTPAREN to Key.KP_LEFT_PAREN,
    RIGHTPAREN to Key.KP_RIGHT_PAREN,
    ASTERISK to Key.STAR,
    PLUS to Key.PLUS,
    COMMA to Key.COMMA,
    MINUS to Key.MINUS,
    PERIOD to Key.PERIOD,
    SLASH to Key.SLASH,
    ZERO to Key.N0,
    ONE to Key.N1,
    TWO to Key.N2,
    THREE to Key.N3,
    FOUR to Key.N4,
    FIVE to Key.N5,
    SIX to Key.N6,
    SEVEN to Key.N7,
    EIGHT to Key.N8,
    NINE to Key.N9,
//    COLON
    SEMICOLON to Key.SEMICOLON,
//    LESS
    EQUALS to Key.EQUAL,
//    GREATER
//    QUESTION
    AT to Key.AT,

    LEFTBRACKET to Key.LEFT_BRACKET,
    BACKSLASH to Key.BACKSLASH,
    RIGHTBRACKET to Key.RIGHT_BRACKET,
//    CARET
    UNDERSCORE to Key.UNDERLINE,
    BACKQUOTE to Key.BACKQUOTE,
    a to Key.A,
    b to Key.B,
    c to Key.C,
    d to Key.D,
    e to Key.E,
    f to Key.F,
    g to Key.G,
    h to Key.H,
    i to Key.I,
    j to Key.J,
    k to Key.K,
    l to Key.L,
    m to Key.M,
    n to Key.N,
    o to Key.O,
    p to Key.P,
    q to Key.Q,
    r to Key.R,
    s to Key.S,
    t to Key.T,
    u to Key.U,
    v to Key.V,
    w to Key.W,
    x to Key.X,
    y to Key.Y,
    z to Key.Z,

    CAPSLOCK to Key.CAPS_LOCK,

    F1 to Key.F1,
    F2 to Key.F2,
    F3 to Key.F3,
    F4 to Key.F4,
    F5 to Key.F5,
    F6 to Key.F6,
    F7 to Key.F7,
    F8 to Key.F8,
    F9 to Key.F9,
    F10 to Key.F10,
    F11 to Key.F11,
    F12 to Key.F12,

    PRINTSCREEN to Key.PRINT_SCREEN,
    SCROLLLOCK to Key.SCROLL_LOCK,
    PAUSE to Key.PAUSE,
    INSERT to Key.INSERT,
    HOME to Key.HOME,
    PAGEUP to Key.PAGE_UP,
    DELETE to Key.DELETE,
    END to Key.END,
    PAGEDOWN to Key.PAGE_DOWN,
    RIGHT to Key.RIGHT,
    LEFT to Key.LEFT,
    DOWN to Key.DOWN,
    UP to Key.UP,

    NUMLOCKCLEAR to Key.NUM_LOCK,
    KP_DIVIDE to Key.KP_DIVIDE,
    KP_MULTIPLY to Key.KP_MULTIPLY,
    KP_MINUS to Key.MINUS,
    KP_PLUS to Key.PLUS,
    KP_ENTER to Key.KP_ENTER,
    KP_1 to Key.KP_1,
    KP_2 to Key.KP_2,
    KP_3 to Key.KP_3,
    KP_4 to Key.KP_4,
    KP_5 to Key.KP_5,
    KP_6 to Key.KP_6,
    KP_7 to Key.KP_7,
    KP_8 to Key.KP_8,
    KP_9 to Key.KP_9,
    KP_0 to Key.KP_0,
    KP_PERIOD to Key.PERIOD,

    APPLICATION to Key.APPS,
    POWER to Key.POWER,
    KP_EQUALS to Key.KP_EQUAL,
    F13 to Key.F13,
    F14 to Key.F14,
    F15 to Key.F15,
    F16 to Key.F16,
    F17 to Key.F17,
    F18 to Key.F18,
    F19 to Key.F19,
    F20 to Key.F20,
    F21 to Key.F21,
    F22 to Key.F22,
    F23 to Key.F23,
    F24 to Key.F24,
    EXECUTE to Key.EXECUTE,
    HELP to Key.HELP,
    MENU to Key.MENU,
    SELECT to Key.SELECT_KEY,
    STOP to Key.MEDIA_STOP,
//    AGAIN
//    UNDO
    CUT to Key.CUT,
    COPY to Key.COPY,
    PASTE to Key.PASTE,
//    FIND
    MUTE to Key.MUTE,
    VOLUMEUP to Key.VOLUME_UP,
    VOLUMEDOWN to Key.VOLUME_DOWN,
    KP_COMMA to Key.KP_COMMA,
    KP_EQUALSAS400 to Key.KP_EQUAL,

//    ALTERASE
    SYSREQ to Key.SYSRQ,
    CANCEL to Key.CANCEL,
    CLEAR to Key.CLEAR,
    PRIOR to Key.PRIOR,
    RETURN2 to Key.RETURN,
    SEPARATOR to Key.KP_SEPARATOR,
//    OUT
//    OPER
//    CLEARAGAIN
    CRSEL to Key.CRSEL,
    EXSEL to Key.EXSEL,

    KP_00 to Key.KP_0,
    KP_000 to Key.KP_0,
//    THOUSANDSSEPARATOR
//    DECIMALSEPARATOR
//    CURRENCYUNIT
//    CURRENCYSUBUNIT
    KP_LEFTPAREN to Key.KP_LEFT_PAREN,
    KP_RIGHTPAREN to Key.KP_RIGHT_PAREN,
    KP_LEFTBRACE to Key.LEFT_BRACKET,
    KP_RIGHTBRACE to Key.RIGHT_BRACKET,
    KP_TAB to Key.TAB,
    KP_BACKSPACE to Key.BACKSPACE,
    KP_A to Key.A,
    KP_B to Key.B,
    KP_C to Key.C,
    KP_D to Key.D,
    KP_E to Key.E,
    KP_F to Key.F,
//    KP_XOR
    KP_POWER to Key.POWER,
//    KP_PERCENT
//    KP_LESS
//    KP_GREATER
//    KP_AMPERSAND
//    KP_DBLAMPERSAND
//    KP_VERTICALBAR
//    KP_DBLVERTICALBAR
//    KP_COLON
    KP_HASH to Key.POUND,
    KP_SPACE to Key.SPACE,
    KP_AT to Key.AT,
//    KP_EXCLAM
//    KP_MEMSTORE
//    KP_MEMRECALL
//    KP_MEMCLEAR
//    KP_MEMADD
//    KP_MEMSUBTRACT
//    KP_MEMMULTIPLY
//    KP_DIVIDE
//    KP_PLUSMINUS
    KP_CLEAR to Key.CLEAR,
//    KP_CLEARENTRY
//    KP_BINARY
//    KP_OCTAL
    KP_DECIMAL to Key.KP_DECIMAL,
//    KP_HEXADECIMAL

    LCTRL to Key.LEFT_CONTROL,
    LSHIFT to Key.LEFT_SHIFT,
    LALT to Key.LEFT_ALT,
    LGUI to Key.LEFT_SUPER,
    RCTRL to Key.RIGHT_CONTROL,
    RSHIFT to Key.RIGHT_SHIFT,
    RALT to Key.RIGHT_ALT,
    RGUI to Key.RIGHT_SUPER,

//    MODE

    AUDIONEXT to Key.MEDIA_NEXT_TRACK,
    AUDIOPREV to Key.MEDIA_PREV_TRACK,
    AUDIOSTOP to Key.MEDIA_STOP,
    AUDIOPLAY to Key.MEDIA_PLAY,
    AUDIOMUTE to Key.VOLUME_MUTE,
    MEDIASELECT to Key.LAUNCH_MEDIA_SELECT,
//    WWW
    MAIL to Key.LAUNCH_MAIL,
    CALCULATOR to Key.CALCULATOR,
//    COMPUTER
    AC_SEARCH to Key.BROWSER_SEARCH,
    AC_HOME to Key.BROWSER_HOME,
    AC_BACK to Key.BROWSER_BACK,
    AC_FORWARD to Key.BROWSER_FORWARD,
    AC_STOP to Key.BROWSER_STOP,
    AC_REFRESH to Key.BROWSER_REFRESH,
    AC_BOOKMARKS to Key.BROWSER_FAVORITES,

    BRIGHTNESSDOWN to Key.BRIGHTNESS_DOWN,
    BRIGHTNESSUP to Key.BRIGHTNESS_UP,
//    DISPLAYSWITCH
//    KBDILLUMTOGGLE
//    KBDILLUMDOWN
//    KBDILLUMUP
    EJECT to Key.MEDIA_EJECT,
    SLEEP to Key.SLEEP,
    APP1 to Key.LAUNCH_APP1,
    APP2 to Key.LAUNCH_APP2,

    AUDIOREWIND to Key.MEDIA_REWIND,
    AUDIOFASTFORWARD to Key.MEDIA_FAST_FORWARD,
)
