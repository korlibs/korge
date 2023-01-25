package com.soywiz.korgw

import com.soywiz.korev.Key

// -	0x0A-0B	Reserved
// -	0x0E-0F	Undefined
//-	0x3A-40	Undefined
// -	0x5E	Reserved
// -	0x88-8F	Unassigned
//0x92-96	OEM specific
//-	0x97-9F	Unassigned
// -	0xB8-B9	Reserved
//-	0xC1-D7	Reserved
//-	0xD8-DA	Unassigned
//-	0xE0	Reserved
//0xE9-F5	OEM specific

const val VK_LBUTTON = 0x01 // Left mouse button
const val VK_RBUTTON = 0x02 // Right mouse button
const val VK_CANCEL = 0x03 // Control-break processing
const val VK_MBUTTON = 0x04 // Middle mouse button (three-button mouse)
const val VK_XBUTTON1 = 0x05 // X1 mouse button
const val VK_XBUTTON2 = 0x06 // X2 mouse button
const val VK_UNDEFINED_07 = 0x07 // Undefined
const val VK_BACK = 0x08 // BACKSPACE key
const val VK_TAB = 0x09 // TAB key
const val VK_CLEAR = 0x0C // CLEAR key
const val VK_RETURN = 0x0D // ENTER key
const val VK_SHIFT = 0x10 // SHIFT key
const val VK_CONTROL = 0x11 // CTRL key
const val VK_MENU = 0x12 // ALT key
const val VK_PAUSE = 0x13 // PAUSE key
const val VK_CAPITAL = 0x14 // CAPS LOCK key
const val VK_KANA = 0x15 // IME Kana mode
const val VK_HANGUEL = 0x15 // IME Hanguel mode (maintained for compatibility; use VK_HANGUL)
const val VK_HANGUL = 0x15 // IME Hangul mode
const val VK_IME_ON = 0x16 // IME On
const val VK_JUNJA = 0x17 // IME Junja mode
const val VK_FINAL = 0x18 // IME final mode
const val VK_HANJA = 0x19 // IME Hanja mode
const val VK_KANJI = 0x19 // IME Kanji mode
const val VK_IME_OFF = 0x1A // IME Off
const val VK_ESCAPE = 0x1B // ESC key
const val VK_CONVERT = 0x1C // IME convert
const val VK_NONCONVERT = 0x1D // IME nonconvert
const val VK_ACCEPT = 0x1E // IME accept
const val VK_MODECHANGE = 0x1F // IME mode change request
const val VK_SPACE = 0x20 // SPACEBAR
const val VK_PRIOR = 0x21 // PAGE UP key
const val VK_NEXT = 0x22 // PAGE DOWN key
const val VK_END = 0x23 // END key
const val VK_HOME = 0x24 // HOME key
const val VK_LEFT = 0x25 // LEFT ARROW key
const val VK_UP = 0x26 // UP ARROW key
const val VK_RIGHT = 0x27 // RIGHT ARROW key
const val VK_DOWN = 0x28 // DOWN ARROW key
const val VK_SELECT = 0x29 // SELECT key
const val VK_PRINT = 0x2A // PRINT key
const val VK_EXECUTE = 0x2B // EXECUTE key
const val VK_SNAPSHOT = 0x2C // PRINT SCREEN key
const val VK_INSERT = 0x2D // INS key
const val VK_DELETE = 0x2E // DEL key
const val VK_HELP = 0x2F // HELP key
const val VK_KEY_0 = 0x30 // 0 key
const val VK_KEY_1 = 0x31 // 1 key
const val VK_KEY_2 = 0x32 // 2 key
const val VK_KEY_3 = 0x33 // 3 key
const val VK_KEY_4 = 0x34 // 4 key
const val VK_KEY_5 = 0x35 // 5 key
const val VK_KEY_6 = 0x36 // 6 key
const val VK_KEY_7 = 0x37 // 7 key
const val VK_KEY_8 = 0x38 // 8 key
const val VK_KEY_9 = 0x39 // 9 key
const val VK_KEY_A = 0x41 // A key
const val VK_KEY_B = 0x42 // B key
const val VK_KEY_C = 0x43 // C key
const val VK_KEY_D = 0x44 // D key
const val VK_KEY_E = 0x45 // E key
const val VK_KEY_F = 0x46 // F key
const val VK_KEY_G = 0x47 // G key
const val VK_KEY_H = 0x48 // H key
const val VK_KEY_I = 0x49 // I key
const val VK_KEY_J = 0x4A // J key
const val VK_KEY_K = 0x4B // K key
const val VK_KEY_L = 0x4C // L key
const val VK_KEY_M = 0x4D // M key
const val VK_KEY_N = 0x4E // N key
const val VK_KEY_O = 0x4F // O key
const val VK_KEY_P = 0x50 // P key
const val VK_KEY_Q = 0x51 // Q key
const val VK_KEY_R = 0x52 // R key
const val VK_KEY_S = 0x53 // S key
const val VK_KEY_T = 0x54 // T key
const val VK_KEY_U = 0x55 // U key
const val VK_KEY_V = 0x56 // V key
const val VK_KEY_W = 0x57 // W key
const val VK_KEY_X = 0x58 // X key
const val VK_KEY_Y = 0x59 // Y key
const val VK_KEY_Z = 0x5A // Z key
const val VK_LWIN = 0x5B // Left Windows key (Natural keyboard)
const val VK_RWIN = 0x5C // Right Windows key (Natural keyboard)
const val VK_APPS = 0x5D // Applications key (Natural keyboard)
const val VK_SLEEP = 0x5F // Computer Sleep key
const val VK_NUMPAD0 = 0x60 // Numeric keypad 0 key
const val VK_NUMPAD1 = 0x61 // Numeric keypad 1 key
const val VK_NUMPAD2 = 0x62 // Numeric keypad 2 key
const val VK_NUMPAD3 = 0x63 // Numeric keypad 3 key
const val VK_NUMPAD4 = 0x64 // Numeric keypad 4 key
const val VK_NUMPAD5 = 0x65 // Numeric keypad 5 key
const val VK_NUMPAD6 = 0x66 // Numeric keypad 6 key
const val VK_NUMPAD7 = 0x67 // Numeric keypad 7 key
const val VK_NUMPAD8 = 0x68 // Numeric keypad 8 key
const val VK_NUMPAD9 = 0x69 // Numeric keypad 9 key
const val VK_MULTIPLY = 0x6A // Multiply key
const val VK_ADD = 0x6B // Add key
const val VK_SEPARATOR = 0x6C // Separator key
const val VK_SUBTRACT = 0x6D // Subtract key
const val VK_DECIMAL = 0x6E // Decimal key
const val VK_DIVIDE = 0x6F // Divide key
const val VK_F1 = 0x70 // F1 key
const val VK_F2 = 0x71 // F2 key
const val VK_F3 = 0x72 // F3 key
const val VK_F4 = 0x73 // F4 key
const val VK_F5 = 0x74 // F5 key
const val VK_F6 = 0x75 // F6 key
const val VK_F7 = 0x76 // F7 key
const val VK_F8 = 0x77 // F8 key
const val VK_F9 = 0x78 // F9 key
const val VK_F10 = 0x79 // F10 key
const val VK_F11 = 0x7A // F11 key
const val VK_F12 = 0x7B // F12 key
const val VK_F13 = 0x7C // F13 key
const val VK_F14 = 0x7D // F14 key
const val VK_F15 = 0x7E // F15 key
const val VK_F16 = 0x7F // F16 key
const val VK_F17 = 0x80 // F17 key
const val VK_F18 = 0x81 // F18 key
const val VK_F19 = 0x82 // F19 key
const val VK_F20 = 0x83 // F20 key
const val VK_F21 = 0x84 // F21 key
const val VK_F22 = 0x85 // F22 key
const val VK_F23 = 0x86 // F23 key
const val VK_F24 = 0x87 // F24 key
const val VK_NUMLOCK = 0x90 // NUM LOCK key
const val VK_SCROLL = 0x91 // SCROLL LOCK key
const val VK_OEM_92 = 0x92
const val VK_OEM_93 = 0x93
const val VK_OEM_94 = 0x94
const val VK_OEM_95 = 0x95
const val VK_OEM_96 = 0x96
const val VK_LSHIFT = 0xA0 // Left SHIFT key
const val VK_RSHIFT = 0xA1 // Right SHIFT key
const val VK_LCONTROL = 0xA2 // Left CONTROL key
const val VK_RCONTROL = 0xA3 // Right CONTROL key
const val VK_LMENU = 0xA4 // Left ALT key
const val VK_RMENU = 0xA5 // Right ALT key
const val VK_BROWSER_BACK = 0xA6 // Browser Back key
const val VK_BROWSER_FORWARD = 0xA7 // Browser Forward key
const val VK_BROWSER_REFRESH = 0xA8 // Browser Refresh key
const val VK_BROWSER_STOP = 0xA9 // Browser Stop key
const val VK_BROWSER_SEARCH = 0xAA // Browser Search key
const val VK_BROWSER_FAVORITES = 0xAB // Browser Favorites key
const val VK_BROWSER_HOME = 0xAC // Browser Start and Home key
const val VK_VOLUME_MUTE = 0xAD // Volume Mute key
const val VK_VOLUME_DOWN = 0xAE // Volume Down key
const val VK_VOLUME_UP = 0xAF // Volume Up key
const val VK_MEDIA_NEXT_TRACK = 0xB0 // Next Track key
const val VK_MEDIA_PREV_TRACK = 0xB1 // Previous Track key
const val VK_MEDIA_STOP = 0xB2 // Stop Media key
const val VK_MEDIA_PLAY_PAUSE = 0xB3 // Play/Pause Media key
const val VK_LAUNCH_MAIL = 0xB4 // Start Mail key
const val VK_LAUNCH_MEDIA_SELECT = 0xB5 // Select Media key
const val VK_LAUNCH_APP1 = 0xB6 // Start Application 1 key
const val VK_LAUNCH_APP2 = 0xB7 // Start Application 2 key
const val VK_OEM_1 = 0xBA // Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the ';:' key
const val VK_OEM_PLUS = 0xBB // For any country/region, the '+' key
const val VK_OEM_COMMA = 0xBC // For any country/region, the ',' key
const val VK_OEM_MINUS = 0xBD // For any country/region, the '-' key
const val VK_OEM_PERIOD = 0xBE // For any country/region, the '.' key
const val VK_OEM_2 = 0xBF // Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the '/?' key
const val VK_OEM_3 = 0xC0 // Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the '`~' key
const val VK_OEM_4 = 0xDB // Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the '[{' key
const val VK_OEM_5 = 0xDC // Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the '\|' key
const val VK_OEM_6 = 0xDD // Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the ']}' key
const val VK_OEM_7 = 0xDE // Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the 'single-quote/double-quote' key
const val VK_OEM_8 = 0xDF // Used for miscellaneous characters; it can vary by keyboard.
const val VK_OEM_E1 = 0xE1 // OEM specific
const val VK_OEM_102 = 0xE2 // The <> keys on the US standard keyboard, or the \\| key on the non-US 102-key keyboard
const val VK_OEM_E3 = 0xE3 // OEM E3
const val VK_OEM_E4 = 0xE4 // OEM E4
const val VK_PROCESSKEY = 0xE5 // IME PROCESS key
const val VK_OEM_E6 = 0xE6 // OEM specific
const val VK_PACKET = 0xE7 // Used to pass Unicode characters as if they were keystrokes. The VK_PACKET key is the low word of a 32-bit Virtual Key value used for non-keyboard input methods. For more information, see Remark in KEYBDINPUT, SendInput, WM_KEYDOWN, and WM_KEYUP
const val VK_UNASIGNED_E8 = 0xE8 // Unassigned
const val VK_ATTN = 0xF6 // Attn key
const val VK_CRSEL = 0xF7 // CrSel key
const val VK_EXSEL = 0xF8 // ExSel key
const val VK_EREOF = 0xF9 // Erase EOF key
const val VK_PLAY = 0xFA // Play key
const val VK_ZOOM = 0xFB // Zoom key
const val VK_NONAME = 0xFC // Reserved
const val VK_PA1 = 0xFD // PA1 key
const val VK_OEM_CLEAR = 0xFE // Clear key

/*
const val VK_ADD = 0x6B
const val VK_ATTN = 0xF6
const val VK_BACK = 0x08
const val VK_CANCEL = 0x03
const val VK_CLEAR = 0x0C
const val VK_CRSEL = 0xF7
const val VK_DECIMAL = 0x6E
const val VK_DIVIDE = 0x6F
const val VK_EREOF = 0xF9
const val VK_ESCAPE = 0x1B
const val VK_EXECUTE = 0x2B
const val VK_EXSEL = 0xF8
const val VK_KEY_0 = 0x30
const val VK_KEY_1 = 0x31
const val VK_KEY_2 = 0x32
const val VK_KEY_3 = 0x33
const val VK_KEY_4 = 0x34
const val VK_KEY_5 = 0x35
const val VK_KEY_6 = 0x36
const val VK_KEY_7 = 0x37
const val VK_KEY_8 = 0x38
const val VK_KEY_9 = 0x39
const val VK_KEY_A = 0x41
const val VK_KEY_B = 0x42
const val VK_KEY_C = 0x43
const val VK_KEY_D = 0x44
const val VK_KEY_E = 0x45
const val VK_KEY_F = 0x46
const val VK_KEY_G = 0x47
const val VK_KEY_H = 0x48
const val VK_KEY_I = 0x49
const val VK_KEY_J = 0x4A
const val VK_KEY_K = 0x4B
const val VK_KEY_L = 0x4C
const val VK_KEY_M = 0x4D
const val VK_KEY_N = 0x4E
const val VK_KEY_O = 0x4F
const val VK_KEY_P = 0x50
const val VK_KEY_Q = 0x51
const val VK_KEY_R = 0x52
const val VK_KEY_S = 0x53
const val VK_KEY_T = 0x54
const val VK_KEY_U = 0x55
const val VK_KEY_V = 0x56
const val VK_KEY_W = 0x57
const val VK_KEY_X = 0x58
const val VK_KEY_Y = 0x59
const val VK_KEY_Z = 0x5A
const val VK_MULTIPLY = 0x6A
const val VK_NONAME = 0xFC
const val VK_NUMPAD0 = 0x60
const val VK_NUMPAD1 = 0x61
const val VK_NUMPAD2 = 0x62
const val VK_NUMPAD3 = 0x63
const val VK_NUMPAD4 = 0x64
const val VK_NUMPAD5 = 0x65
const val VK_NUMPAD6 = 0x66
const val VK_NUMPAD7 = 0x67
const val VK_NUMPAD8 = 0x68
const val VK_NUMPAD9 = 0x69
const val VK_PA1 = 0xFD
const val VK_PACKET = 0xE7
const val VK_PLAY = 0xFA
const val VK_PROCESSKEY = 0xE5
const val VK_RETURN = 0x0D
const val VK_SELECT = 0x29
const val VK_SEPARATOR = 0x6C
const val VK_SPACE = 0x20
const val VK_SUBTRACT = 0x6D
const val VK_TAB = 0x09
const val VK_ZOOM = 0xFB
const val VK_ACCEPT = 0x1E
const val VK_APPS = 0x5D
const val VK_BROWSER_BACK = 0xA6
const val VK_BROWSER_FAVORITES = 0xAB
const val VK_BROWSER_FORWARD = 0xA7
const val VK_BROWSER_HOME = 0xAC
const val VK_BROWSER_REFRESH = 0xA8
const val VK_BROWSER_SEARCH = 0xAA
const val VK_BROWSER_STOP = 0xA9
const val VK_CAPITAL = 0x14
const val VK_CONVERT = 0x1C
const val VK_DELETE = 0x2E
const val VK_DOWN = 0x28
const val VK_END = 0x23
const val VK_F1 = 0x70
const val VK_F10 = 0x79
const val VK_F11 = 0x7A
const val VK_F12 = 0x7B
const val VK_F13 = 0x7C
const val VK_F14 = 0x7D
const val VK_F15 = 0x7E
const val VK_F16 = 0x7F
const val VK_F17 = 0x80
const val VK_F18 = 0x81
const val VK_F19 = 0x82
const val VK_F2 = 0x71
const val VK_F20 = 0x83
const val VK_F21 = 0x84
const val VK_F22 = 0x85
const val VK_F23 = 0x86
const val VK_F24 = 0x87
const val VK_F3 = 0x72
const val VK_F4 = 0x73
const val VK_F5 = 0x74
const val VK_F6 = 0x75
const val VK_F7 = 0x76
const val VK_F8 = 0x77
const val VK_F9 = 0x78
const val VK_FINAL = 0x18
const val VK_HELP = 0x2F
const val VK_HOME = 0x24
const val VK_INSERT = 0x2D
const val VK_JUNJA = 0x17
const val VK_KANA = 0x15
const val VK_KANJI = 0x19
const val VK_LAUNCH_APP1 = 0xB6
const val VK_LAUNCH_APP2 = 0xB7
const val VK_LAUNCH_MAIL = 0xB4
const val VK_LAUNCH_MEDIA_SELECT = 0xB5
const val VK_LBUTTON = 0x01
const val VK_LCONTROL = 0xA2
const val VK_LEFT = 0x25
const val VK_LMENU = 0xA4
const val VK_LSHIFT = 0xA0
const val VK_LWIN = 0x5B
const val VK_MBUTTON = 0x04
const val VK_MEDIA_NEXT_TRACK = 0xB0
const val VK_MEDIA_PLAY_PAUSE = 0xB3
const val VK_MEDIA_PREV_TRACK = 0xB1
const val VK_MEDIA_STOP = 0xB2
const val VK_MODECHANGE = 0x1F
const val VK_NEXT = 0x22
const val VK_NONCONVERT = 0x1D
const val VK_NUMLOCK = 0x90
const val VK_PAUSE = 0x13
const val VK_PRINT = 0x2A
const val VK_PRIOR = 0x21
const val VK_RBUTTON = 0x02
const val VK_RCONTROL = 0xA3
const val VK_RIGHT = 0x27
const val VK_RMENU = 0xA5
const val VK_RSHIFT = 0xA1
const val VK_RWIN = 0x5C
const val VK_SCROLL = 0x91
const val VK_SLEEP = 0x5F
const val VK_SNAPSHOT = 0x2C
const val VK_UP = 0x26
const val VK_VOLUME_DOWN = 0xAE
const val VK_VOLUME_MUTE = 0xAD
const val VK_VOLUME_UP = 0xAF
const val VK_XBUTTON1 = 0x05
const val VK_XBUTTON2 = 0x06
*/

const val VK_OEM_ATTN = 0xF0
const val VK_OEM_AUTO = 0xF3
const val VK_OEM_AX = 0xE1
const val VK_OEM_BACKTAB = 0xF5
const val VK_OEM_COPY = 0xF2
const val VK_OEM_CUSEL = 0xEF
const val VK_OEM_ENLW = 0xF4
const val VK_OEM_FINISH = 0xF1
const val VK_OEM_FJ_LOYA = 0x95
const val VK_OEM_FJ_MASSHOU = 0x93
const val VK_OEM_FJ_ROYA = 0x96
const val VK_OEM_FJ_TOUROKU = 0x94
const val VK_OEM_JUMP = 0xEA
const val VK_OEM_PA1 = 0xEB
const val VK_OEM_PA2 = 0xEC
const val VK_OEM_PA3 = 0xED
const val VK_OEM_RESET = 0xE9
const val VK_OEM_WSCTRL = 0xEE
const val VK_OEM_FJ_JISHO = 0x92
const val VK_ICO_00 = 0xE4

const val VK_ICO_CLEAR = 0xE6
const val VK_ICO_HELP = 0xE3
const val VK_ABNT_C1 = 0xC1
const val VK_ABNT_C2 = 0xC2

const val VK__none_ = 0xFF

val KEYS = mapOf(
    VK_ABNT_C1 to Key.ABNT_C1,
    VK_ABNT_C2 to Key.ABNT_C2,
    VK_ADD to Key.KP_ADD,
    VK_ATTN to Key.ATTN,
    VK_BACK to Key.BACKSPACE,
    VK_CANCEL to Key.CANCEL,
    VK_CLEAR to Key.CLEAR,
    VK_CRSEL to Key.CRSEL,
    VK_DECIMAL to Key.KP_DECIMAL,
    VK_DIVIDE to Key.KP_DIVIDE,
    VK_EREOF to Key.EREOF,
    VK_ESCAPE to Key.ESCAPE,
    VK_EXECUTE to Key.EXECUTE,
    VK_EXSEL to Key.EXSEL,
    VK_ICO_CLEAR to Key.ICO_CLEAR,
    VK_ICO_HELP to Key.ICO_HELP,
    VK_KEY_0 to Key.N0,
    VK_KEY_1 to Key.N1,
    VK_KEY_2 to Key.N2,
    VK_KEY_3 to Key.N3,
    VK_KEY_4 to Key.N4,
    VK_KEY_5 to Key.N5,
    VK_KEY_6 to Key.N6,
    VK_KEY_7 to Key.N7,
    VK_KEY_8 to Key.N8,
    VK_KEY_9 to Key.N9,
    VK_KEY_A to Key.A,
    VK_KEY_B to Key.B,
    VK_KEY_C to Key.C,
    VK_KEY_D to Key.D,
    VK_KEY_E to Key.E,
    VK_KEY_F to Key.F,
    VK_KEY_G to Key.G,
    VK_KEY_H to Key.H,
    VK_KEY_I to Key.I,
    VK_KEY_J to Key.J,
    VK_KEY_K to Key.K,
    VK_KEY_L to Key.L,
    VK_KEY_M to Key.M,
    VK_KEY_N to Key.N,
    VK_KEY_O to Key.O,
    VK_KEY_P to Key.P,
    VK_KEY_Q to Key.Q,
    VK_KEY_R to Key.R,
    VK_KEY_S to Key.S,
    VK_KEY_T to Key.T,
    VK_KEY_U to Key.U,
    VK_KEY_V to Key.V,
    VK_KEY_W to Key.W,
    VK_KEY_X to Key.X,
    VK_KEY_Y to Key.Y,
    VK_KEY_Z to Key.Z,
    VK_MULTIPLY to Key.KP_MULTIPLY,
    VK_NONAME to Key.NONAME,
    VK_NUMPAD0 to Key.N0,
    VK_NUMPAD1 to Key.N1,
    VK_NUMPAD2 to Key.N2,
    VK_NUMPAD3 to Key.N3,
    VK_NUMPAD4 to Key.N4,
    VK_NUMPAD5 to Key.N5,
    VK_NUMPAD6 to Key.N6,
    VK_NUMPAD7 to Key.N7,
    VK_NUMPAD8 to Key.N8,
    VK_NUMPAD9 to Key.N9,
    VK_OEM_1 to Key.OEM1,
    VK_OEM_102 to Key.OEM102,
    VK_OEM_2 to Key.OEM2,
    VK_OEM_3 to Key.OEM3,
    VK_OEM_4 to Key.OEM4,
    VK_OEM_5 to Key.OEM5,
    VK_OEM_6 to Key.OEM6,
    VK_OEM_7 to Key.OEM7,
    VK_OEM_8 to Key.OEM8,
    VK_OEM_ATTN to Key.OEM_ATTN,
    VK_OEM_AUTO to Key.OEM_AUTO,
    VK_OEM_AX to Key.OEM_AX,
    VK_OEM_BACKTAB to Key.OEM_BACKTAB,
    VK_OEM_CLEAR to Key.OEM_CLEAR,
    VK_OEM_COMMA to Key.OEM_COMMA,
    VK_OEM_COPY to Key.OEM_COPY,
    VK_OEM_CUSEL to Key.OEM_CUSEL,
    VK_OEM_ENLW to Key.OEM_ENLW,
    VK_OEM_FINISH to Key.OEM_FINISH,
    VK_OEM_FJ_LOYA to Key.OEM_FJ_LOYA,
    VK_OEM_FJ_MASSHOU to Key.OEM_FJ_MASSHOU,
    VK_OEM_FJ_ROYA to Key.OEM_FJ_ROYA,
    VK_OEM_FJ_TOUROKU to Key.OEM_FJ_TOUROKU,
    VK_OEM_JUMP to Key.OEM_JUMP,
    VK_OEM_MINUS to Key.OEM_MINUS,
    VK_OEM_PA1 to Key.OEM_PA1,
    VK_OEM_PA2 to Key.OEM_PA2,
    VK_OEM_PA3 to Key.OEM_PA3,
    VK_OEM_PERIOD to Key.OEM_PERIOD,
    VK_OEM_PLUS to Key.OEM_PLUS,
    VK_OEM_RESET to Key.OEM_RESET,
    VK_OEM_WSCTRL to Key.OEM_WSCTRL,
    VK_PA1 to Key.PA1,
    VK_PACKET to Key.PACKET,
    VK_PLAY to Key.PLAY,
    VK_PROCESSKEY to Key.PROCESSKEY,
    VK_RETURN to Key.ENTER,
    VK_SELECT to Key.SELECT_KEY,
    VK_SEPARATOR to Key.KP_SEPARATOR,
    VK_SPACE to Key.SPACE,
    VK_SUBTRACT to Key.KP_SUBTRACT,
    VK_TAB to Key.TAB,
    VK_ZOOM to Key.ZOOM,
    VK__none_ to Key.NONE,
    VK_ACCEPT to Key.ACCEPT,
    VK_APPS to Key.APPS,
    VK_BROWSER_BACK to Key.BROWSER_BACK,
    VK_BROWSER_FAVORITES to Key.BROWSER_FAVORITES,
    VK_BROWSER_FORWARD to Key.BROWSER_FORWARD,
    VK_BROWSER_HOME to Key.BROWSER_HOME,
    VK_BROWSER_REFRESH to Key.BROWSER_REFRESH,
    VK_BROWSER_SEARCH to Key.BROWSER_SEARCH,
    VK_BROWSER_STOP to Key.BROWSER_STOP,
    VK_CAPITAL to Key.CAPITAL,
    VK_CONVERT to Key.CONVERT,
    VK_DELETE to Key.DELETE,
    VK_DOWN to Key.DOWN,
    VK_END to Key.END,
    VK_F1 to Key.F1,
    VK_F2 to Key.F2,
    VK_F3 to Key.F3,
    VK_F4 to Key.F4,
    VK_F5 to Key.F5,
    VK_F6 to Key.F6,
    VK_F7 to Key.F7,
    VK_F8 to Key.F8,
    VK_F9 to Key.F9,
    VK_F10 to Key.F10,
    VK_F11 to Key.F11,
    VK_F12 to Key.F12,
    VK_F13 to Key.F13,
    VK_F14 to Key.F14,
    VK_F15 to Key.F15,
    VK_F16 to Key.F16,
    VK_F17 to Key.F17,
    VK_F18 to Key.F18,
    VK_F19 to Key.F19,
    VK_F20 to Key.F20,
    VK_F21 to Key.F21,
    VK_F22 to Key.F22,
    VK_F23 to Key.F23,
    VK_F24 to Key.F24,
    VK_FINAL to Key.FINAL,
    VK_HELP to Key.HELP,
    VK_HOME to Key.HOME,
    VK_ICO_00 to Key.ICO_00,
    VK_INSERT to Key.INSERT,
    VK_JUNJA to Key.JUNJA,
    VK_KANA to Key.KANA,
    VK_KANJI to Key.KANJI,
    VK_LAUNCH_APP1 to Key.LAUNCH_APP1,
    VK_LAUNCH_APP2 to Key.LAUNCH_APP2,
    VK_LAUNCH_MAIL to Key.LAUNCH_MAIL,
    VK_LAUNCH_MEDIA_SELECT to Key.LAUNCH_MEDIA_SELECT,
    VK_LBUTTON to Key.LEFT_BUTTON,
    VK_LCONTROL to Key.LEFT_CONTROL,
    VK_LEFT to Key.LEFT,
    VK_LMENU to Key.LEFT_MENU,
    VK_LSHIFT to Key.LEFT_SHIFT,
    VK_LWIN to Key.LEFT_SUPER,
    VK_MBUTTON to Key.MIDDLE_BUTTON,
    VK_MEDIA_NEXT_TRACK to Key.MEDIA_NEXT_TRACK,
    VK_MEDIA_PLAY_PAUSE to Key.MEDIA_PLAY_PAUSE,
    VK_MEDIA_PREV_TRACK to Key.MEDIA_PREV_TRACK,
    VK_MEDIA_STOP to Key.MEDIA_STOP,
    VK_MODECHANGE to Key.MODECHANGE,
    VK_NEXT to Key.NEXT,
    VK_NONCONVERT to Key.NONCONVERT,
    VK_NUMLOCK to Key.NUM_LOCK,
    VK_OEM_FJ_JISHO to Key.OEM_FJ_JISHO,
    VK_PAUSE to Key.PAUSE,
    VK_PRINT to Key.PRINT_SCREEN,
    VK_PRIOR to Key.PRIOR,
    VK_RBUTTON to Key.RIGHT_BUTTON,
    VK_RCONTROL to Key.RIGHT_CONTROL,
    VK_RIGHT to Key.RIGHT,
    VK_RMENU to Key.RIGHT_MENU,
    VK_RSHIFT to Key.RIGHT_SHIFT,
    VK_RWIN to Key.RIGHT_SUPER,
    VK_SCROLL to Key.SCROLL_LOCK,
    VK_SLEEP to Key.SLEEP,
    VK_SNAPSHOT to Key.SNAPSHOT,
    VK_UP to Key.UP,
    VK_VOLUME_DOWN to Key.VOLUME_DOWN,
    VK_VOLUME_MUTE to Key.VOLUME_MUTE,
    VK_VOLUME_UP to Key.VOLUME_UP,
    VK_XBUTTON1 to Key.XBUTTON1,
    VK_XBUTTON2 to Key.XBUTTON2,

    VK_SHIFT to Key.SHIFT,
    VK_CONTROL to Key.CONTROL,
    VK_MENU to Key.ALT,
    VK_PAUSE to Key.PAUSE,
    VK_CAPITAL to Key.CAPS_LOCK,
    VK_KANA to Key.KANA,
)
