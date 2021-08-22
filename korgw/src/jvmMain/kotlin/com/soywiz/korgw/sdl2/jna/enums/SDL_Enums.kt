package com.soywiz.korgw.sdl2.jna.enums

enum class SDL_EventType(val value: Int) {
    FIRSTEVENT(0),

    QUIT(0x100),
    APP_TERMINATING(0x101),
    APP_LOWMEMORY(0x102),
    APP_WILLENTERBACKGROUND(0x103),
    APP_DIDENTERBACKGROUND(0x104),
    APP_WILLENTERFOREGROUND(0x105),
    APP_DIDENTERFOREGROUND(0x106),
    LOCALECHANGED(0x107),

    DISPLAYEVENT(0x150),

    WINDOWEVENT(0x200),
    SYSWMEVENT(0x201),

    KEYDOWN(0x300),
    KEYUP(0x301),
    TEXTEDITING(0x302),
    TEXTINPUT(0x303),
    KEYMAPCHANGED(0x304),

    MOUSEMOTION(0x400),
    MOUSEBUTTONDOWN(0x401),
    MOUSEBUTTONUP(0x402),
    MOUSEWHEEL(0x403),

    JOYAXISMOTION(0x600),
    JOYBALLMOTION(0x601),
    JOYHATMOTION(0x602),
    JOYBUTTONDOWN(0x603),
    JOYBUTTONUP(0x604),
    JOYDEVICEADDED(0x605),
    JOYDEVICEREMOVED(0x606),

    CONTROLLERAXISMOTION(0x650),
    CONTROLLERBUTTONDOWN(0x651),
    CONTROLLERBUTTONUP(0x652),
    CONTROLLERDEVICEADDED(0x653),
    CONTROLLERDEVICEREMOVED(0x654),
    CONTROLLERDEVICEREMAPPED(0x655),
    CONTROLLERTOUCHPADDOWN(0x656),
    CONTROLLERTOUCHPADMOTION(0x657),
    CONTROLLERTOUCHPADUP(0x658),
    CONTROLLERSENSORUPDATE(0x659),

    FINGERDOWN(0x700),
    FINGERUP(0x701),
    FINGERMOTION(0x702),

    DOLLARGESTURE(0x800),
    DOLLARRECORD(0x801),
    MULTIGESTURE(0x802),

    CLIPBOARDUPDATE(0x900),

    DROPFILE(0x1000),
    DROPTEXT(0x1001),
    DROPBEGIN(0x1002),
    DROPCOMPLETE(0x1003),

    AUDIODEVICEADDED(0x1100),
    AUDIODEVICEREMOVED(0x1101),

    SENSORUPDATE(0x1200),

    RENDER_TARGETS_RESET(0x2000),
    RENDER_DEVICE_RESET(0x2001),

    USEREVENT(0x8000),
    LASTEVENT(0xFFFF);

    companion object {
        private val sLookup = mutableMapOf<Int, SDL_EventType>()

        init {
            values().forEach { sLookup[it.value] = it }
        }

        fun fromInt(value: Int): SDL_EventType = sLookup[value] as SDL_EventType
    }
}

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

enum class SDL_KeyCode(val value: Int) {
    UNKNOWN(0),

    RETURN('\r'.code),
    ESCAPE(27),
    BACKSPACE('\b'.code),
    TAB('\t'.code),
    SPACE(' '.code),
    EXCLAIM('!'.code),
    QUOTEDBL('"'.code),
    HASH('#'.code),
    PERCENT('%'.code),
    DOLLAR('$'.code),
    AMPERSAND('&'.code),
    QUOTE('\''.code),
    LEFTPAREN('('.code),
    RIGHTPAREN(')'.code),
    ASTERISK('*'.code),
    PLUS('+'.code),
    COMMA(','.code),
    MINUS('-'.code),
    PERIOD('.'.code),
    SLASH('/'.code),
    ZERO('0'.code),
    ONE('1'.code),
    TWO('2'.code),
    THREE('3'.code),
    FOUR('4'.code),
    FIVE('5'.code),
    SIX('6'.code),
    SEVEN('7'.code),
    EIGHT('8'.code),
    NINE('9'.code),
    COLON(':'.code),
    SEMICOLON(';'.code),
    LESS('<'.code),
    EQUALS('='.code),
    GREATER('>'.code),
    QUESTION('?'.code),
    AT('@'.code),

    LEFTBRACKET('['.code),
    BACKSLASH('\\'.code),
    RIGHTBRACKET(']'.code),
    CARET('^'.code),
    UNDERSCORE('_'.code),
    BACKQUOTE('`'.code),
    a('a'.code),
    b('b'.code),
    c('c'.code),
    d('d'.code),
    e('e'.code),
    f('f'.code),
    g('g'.code),
    h('h'.code),
    i('i'.code),
    j('j'.code),
    k('k'.code),
    l('l'.code),
    m('m'.code),
    n('n'.code),
    o('o'.code),
    p('p'.code),
    q('q'.code),
    r('r'.code),
    s('s'.code),
    t('t'.code),
    u('u'.code),
    v('v'.code),
    w('w'.code),
    x('x'.code),
    y('y'.code),
    z('z'.code),

    CAPSLOCK(SDL_Scancode.CAPSLOCK.keycode),

    F1(SDL_Scancode.F1.keycode),
    F2(SDL_Scancode.F2.keycode),
    F3(SDL_Scancode.F3.keycode),
    F4(SDL_Scancode.F4.keycode),
    F5(SDL_Scancode.F5.keycode),
    F6(SDL_Scancode.F6.keycode),
    F7(SDL_Scancode.F7.keycode),
    F8(SDL_Scancode.F8.keycode),
    F9(SDL_Scancode.F9.keycode),
    F10(SDL_Scancode.F10.keycode),
    F11(SDL_Scancode.F11.keycode),
    F12(SDL_Scancode.F12.keycode),

    PRINTSCREEN(SDL_Scancode.PRINTSCREEN.keycode),
    SCROLLLOCK(SDL_Scancode.SCROLLLOCK.keycode),
    PAUSE(SDL_Scancode.PAUSE.keycode),
    INSERT(SDL_Scancode.INSERT.keycode),
    HOME(SDL_Scancode.HOME.keycode),
    PAGEUP(SDL_Scancode.PAGEUP.keycode),
    DELETE(127),
    END(SDL_Scancode.END.keycode),
    PAGEDOWN(SDL_Scancode.PAGEDOWN.keycode),
    RIGHT(SDL_Scancode.RIGHT.keycode),
    LEFT(SDL_Scancode.LEFT.keycode),
    DOWN(SDL_Scancode.DOWN.keycode),
    UP(SDL_Scancode.UP.keycode),

    NUMLOCKCLEAR(SDL_Scancode.NUMLOCKCLEAR.keycode),
    KP_DIVIDE(SDL_Scancode.KP_DIVIDE.keycode),
    KP_MULTIPLY(SDL_Scancode.KP_MULTIPLY.keycode),
    KP_MINUS(SDL_Scancode.KP_MINUS.keycode),
    KP_PLUS(SDL_Scancode.KP_PLUS.keycode),
    KP_ENTER(SDL_Scancode.KP_ENTER.keycode),
    KP_1(SDL_Scancode.KP_1.keycode),
    KP_2(SDL_Scancode.KP_2.keycode),
    KP_3(SDL_Scancode.KP_3.keycode),
    KP_4(SDL_Scancode.KP_4.keycode),
    KP_5(SDL_Scancode.KP_5.keycode),
    KP_6(SDL_Scancode.KP_6.keycode),
    KP_7(SDL_Scancode.KP_7.keycode),
    KP_8(SDL_Scancode.KP_8.keycode),
    KP_9(SDL_Scancode.KP_9.keycode),
    KP_0(SDL_Scancode.KP_0.keycode),
    KP_PERIOD(SDL_Scancode.KP_PERIOD.keycode),

    APPLICATION(SDL_Scancode.APPLICATION.keycode),
    POWER(SDL_Scancode.POWER.keycode),
    KP_EQUALS(SDL_Scancode.KP_EQUALS.keycode),
    F13(SDL_Scancode.F13.keycode),
    F14(SDL_Scancode.F14.keycode),
    F15(SDL_Scancode.F15.keycode),
    F16(SDL_Scancode.F16.keycode),
    F17(SDL_Scancode.F17.keycode),
    F18(SDL_Scancode.F18.keycode),
    F19(SDL_Scancode.F19.keycode),
    F20(SDL_Scancode.F20.keycode),
    F21(SDL_Scancode.F21.keycode),
    F22(SDL_Scancode.F22.keycode),
    F23(SDL_Scancode.F23.keycode),
    F24(SDL_Scancode.F24.keycode),
    EXECUTE(SDL_Scancode.EXECUTE.keycode),
    HELP(SDL_Scancode.HELP.keycode),
    MENU(SDL_Scancode.MENU.keycode),
    SELECT(SDL_Scancode.SELECT.keycode),
    STOP(SDL_Scancode.STOP.keycode),
    AGAIN(SDL_Scancode.AGAIN.keycode),
    UNDO(SDL_Scancode.UNDO.keycode),
    CUT(SDL_Scancode.CUT.keycode),
    COPY(SDL_Scancode.COPY.keycode),
    PASTE(SDL_Scancode.PASTE.keycode),
    FIND(SDL_Scancode.FIND.keycode),
    MUTE(SDL_Scancode.MUTE.keycode),
    VOLUMEUP(SDL_Scancode.VOLUMEUP.keycode),
    VOLUMEDOWN(SDL_Scancode.VOLUMEDOWN.keycode),
    KP_COMMA(SDL_Scancode.KP_COMMA.keycode),
    KP_EQUALSAS400(SDL_Scancode.KP_EQUALSAS400.keycode),

    ALTERASE(SDL_Scancode.ALTERASE.keycode),
    SYSREQ(SDL_Scancode.SYSREQ.keycode),
    CANCEL(SDL_Scancode.CANCEL.keycode),
    CLEAR(SDL_Scancode.CLEAR.keycode),
    PRIOR(SDL_Scancode.PRIOR.keycode),
    RETURN2(SDL_Scancode.RETURN2.keycode),
    SEPARATOR(SDL_Scancode.SEPARATOR.keycode),
    OUT(SDL_Scancode.OUT.keycode),
    OPER(SDL_Scancode.OPER.keycode),
    CLEARAGAIN(SDL_Scancode.CLEARAGAIN.keycode),
    CRSEL(SDL_Scancode.CRSEL.keycode),
    EXSEL(SDL_Scancode.EXSEL.keycode),

    KP_00(SDL_Scancode.KP_00.keycode),
    KP_000(SDL_Scancode.KP_000.keycode),
    THOUSANDSSEPARATOR(SDL_Scancode.THOUSANDSSEPARATOR.keycode),
    DECIMALSEPARATOR(SDL_Scancode.DECIMALSEPARATOR.keycode),
    CURRENCYUNIT(SDL_Scancode.CURRENCYUNIT.keycode),
    CURRENCYSUBUNIT(SDL_Scancode.CURRENCYSUBUNIT.keycode),
    KP_LEFTPAREN(SDL_Scancode.KP_LEFTPAREN.keycode),
    KP_RIGHTPAREN(SDL_Scancode.KP_RIGHTPAREN.keycode),
    KP_LEFTBRACE(SDL_Scancode.KP_LEFTBRACE.keycode),
    KP_RIGHTBRACE(SDL_Scancode.KP_RIGHTBRACE.keycode),
    KP_TAB(SDL_Scancode.KP_TAB.keycode),
    KP_BACKSPACE(SDL_Scancode.KP_BACKSPACE.keycode),
    KP_A(SDL_Scancode.KP_A.keycode),
    KP_B(SDL_Scancode.KP_B.keycode),
    KP_C(SDL_Scancode.KP_C.keycode),
    KP_D(SDL_Scancode.KP_D.keycode),
    KP_E(SDL_Scancode.KP_E.keycode),
    KP_F(SDL_Scancode.KP_F.keycode),
    KP_XOR(SDL_Scancode.KP_XOR.keycode),
    KP_POWER(SDL_Scancode.KP_POWER.keycode),
    KP_PERCENT(SDL_Scancode.KP_PERCENT.keycode),
    KP_LESS(SDL_Scancode.KP_LESS.keycode),
    KP_GREATER(SDL_Scancode.KP_GREATER.keycode),
    KP_AMPERSAND(SDL_Scancode.KP_AMPERSAND.keycode),
    KP_DBLAMPERSAND(SDL_Scancode.KP_DBLAMPERSAND.keycode),
    KP_VERTICALBAR(SDL_Scancode.KP_VERTICALBAR.keycode),
    KP_DBLVERTICALBAR(SDL_Scancode.KP_DBLVERTICALBAR.keycode),
    KP_COLON(SDL_Scancode.KP_COLON.keycode),
    KP_HASH(SDL_Scancode.KP_HASH.keycode),
    KP_SPACE(SDL_Scancode.KP_SPACE.keycode),
    KP_AT(SDL_Scancode.KP_AT.keycode),
    KP_EXCLAM(SDL_Scancode.KP_EXCLAM.keycode),
    KP_MEMSTORE(SDL_Scancode.KP_MEMSTORE.keycode),
    KP_MEMRECALL(SDL_Scancode.KP_MEMRECALL.keycode),
    KP_MEMCLEAR(SDL_Scancode.KP_MEMCLEAR.keycode),
    KP_MEMADD(SDL_Scancode.KP_MEMADD.keycode),
    KP_MEMSUBTRACT(SDL_Scancode.KP_MEMSUBTRACT.keycode),
    KP_MEMMULTIPLY(SDL_Scancode.KP_MEMMULTIPLY.keycode),
    KP_MEMDIVIDE(SDL_Scancode.KP_MEMDIVIDE.keycode),
    KP_PLUSMINUS(SDL_Scancode.KP_PLUSMINUS.keycode),
    KP_CLEAR(SDL_Scancode.KP_CLEAR.keycode),
    KP_CLEARENTRY(SDL_Scancode.KP_CLEARENTRY.keycode),
    KP_BINARY(SDL_Scancode.KP_BINARY.keycode),
    KP_OCTAL(SDL_Scancode.KP_OCTAL.keycode),
    KP_DECIMAL(SDL_Scancode.KP_DECIMAL.keycode),
    KP_HEXADECIMAL(SDL_Scancode.KP_HEXADECIMAL.keycode),

    LCTRL(SDL_Scancode.LCTRL.keycode),
    LSHIFT(SDL_Scancode.LSHIFT.keycode),
    LALT(SDL_Scancode.LALT.keycode),
    LGUI(SDL_Scancode.LGUI.keycode),
    RCTRL(SDL_Scancode.RCTRL.keycode),
    RSHIFT(SDL_Scancode.RSHIFT.keycode),
    RALT(SDL_Scancode.RALT.keycode),
    RGUI(SDL_Scancode.RGUI.keycode),

    MODE(SDL_Scancode.MODE.keycode),

    AUDIONEXT(SDL_Scancode.AUDIONEXT.keycode),
    AUDIOPREV(SDL_Scancode.AUDIOPREV.keycode),
    AUDIOSTOP(SDL_Scancode.AUDIOSTOP.keycode),
    AUDIOPLAY(SDL_Scancode.AUDIOPLAY.keycode),
    AUDIOMUTE(SDL_Scancode.AUDIOMUTE.keycode),
    MEDIASELECT(SDL_Scancode.MEDIASELECT.keycode),
    WWW(SDL_Scancode.WWW.keycode),
    MAIL(SDL_Scancode.MAIL.keycode),
    CALCULATOR(SDL_Scancode.CALCULATOR.keycode),
    COMPUTER(SDL_Scancode.COMPUTER.keycode),
    AC_SEARCH(SDL_Scancode.AC_SEARCH.keycode),
    AC_HOME(SDL_Scancode.AC_HOME.keycode),
    AC_BACK(SDL_Scancode.AC_BACK.keycode),
    AC_FORWARD(SDL_Scancode.AC_FORWARD.keycode),
    AC_STOP(SDL_Scancode.AC_STOP.keycode),
    AC_REFRESH(SDL_Scancode.AC_REFRESH.keycode),
    AC_BOOKMARKS(SDL_Scancode.AC_BOOKMARKS.keycode),

    BRIGHTNESSDOWN(SDL_Scancode.BRIGHTNESSDOWN.keycode),
    BRIGHTNESSUP(SDL_Scancode.BRIGHTNESSUP.keycode),
    DISPLAYSWITCH(SDL_Scancode.DISPLAYSWITCH.keycode),
    KBDILLUMTOGGLE(SDL_Scancode.KBDILLUMTOGGLE.keycode),
    KBDILLUMDOWN(SDL_Scancode.KBDILLUMDOWN.keycode),
    KBDILLUMUP(SDL_Scancode.KBDILLUMUP.keycode),
    EJECT(SDL_Scancode.EJECT.keycode),
    SLEEP(SDL_Scancode.SLEEP.keycode),
    APP1(SDL_Scancode.APP1.keycode),
    APP2(SDL_Scancode.APP2.keycode),

    AUDIOREWIND(SDL_Scancode.AUDIOREWIND.keycode),
    AUDIOFASTFORWARD(SDL_Scancode.AUDIOFASTFORWARD.keycode);

    companion object {
        private val lookup = mutableMapOf<Int, SDL_KeyCode>()

        init {
            values().forEach { lookup[it.value] = it }
        }

        fun fromInt(value: Int): SDL_KeyCode = lookup[value] ?: UNKNOWN
    }
}

object SDL_MouseButton {
    const val LEFT = 1
    const val MIDDLE = 2
    const val RIGHT = 3
    const val X1 = 4
    const val X2 = 5
}

object SDL_RendererFlags {
    const val SOFTWARE = 0x00000001
    const val ACCELERATED = 0x00000002
    const val PRESENTVSYNC = 0x00000004
    const val TARGETTEXTURE = 0x00000008
}

enum class SDL_Scancode(val value: Int) {
    UNKNOWN(0),

    A(4),
    B(5),
    C(6),
    D(7),
    E(8),
    F(9),
    G(10),
    H(11),
    I(12),
    J(13),
    K(14),
    L(15),
    M(16),
    N(17),
    O(18),
    P(19),
    Q(20),
    R(21),
    S(22),
    T(23),
    U(24),
    V(25),
    W(26),
    X(27),
    Y(28),
    Z(29),

    ONE(30),
    TWO(31),
    THREE(32),
    FOUR(33),
    FIVE(34),
    SIX(35),
    SEVEN(36),
    EIGHT(37),
    NINE(38),
    ZERO(39),

    RETURN(40),
    ESCAPE(41),
    BACKSPACE(42),
    TAB(43),
    SPACE(44),

    MINUS(45),
    EQUALS(46),
    LEFTBRACKET(47),
    RIGHTBRACKET(48),
    BACKSLASH(49),
    NONUSHASH(50),
    SEMICOLON(51),
    APOSTROPHE(52),
    GRAVE(53),
    COMMA(54),
    PERIOD(55),
    SLASH(56),

    CAPSLOCK(57),

    F1(58),
    F2(59),
    F3(60),
    F4(61),
    F5(62),
    F6(63),
    F7(64),
    F8(65),
    F9(66),
    F10(67),
    F11(68),
    F12(69),

    PRINTSCREEN(70),
    SCROLLLOCK(71),
    PAUSE(72),
    INSERT(73),

    HOME(74),
    PAGEUP(75),
    DELETE(76),
    END(77),
    PAGEDOWN(78),
    RIGHT(79),
    LEFT(80),
    DOWN(81),
    UP(82),

    NUMLOCKCLEAR(83),
    KP_DIVIDE(84),
    KP_MULTIPLY(85),
    KP_MINUS(86),
    KP_PLUS(87),
    KP_ENTER(88),
    KP_1(89),
    KP_2(90),
    KP_3(91),
    KP_4(92),
    KP_5(93),
    KP_6(94),
    KP_7(95),
    KP_8(96),
    KP_9(97),
    KP_0(98),
    KP_PERIOD(99),

    NONUSBACKSLASH(100),
    APPLICATION(101),
    POWER(102),
    KP_EQUALS(103),
    F13(104),
    F14(105),
    F15(106),
    F16(107),
    F17(108),
    F18(109),
    F19(110),
    F20(111),
    F21(112),
    F22(113),
    F23(114),
    F24(115),
    EXECUTE(116),
    HELP(117),
    MENU(118),
    SELECT(119),
    STOP(120),
    AGAIN(121),
    UNDO(122),
    CUT(123),
    COPY(124),
    PASTE(125),
    FIND(126),
    MUTE(127),
    VOLUMEUP(128),
    VOLUMEDOWN(129),
    KP_COMMA(133),
    KP_EQUALSAS400(134),

    INTERNATIONAL1(135),
    INTERNATIONAL2(136),
    INTERNATIONAL3(137),
    INTERNATIONAL4(138),
    INTERNATIONAL5(139),
    INTERNATIONAL6(140),
    INTERNATIONAL7(141),
    INTERNATIONAL8(142),
    INTERNATIONAL9(143),
    LANG1(144),
    LANG2(145),
    LANG3(146),
    LANG4(147),
    LANG5(148),
    LANG6(149),
    LANG7(150),
    LANG8(151),
    LANG9(152),

    ALTERASE(153),
    SYSREQ(154),
    CANCEL(155),
    CLEAR(156),
    PRIOR(157),
    RETURN2(158),
    SEPARATOR(159),
    OUT(160),
    OPER(161),
    CLEARAGAIN(162),
    CRSEL(163),
    EXSEL(164),

    KP_00(176),
    KP_000(177),
    THOUSANDSSEPARATOR(1),
    DECIMALSEPARATOR(179),
    CURRENCYUNIT(180),
    CURRENCYSUBUNIT(181),
    KP_LEFTPAREN(182),
    KP_RIGHTPAREN(183),
    KP_LEFTBRACE(184),
    KP_RIGHTBRACE(185),
    KP_TAB(186),
    KP_BACKSPACE(187),
    KP_A(188),
    KP_B(189),
    KP_C(190),
    KP_D(191),
    KP_E(192),
    KP_F(193),
    KP_XOR(194),
    KP_POWER(195),
    KP_PERCENT(196),
    KP_LESS(197),
    KP_GREATER(198),
    KP_AMPERSAND(199),
    KP_DBLAMPERSAND(200),
    KP_VERTICALBAR(201),
    KP_DBLVERTICALBAR(20),
    KP_COLON(203),
    KP_HASH(204),
    KP_SPACE(205),
    KP_AT(206),
    KP_EXCLAM(207),
    KP_MEMSTORE(208),
    KP_MEMRECALL(209),
    KP_MEMCLEAR(210),
    KP_MEMADD(211),
    KP_MEMSUBTRACT(212),
    KP_MEMMULTIPLY(213),
    KP_MEMDIVIDE(214),
    KP_PLUSMINUS(215),
    KP_CLEAR(216),
    KP_CLEARENTRY(217),
    KP_BINARY(218),
    KP_OCTAL(219),
    KP_DECIMAL(220),
    KP_HEXADECIMAL(221),

    LCTRL(224),
    LSHIFT(225),
    LALT(226),
    LGUI(227),
    RCTRL(228),
    RSHIFT(229),
    RALT(230),
    RGUI(231),

    MODE(257),

    AUDIONEXT(258),
    AUDIOPREV(259),
    AUDIOSTOP(260),
    AUDIOPLAY(261),
    AUDIOMUTE(262),
    MEDIASELECT(263),
    WWW(264),
    MAIL(265),
    CALCULATOR(266),
    COMPUTER(267),
    AC_SEARCH(268),
    AC_HOME(269),
    AC_BACK(270),
    AC_FORWARD(271),
    AC_STOP(272),
    AC_REFRESH(273),
    AC_BOOKMARKS(274),

    BRIGHTNESSDOWN(275),
    BRIGHTNESSUP(276),
    DISPLAYSWITCH(277),
    KBDILLUMTOGGLE(278),
    KBDILLUMDOWN(279),
    KBDILLUMUP(280),
    EJECT(281),
    SLEEP(282),

    APP1(283),
    APP2(284),

    AUDIOREWIND(285),
    AUDIOFASTFORWARD(286);

    val keycode: Int = value or (1 shl 30)
}

enum class SDL_WindowEventID {
    NONE,
    SHOWN,
    HIDDEN,
    EXPOSED,
    MOVED,
    RESIZED,
    SIZE_CHANGED,
    MINIMIZED,
    MAXIMIZED,
    RESTORED,
    ENTER,
    LEAVE,
    FOCUS_GAINED,
    FOCUS_LOST,
    CLOSE,
    TAKE_FOCUS,
    HIT_TEST;

    companion object {
        val lookup = mutableMapOf<Int, SDL_WindowEventID>()

        init {
            values().forEach { lookup[it.ordinal] = it }
        }

        fun fromInt(value: Int) = lookup[value] ?: NONE
    }
}

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
