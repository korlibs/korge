package com.soywiz.korgw.x11

import com.soywiz.kds.IntMap
import com.soywiz.korev.Key
import com.soywiz.korgw.platform.INativeGL
import com.soywiz.korgw.platform.KStructure
import com.soywiz.korgw.platform.NativeKgl
import com.sun.jna.*
import com.sun.jna.platform.unix.X11
import com.sun.jna.ptr.*


typealias XVisualInfo = Pointer
typealias GLXContext = Pointer

internal const val GLX_RGBA = 4
internal const val GLX_RED_SIZE	= 8
internal const val GLX_GREEN_SIZE = 9
internal const val GLX_BLUE_SIZE = 10
internal const val GLX_ALPHA_SIZE = 11
internal const val GLX_DEPTH_SIZE = 12
internal const val GLX_DOUBLEBUFFER = 5

internal const val XK_space = 0x0020  /* U+0020 SPACE */
internal const val XK_exclam = 0x0021  /* U+0021 EXCLAMATION MARK */
internal const val XK_quotedbl = 0x0022  /* U+0022 QUOTATION MARK */
internal const val XK_numbersign = 0x0023  /* U+0023 NUMBER SIGN */
internal const val XK_dollar = 0x0024  /* U+0024 DOLLAR SIGN */
internal const val XK_percent = 0x0025  /* U+0025 PERCENT SIGN */
internal const val XK_ampersand = 0x0026  /* U+0026 AMPERSAND */
internal const val XK_apostrophe = 0x0027  /* U+0027 APOSTROPHE */
internal const val XK_quoteright = 0x0027  /* deprecated */
internal const val XK_parenleft = 0x0028  /* U+0028 LEFT PARENTHESIS */
internal const val XK_parenright = 0x0029  /* U+0029 RIGHT PARENTHESIS */
internal const val XK_asterisk = 0x002a  /* U+002A ASTERISK */
internal const val XK_plus = 0x002b  /* U+002B PLUS SIGN */
internal const val XK_comma = 0x002c  /* U+002C COMMA */
internal const val XK_minus = 0x002d  /* U+002D HYPHEN-MINUS */
internal const val XK_period = 0x002e  /* U+002E FULL STOP */
internal const val XK_slash = 0x002f  /* U+002F SOLIDUS */
internal const val XK_0 = 0x0030  /* U+0030 DIGIT ZERO */
internal const val XK_1 = 0x0031  /* U+0031 DIGIT ONE */
internal const val XK_2 = 0x0032  /* U+0032 DIGIT TWO */
internal const val XK_3 = 0x0033  /* U+0033 DIGIT THREE */
internal const val XK_4 = 0x0034  /* U+0034 DIGIT FOUR */
internal const val XK_5 = 0x0035  /* U+0035 DIGIT FIVE */
internal const val XK_6 = 0x0036  /* U+0036 DIGIT SIX */
internal const val XK_7 = 0x0037  /* U+0037 DIGIT SEVEN */
internal const val XK_8 = 0x0038  /* U+0038 DIGIT EIGHT */
internal const val XK_9 = 0x0039  /* U+0039 DIGIT NINE */
internal const val XK_colon = 0x003a  /* U+003A COLON */
internal const val XK_semicolon = 0x003b  /* U+003B SEMICOLON */
internal const val XK_less = 0x003c  /* U+003C LESS-THAN SIGN */
internal const val XK_equal = 0x003d  /* U+003D EQUALS SIGN */
internal const val XK_greater = 0x003e  /* U+003E GREATER-THAN SIGN */
internal const val XK_question = 0x003f  /* U+003F QUESTION MARK */
internal const val XK_at = 0x0040  /* U+0040 COMMERCIAL AT */
internal const val XK_A = 0x0041  /* U+0041 LATIN CAPITAL LETTER A */
internal const val XK_B = 0x0042  /* U+0042 LATIN CAPITAL LETTER B */
internal const val XK_C = 0x0043  /* U+0043 LATIN CAPITAL LETTER C */
internal const val XK_D = 0x0044  /* U+0044 LATIN CAPITAL LETTER D */
internal const val XK_E = 0x0045  /* U+0045 LATIN CAPITAL LETTER E */
internal const val XK_F = 0x0046  /* U+0046 LATIN CAPITAL LETTER F */
internal const val XK_G = 0x0047  /* U+0047 LATIN CAPITAL LETTER G */
internal const val XK_H = 0x0048  /* U+0048 LATIN CAPITAL LETTER H */
internal const val XK_I = 0x0049  /* U+0049 LATIN CAPITAL LETTER I */
internal const val XK_J = 0x004a  /* U+004A LATIN CAPITAL LETTER J */
internal const val XK_K = 0x004b  /* U+004B LATIN CAPITAL LETTER K */
internal const val XK_L = 0x004c  /* U+004C LATIN CAPITAL LETTER L */
internal const val XK_M = 0x004d  /* U+004D LATIN CAPITAL LETTER M */
internal const val XK_N = 0x004e  /* U+004E LATIN CAPITAL LETTER N */
internal const val XK_O = 0x004f  /* U+004F LATIN CAPITAL LETTER O */
internal const val XK_P = 0x0050  /* U+0050 LATIN CAPITAL LETTER P */
internal const val XK_Q = 0x0051  /* U+0051 LATIN CAPITAL LETTER Q */
internal const val XK_R = 0x0052  /* U+0052 LATIN CAPITAL LETTER R */
internal const val XK_S = 0x0053  /* U+0053 LATIN CAPITAL LETTER S */
internal const val XK_T = 0x0054  /* U+0054 LATIN CAPITAL LETTER T */
internal const val XK_U = 0x0055  /* U+0055 LATIN CAPITAL LETTER U */
internal const val XK_V = 0x0056  /* U+0056 LATIN CAPITAL LETTER V */
internal const val XK_W = 0x0057  /* U+0057 LATIN CAPITAL LETTER W */
internal const val XK_X = 0x0058  /* U+0058 LATIN CAPITAL LETTER X */
internal const val XK_Y = 0x0059  /* U+0059 LATIN CAPITAL LETTER Y */
internal const val XK_Z = 0x005a  /* U+005A LATIN CAPITAL LETTER Z */
internal const val XK_bracketleft = 0x005b  /* U+005B LEFT SQUARE BRACKET */
internal const val XK_backslash = 0x005c  /* U+005C REVERSE SOLIDUS */
internal const val XK_bracketright = 0x005d  /* U+005D RIGHT SQUARE BRACKET */
internal const val XK_asciicircum = 0x005e  /* U+005E CIRCUMFLEX ACCENT */
internal const val XK_underscore = 0x005f  /* U+005F LOW LINE */
internal const val XK_grave = 0x0060  /* U+0060 GRAVE ACCENT */
internal const val XK_quoteleft = 0x0060  /* deprecated */
internal const val XK_a = 0x0061  /* U+0061 LATIN SMALL LETTER A */
internal const val XK_b = 0x0062  /* U+0062 LATIN SMALL LETTER B */
internal const val XK_c = 0x0063  /* U+0063 LATIN SMALL LETTER C */
internal const val XK_d = 0x0064  /* U+0064 LATIN SMALL LETTER D */
internal const val XK_e = 0x0065  /* U+0065 LATIN SMALL LETTER E */
internal const val XK_f = 0x0066  /* U+0066 LATIN SMALL LETTER F */
internal const val XK_g = 0x0067  /* U+0067 LATIN SMALL LETTER G */
internal const val XK_h = 0x0068  /* U+0068 LATIN SMALL LETTER H */
internal const val XK_i = 0x0069  /* U+0069 LATIN SMALL LETTER I */
internal const val XK_j = 0x006a  /* U+006A LATIN SMALL LETTER J */
internal const val XK_k = 0x006b  /* U+006B LATIN SMALL LETTER K */
internal const val XK_l = 0x006c  /* U+006C LATIN SMALL LETTER L */
internal const val XK_m = 0x006d  /* U+006D LATIN SMALL LETTER M */
internal const val XK_n = 0x006e  /* U+006E LATIN SMALL LETTER N */
internal const val XK_o = 0x006f  /* U+006F LATIN SMALL LETTER O */
internal const val XK_p = 0x0070  /* U+0070 LATIN SMALL LETTER P */
internal const val XK_q = 0x0071  /* U+0071 LATIN SMALL LETTER Q */
internal const val XK_r = 0x0072  /* U+0072 LATIN SMALL LETTER R */
internal const val XK_s = 0x0073  /* U+0073 LATIN SMALL LETTER S */
internal const val XK_t = 0x0074  /* U+0074 LATIN SMALL LETTER T */
internal const val XK_u = 0x0075  /* U+0075 LATIN SMALL LETTER U */
internal const val XK_v = 0x0076  /* U+0076 LATIN SMALL LETTER V */
internal const val XK_w = 0x0077  /* U+0077 LATIN SMALL LETTER W */
internal const val XK_x = 0x0078  /* U+0078 LATIN SMALL LETTER X */
internal const val XK_y = 0x0079  /* U+0079 LATIN SMALL LETTER Y */
internal const val XK_z = 0x007a  /* U+007A LATIN SMALL LETTER Z */
internal const val XK_braceleft = 0x007b  /* U+007B LEFT CURLY BRACKET */
internal const val XK_bar = 0x007c  /* U+007C VERTICAL LINE */
internal const val XK_braceright = 0x007d  /* U+007D RIGHT CURLY BRACKET */
internal const val XK_asciitilde = 0x007e  /* U+007E TILDE */

internal const val XK_leftarrow = 0x08fb  /* U+2190 LEFTWARDS ARROW */
internal const val XK_uparrow = 0x08fc  /* U+2191 UPWARDS ARROW */
internal const val XK_rightarrow = 0x08fd  /* U+2192 RIGHTWARDS ARROW */
internal const val XK_downarrow = 0x08fe  /* U+2193 DOWNWARDS ARROW */
internal const val XK_BackSpace = 0xff08  /* Back space, back char */
internal const val XK_Tab = 0xff09
internal const val XK_Linefeed = 0xff0a  /* Linefeed, LF */
internal const val XK_Clear = 0xff0b
internal const val XK_Return = 0xff0d  /* Return, enter */
internal const val XK_Pause = 0xff13  /* Pause, hold */
internal const val XK_Scroll_Lock = 0xff14
internal const val XK_Sys_Req = 0xff15
internal const val XK_Escape = 0xff1b
internal const val XK_Delete = 0xffff  /* Delete, rubout */

internal const val XK_Home                          = 0xff50
internal const val XK_Left                          = 0xff51  /* Move left, left arrow */
internal const val XK_Up                            = 0xff52  /* Move up, up arrow */
internal const val XK_Right                         = 0xff53  /* Move right, right arrow */
internal const val XK_Down                          = 0xff54  /* Move down, down arrow */
internal const val XK_Prior                         = 0xff55  /* Prior, previous */
internal const val XK_Page_Up                       = 0xff55
internal const val XK_Next                          = 0xff56  /* Next */
internal const val XK_Page_Down                     = 0xff56
internal const val XK_End                           = 0xff57  /* EOL */
internal const val XK_Begin                         = 0xff58  /* BOL */
internal const val XK_Select                        = 0xff60  /* Select, mark */
internal const val XK_Print                         = 0xff61
internal const val XK_Execute                       = 0xff62  /* Execute, run, do */
internal const val XK_Insert                        = 0xff63  /* Insert, insert here */
internal const val XK_Undo                          = 0xff65
internal const val XK_Redo                          = 0xff66  /* Redo, again */
internal const val XK_Menu                          = 0xff67
internal const val XK_Find                          = 0xff68  /* Find, search */
internal const val XK_Cancel                        = 0xff69  /* Cancel, stop, abort, exit */
internal const val XK_Help                          = 0xff6a  /* Help */
internal const val XK_Break                         = 0xff6b
internal const val XK_Mode_switch                   = 0xff7e  /* Character set switch */
internal const val XK_script_switch                 = 0xff7e  /* Alias for mode_switch */
internal const val XK_Num_Lock                      = 0xff7f
internal const val XK_KP_Space                      = 0xff80  /* Space */
internal const val XK_KP_Tab                        = 0xff89
internal const val XK_KP_Enter                      = 0xff8d  /* Enter */
internal const val XK_KP_F1                         = 0xff91  /* PF1, KP_A, ... */
internal const val XK_KP_F2                         = 0xff92
internal const val XK_KP_F3                         = 0xff93
internal const val XK_KP_F4                         = 0xff94
internal const val XK_KP_Home                       = 0xff95
internal const val XK_KP_Left                       = 0xff96
internal const val XK_KP_Up                         = 0xff97
internal const val XK_KP_Right                      = 0xff98
internal const val XK_KP_Down                       = 0xff99
internal const val XK_KP_Prior                      = 0xff9a
internal const val XK_KP_Page_Up                    = 0xff9a
internal const val XK_KP_Next                       = 0xff9b
internal const val XK_KP_Page_Down                  = 0xff9b
internal const val XK_KP_End                        = 0xff9c
internal const val XK_KP_Begin                      = 0xff9d
internal const val XK_KP_Insert                     = 0xff9e
internal const val XK_KP_Delete                     = 0xff9f
internal const val XK_KP_Equal                      = 0xffbd  /* Equals */
internal const val XK_KP_Multiply                   = 0xffaa
internal const val XK_KP_Add                        = 0xffab
internal const val XK_KP_Separator                  = 0xffac  /* Separator, often comma */
internal const val XK_KP_Subtract                   = 0xffad
internal const val XK_KP_Decimal                    = 0xffae
internal const val XK_KP_Divide                     = 0xffaf
internal const val XK_KP_0                          = 0xffb0
internal const val XK_KP_1                          = 0xffb1
internal const val XK_KP_2                          = 0xffb2
internal const val XK_KP_3                          = 0xffb3
internal const val XK_KP_4                          = 0xffb4
internal const val XK_KP_5                          = 0xffb5
internal const val XK_KP_6                          = 0xffb6
internal const val XK_KP_7                          = 0xffb7
internal const val XK_KP_8                          = 0xffb8
internal const val XK_KP_9                          = 0xffb9
internal const val XK_F1                            = 0xffbe
internal const val XK_F2                            = 0xffbf
internal const val XK_F3                            = 0xffc0
internal const val XK_F4                            = 0xffc1
internal const val XK_F5                            = 0xffc2
internal const val XK_F6                            = 0xffc3
internal const val XK_F7                            = 0xffc4
internal const val XK_F8                            = 0xffc5
internal const val XK_F9                            = 0xffc6
internal const val XK_F10                           = 0xffc7
internal const val XK_F11                           = 0xffc8
internal const val XK_L1                            = 0xffc8
internal const val XK_F12                           = 0xffc9
internal const val XK_L2                            = 0xffc9
internal const val XK_F13                           = 0xffca
internal const val XK_L3                            = 0xffca
internal const val XK_F14                           = 0xffcb
internal const val XK_L4                            = 0xffcb
internal const val XK_F15                           = 0xffcc
internal const val XK_L5                            = 0xffcc
internal const val XK_F16                           = 0xffcd
internal const val XK_L6                            = 0xffcd
internal const val XK_F17                           = 0xffce
internal const val XK_L7                            = 0xffce
internal const val XK_F18                           = 0xffcf
internal const val XK_L8                            = 0xffcf
internal const val XK_F19                           = 0xffd0
internal const val XK_L9                            = 0xffd0
internal const val XK_F20                           = 0xffd1
internal const val XK_L10                           = 0xffd1
internal const val XK_F21                           = 0xffd2
internal const val XK_R1                            = 0xffd2
internal const val XK_F22                           = 0xffd3
internal const val XK_R2                            = 0xffd3
internal const val XK_F23                           = 0xffd4
internal const val XK_R3                            = 0xffd4
internal const val XK_F24                           = 0xffd5
internal const val XK_R4                            = 0xffd5
internal const val XK_F25                           = 0xffd6
internal const val XK_R5                            = 0xffd6
internal const val XK_F26                           = 0xffd7
internal const val XK_R6                            = 0xffd7
internal const val XK_F27                           = 0xffd8
internal const val XK_R7                            = 0xffd8
internal const val XK_F28                           = 0xffd9
internal const val XK_R8                            = 0xffd9
internal const val XK_F29                           = 0xffda
internal const val XK_R9                            = 0xffda
internal const val XK_F30                           = 0xffdb
internal const val XK_R10                           = 0xffdb
internal const val XK_F31                           = 0xffdc
internal const val XK_R11                           = 0xffdc
internal const val XK_F32                           = 0xffdd
internal const val XK_R12                           = 0xffdd
internal const val XK_F33                           = 0xffde
internal const val XK_R13                           = 0xffde
internal const val XK_F34                           = 0xffdf
internal const val XK_R14                           = 0xffdf
internal const val XK_F35                           = 0xffe0
internal const val XK_R15                           = 0xffe0
internal const val XK_Shift_L                       = 0xffe1  /* Left shift */
internal const val XK_Shift_R                       = 0xffe2  /* Right shift */
internal const val XK_Control_L                     = 0xffe3  /* Left control */
internal const val XK_Control_R                     = 0xffe4  /* Right control */
internal const val XK_Caps_Lock                     = 0xffe5  /* Caps lock */
internal const val XK_Shift_Lock                    = 0xffe6  /* Shift lock */
internal const val XK_Meta_L                        = 0xffe7  /* Left meta */
internal const val XK_Meta_R                        = 0xffe8  /* Right meta */
internal const val XK_Alt_L                         = 0xffe9  /* Left alt */
internal const val XK_Alt_R                         = 0xffea  /* Right alt */
internal const val XK_Super_L                       = 0xffeb  /* Left super */
internal const val XK_Super_R                       = 0xffec  /* Right super */
internal const val XK_Hyper_L                       = 0xffed  /* Left hyper */
internal const val XK_Hyper_R                       = 0xffee  /* Right hyper */


internal val XK_KeyMap: IntMap<Key> by lazy {
    IntMap<Key>().apply {
        this[XK_space] = Key.SPACE
        this[XK_exclam] = Key.UNKNOWN
        this[XK_quotedbl] = Key.UNKNOWN
        this[XK_numbersign] = Key.UNKNOWN
        this[XK_dollar] = Key.UNKNOWN
        this[XK_percent] = Key.UNKNOWN
        this[XK_ampersand] = Key.UNKNOWN
        this[XK_apostrophe] = Key.APOSTROPHE
        this[XK_quoteright] = Key.UNKNOWN
        this[XK_parenleft] = Key.UNKNOWN
        this[XK_parenright] = Key.UNKNOWN
        this[XK_asterisk] = Key.UNKNOWN
        this[XK_plus] = Key.KP_ADD
        this[XK_comma] = Key.COMMA
        this[XK_minus] = Key.MINUS
        this[XK_period] = Key.PERIOD
        this[XK_slash] = Key.SLASH
        this[XK_0] = Key.N0
        this[XK_1] = Key.N1
        this[XK_2] = Key.N2
        this[XK_3] = Key.N3
        this[XK_4] = Key.N4
        this[XK_5] = Key.N5
        this[XK_6] = Key.N6
        this[XK_7] = Key.N7
        this[XK_8] = Key.N8
        this[XK_9] = Key.N9
        this[XK_colon] = Key.UNKNOWN
        this[XK_semicolon] = Key.SEMICOLON
        this[XK_less] = Key.UNKNOWN
        this[XK_equal] = Key.EQUAL
        this[XK_greater] = Key.UNKNOWN
        this[XK_question] = Key.UNKNOWN
        this[XK_at] = Key.UNKNOWN
        this[XK_A] = Key.A
        this[XK_B] = Key.B
        this[XK_C] = Key.C
        this[XK_D] = Key.D
        this[XK_E] = Key.E
        this[XK_F] = Key.F
        this[XK_G] = Key.G
        this[XK_H] = Key.H
        this[XK_I] = Key.I
        this[XK_J] = Key.J
        this[XK_K] = Key.K
        this[XK_L] = Key.L
        this[XK_M] = Key.M
        this[XK_N] = Key.N
        this[XK_O] = Key.O
        this[XK_P] = Key.P
        this[XK_Q] = Key.Q
        this[XK_R] = Key.R
        this[XK_S] = Key.S
        this[XK_T] = Key.T
        this[XK_U] = Key.U
        this[XK_V] = Key.V
        this[XK_W] = Key.W
        this[XK_X] = Key.X
        this[XK_Y] = Key.Y
        this[XK_Z] = Key.Z
        this[XK_bracketleft] = Key.UNKNOWN
        this[XK_backslash] = Key.BACKSLASH
        this[XK_bracketright] = Key.UNKNOWN
        this[XK_asciicircum] = Key.UNKNOWN
        this[XK_underscore] = Key.UNKNOWN
        this[XK_grave] = Key.UNKNOWN
        this[XK_quoteleft] = Key.UNKNOWN
        this[XK_a] = Key.A
        this[XK_b] = Key.B
        this[XK_c] = Key.C
        this[XK_d] = Key.D
        this[XK_e] = Key.E
        this[XK_f] = Key.F
        this[XK_g] = Key.G
        this[XK_h] = Key.H
        this[XK_i] = Key.I
        this[XK_j] = Key.J
        this[XK_k] = Key.K
        this[XK_l] = Key.L
        this[XK_m] = Key.M
        this[XK_n] = Key.N
        this[XK_o] = Key.O
        this[XK_p] = Key.P
        this[XK_q] = Key.Q
        this[XK_r] = Key.R
        this[XK_s] = Key.S
        this[XK_t] = Key.T
        this[XK_u] = Key.U
        this[XK_v] = Key.V
        this[XK_w] = Key.W
        this[XK_x] = Key.X
        this[XK_y] = Key.Y
        this[XK_z] = Key.Z
        this[XK_leftarrow] = Key.LEFT
        this[XK_uparrow] = Key.UP
        this[XK_rightarrow] = Key.RIGHT
        this[XK_downarrow] = Key.DOWN
        this[XK_BackSpace] = Key.BACKSPACE
        this[XK_Tab] = Key.TAB
        this[XK_Linefeed] = Key.UNKNOWN
        this[XK_Clear] = Key.CLEAR
        this[XK_Return] = Key.RETURN
        this[XK_Pause] = Key.PAUSE
        this[XK_Scroll_Lock] = Key.SCROLL_LOCK
        this[XK_Sys_Req] = Key.UNKNOWN
        this[XK_Escape] = Key.ESCAPE
        this[XK_Delete] = Key.DELETE
        this[XK_Home] = Key.HOME
        this[XK_Left] = Key.LEFT
        this[XK_Up] = Key.UP
        this[XK_Right] = Key.RIGHT
        this[XK_Down] = Key.DOWN
        this[XK_Prior] = Key.UNKNOWN
        this[XK_Page_Up] = Key.PAGE_UP
        this[XK_Next] = Key.UNKNOWN
        this[XK_Page_Down] = Key.PAGE_DOWN
        this[XK_End] = Key.END
        this[XK_Begin] = Key.UNKNOWN
        this[XK_Select] = Key.UNKNOWN
        this[XK_Print] = Key.PRINT_SCREEN
        this[XK_Execute] = Key.UNKNOWN
        this[XK_Insert] = Key.INSERT
        this[XK_Undo] = Key.UNKNOWN
        this[XK_Redo] = Key.UNKNOWN
        this[XK_Menu] = Key.MENU
        this[XK_Find] = Key.UNKNOWN
        this[XK_Cancel] = Key.CANCEL
        this[XK_Help] = Key.HELP
        this[XK_Break] = Key.UNKNOWN
        this[XK_Mode_switch] = Key.UNKNOWN
        this[XK_script_switch] = Key.UNKNOWN
        this[XK_Num_Lock] = Key.NUM_LOCK
        this[XK_KP_Space] = Key.UNKNOWN
        this[XK_KP_Tab] = Key.UNKNOWN
        this[XK_KP_Enter] = Key.KP_ENTER
        this[XK_KP_F1] = Key.F1
        this[XK_KP_F2] = Key.F2
        this[XK_KP_F3] = Key.F3
        this[XK_KP_F4] = Key.F4
        this[XK_KP_Home] = Key.HOME
        this[XK_KP_Left] = Key.KP_LEFT
        this[XK_KP_Up] = Key.KP_UP
        this[XK_KP_Right] = Key.KP_RIGHT
        this[XK_KP_Down] = Key.KP_DOWN
        this[XK_KP_Prior] = Key.UNKNOWN
        this[XK_KP_Page_Up] = Key.UNKNOWN
        this[XK_KP_Next] = Key.UNKNOWN
        this[XK_KP_Page_Down] = Key.UNKNOWN
        this[XK_KP_End] = Key.END
        this[XK_KP_Begin] = Key.HOME
        this[XK_KP_Insert] = Key.INSERT
        this[XK_KP_Delete] = Key.DELETE
        this[XK_KP_Equal] = Key.KP_EQUAL
        this[XK_KP_Multiply] = Key.KP_MULTIPLY
        this[XK_KP_Add] = Key.KP_ADD
        this[XK_KP_Separator] = Key.KP_SEPARATOR
        this[XK_KP_Subtract] = Key.KP_SUBTRACT
        this[XK_KP_Decimal] = Key.KP_DECIMAL
        this[XK_KP_Divide] = Key.KP_DIVIDE
        this[XK_KP_0] = Key.KP_0
        this[XK_KP_1] = Key.KP_1
        this[XK_KP_2] = Key.KP_2
        this[XK_KP_3] = Key.KP_3
        this[XK_KP_4] = Key.KP_4
        this[XK_KP_5] = Key.KP_5
        this[XK_KP_6] = Key.KP_6
        this[XK_KP_7] = Key.KP_7
        this[XK_KP_8] = Key.KP_8
        this[XK_KP_9] = Key.KP_9
        this[XK_F1] = Key.F1
        this[XK_F2] = Key.F2
        this[XK_F3] = Key.F3
        this[XK_F4] = Key.F4
        this[XK_F5] = Key.F5
        this[XK_F6] = Key.F6
        this[XK_F7] = Key.F7
        this[XK_F8] = Key.F8
        this[XK_F9] = Key.F9
        this[XK_F10] = Key.F10
        this[XK_F11] = Key.F11
        this[XK_F12] = Key.F12
        this[XK_F13] = Key.F13
        this[XK_F14] = Key.F14
        this[XK_F15] = Key.F15
        this[XK_F16] = Key.F16
        this[XK_F17] = Key.F17
        this[XK_F18] = Key.F18
        this[XK_F19] = Key.F19
        this[XK_F20] = Key.F20
        this[XK_F21] = Key.F21
        this[XK_F22] = Key.F22
        this[XK_F23] = Key.F23
        this[XK_F24] = Key.F24
        this[XK_F25] = Key.F25
        this[XK_F26] = Key.UNKNOWN
        this[XK_F27] = Key.UNKNOWN
        this[XK_F28] = Key.UNKNOWN
        this[XK_F29] = Key.UNKNOWN
        this[XK_F30] = Key.UNKNOWN
        this[XK_F31] = Key.UNKNOWN
        this[XK_F32] = Key.UNKNOWN
        this[XK_F33] = Key.UNKNOWN
        this[XK_F34] = Key.UNKNOWN
        this[XK_F35] = Key.UNKNOWN

        this[XK_R1] = Key.UNKNOWN
        this[XK_R2] = Key.UNKNOWN
        this[XK_R3] = Key.UNKNOWN
        this[XK_R4] = Key.UNKNOWN
        this[XK_R5] = Key.UNKNOWN
        this[XK_R6] = Key.UNKNOWN
        this[XK_R7] = Key.UNKNOWN
        this[XK_R8] = Key.UNKNOWN
        this[XK_R9] = Key.UNKNOWN
        this[XK_R10] = Key.UNKNOWN
        this[XK_R11] = Key.UNKNOWN
        this[XK_R12] = Key.UNKNOWN
        this[XK_R13] = Key.UNKNOWN
        this[XK_R14] = Key.UNKNOWN
        this[XK_R15] = Key.UNKNOWN

        this[XK_L1] = Key.UNKNOWN
        this[XK_L2] = Key.UNKNOWN
        this[XK_L3] = Key.UNKNOWN
        this[XK_L4] = Key.UNKNOWN
        this[XK_L5] = Key.UNKNOWN
        this[XK_L6] = Key.UNKNOWN
        this[XK_L7] = Key.UNKNOWN
        this[XK_L8] = Key.UNKNOWN
        this[XK_L9] = Key.UNKNOWN
        this[XK_L10] = Key.UNKNOWN

        this[XK_Shift_L] = Key.LEFT_SHIFT
        this[XK_Shift_R] = Key.RIGHT_SHIFT
        this[XK_Control_L] = Key.LEFT_CONTROL
        this[XK_Control_R] = Key.RIGHT_CONTROL
        this[XK_Caps_Lock] = Key.CAPS_LOCK
        this[XK_Shift_Lock] = Key.CAPS_LOCK
        this[XK_Meta_L] = Key.LEFT_SUPER
        this[XK_Meta_R] = Key.RIGHT_SUPER
        this[XK_Alt_L] = Key.LEFT_ALT
        this[XK_Alt_R] = Key.RIGHT_ALT
        this[XK_Super_L] = Key.LEFT_SUPER
        this[XK_Super_R] = Key.RIGHT_SUPER
        this[XK_Hyper_L] = Key.LEFT_SUPER
        this[XK_Hyper_R] = Key.RIGHT_SUPER
    }
}

internal fun KStructure.display() = pointer<X11.Display?>()
internal fun KStructure.window() = pointer<X11.Window?>()

internal class XConfigureEvent(p: Pointer? = null) : KStructure(p) {
    var type by int()
    var serial by nativeLong()
    var send_event by int()
    var display by display()
    var event by window()
    var window by window()
    var x by int()
    var y by int()
    var width by int()
    var height by int()
    var border_width by int()
    var above by window()
    var override_redirect by int()
}

internal class XKeyEvent(p: Pointer? = null) : KStructure(p) {
    var type by int()
    var serial by nativeLong()
    var send_event by int()
    var display by pointer<X11.Display>()
    var window by pointer<X11.Window>()
    var root by pointer<X11.Window>()
    var subwindow by pointer<X11.Window>()
    var time by nativeLong()
    var x by int()
    var y by int()
    var x_root by int()
    var y_root by int()
    var state by int()
    var keycode by int()
    var same_screen by int()
}

internal class MyXMotionEvent(p: Pointer? = null) : KStructure(p) {
    var type by int()
    var serial by nativeLong()
    var send_event by int()
    var display by pointer<X11.Display?>()
    var window by pointer<X11.Window?>()
    var root by pointer<X11.Window?>()
    var subwindow by pointer<X11.Window?>()
    var time by nativeLong()
    var x by int()
    var y by int()
    var x_root by int()
    var y_root by int()
    var state by int()
    var button by int()
    var same_screen by int()
}

object X :
    X11Impl by Native.load(System.getenv("X11LIB_PATH") ?: "libX11", X11Impl::class.java),
    GL by Native.load(System.getenv("GLLIB_PATH") ?: "libGL", GL::class.java)

internal interface X11Impl : X11 {
    fun XCreateWindow(
        display: X11.Display, parent: X11.Window,
        x: Int, y: Int, width: Int, height: Int,
        border_width: Int, depth: Int, clazz: Int, visual: X11.Visual,
        valuemask: NativeLong,
        attributes: X11.XSetWindowAttributes
    ): X11.Window
    //Window XCreateSimpleWindow(Display display, Window parent, int x, int y,
    //int width, int height, int border_width,
    //int border, int background);
    fun XDefaultGC(display: X11.Display?, scn: Int): X11.GC?
    fun XBlackPixel(display: X11.Display?, scn: Int): Int
    fun XWhitePixel(display: X11.Display?, scn: Int): Int
    fun XStoreName(display: X11.Display?, w: X11.Window?, window_name: String)
    fun XSetIconName(display: X11.Display?, w: X11.Window?, window_name: String)
    fun XLookupKeysym(e: X11.XEvent?, i: Int): Int
    fun XDisplayString(display: X11.Display?): String?
    fun XSynchronize(display: X11.Display?, value: Boolean)
}

internal interface GL : INativeGL, Library {
    fun glXQueryDrawable(dpy: X11.Display, draw: X11.Drawable?, attribute: Int, value: IntByReference): Int
    fun glXQueryContext(dpy: X11.Display, ctx: GLXContext?, attribute: Int, value: Pointer): Int

    //fun glClearColor(r: Float, g: Float, b: Float, a: Float)
    //fun glClear(flags: Int)
    //fun glGetString(id: Int): String
    //fun glViewport(x: Int, y: Int, width: Int, height: Int)
    fun glXChooseVisual(display: X11.Display?, screen: Int, attribList: LongArray): XVisualInfo?
    fun glXChooseVisual(display: X11.Display?, screen: Int, attribList: IntArray): XVisualInfo?
    fun glXChooseVisual(display: X11.Display?, screen: Int, attribList: Pointer): XVisualInfo?
    fun glXCreateContext(display: X11.Display?, vis: XVisualInfo?, shareList: GLXContext?, direct: Boolean): GLXContext?
    fun glXDestroyContext(display: X11.Display?, context: GLXContext?): Unit
    fun glXMakeCurrent(display: X11.Display?, drawable: X11.Drawable?, ctx: GLXContext?): Boolean
    fun glXMakeContextCurrent(display: X11.Display?, draw: X11.Drawable?, read: X11.Drawable?, ctx: GLXContext?): Boolean
    fun glXSwapBuffers(display: X11.Display?, drawable: X11.Drawable?)
    fun glXGetProcAddress(name: String): Pointer
    fun glXGetCurrentDrawable(): Pointer
    fun glXGetCurrentDisplay(): X11.Display?

    //fun glXChooseVisual(display: X11.Display, screen: Int, attribList: IntArray): XVisualInfo
    //fun glXCreateContext(display: X11.Display, vis: XVisualInfo, shareList: GLXContext?, direct: Boolean): GLXContext
    //fun glXMakeCurrent(display: X11.Display, drawable: X11.Window, ctx: GLXContext?): Boolean
    //fun glXSwapBuffers(display: X11.Display, drawable: X11.Window)


    companion object {
        const val GL_DEPTH_BUFFER_BIT = 0x00000100
        const val GL_STENCIL_BUFFER_BIT = 0x00000400
        const val GL_COLOR_BUFFER_BIT = 0x00004000

        const val WGL_CONTEXT_MAJOR_VERSION_ARB = 0x2091
        const val WGL_CONTEXT_MINOR_VERSION_ARB = 0x2092

        const val GL_VENDOR = 0x1F00
        const val GL_RENDERER = 0x1F01
        const val GL_VERSION = 0x1F02
        const val GL_SHADING_LANGUAGE_VERSION = 0x8B8C
        const val GL_EXTENSIONS = 0x1F03
    }
}

//internal object X11KmlGl : NativeKgl(X)

object X11KmlGl : NativeKgl(X)
