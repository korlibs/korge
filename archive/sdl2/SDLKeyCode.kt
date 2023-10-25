@file:Suppress("unused", "EnumEntryName", "ClassName")

package korlibs.render.sdl2

enum class SDLKeyCode(val value: Int) {
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

    CAPSLOCK(SDLScancode.CAPSLOCK.keycode),

    F1(SDLScancode.F1.keycode),
    F2(SDLScancode.F2.keycode),
    F3(SDLScancode.F3.keycode),
    F4(SDLScancode.F4.keycode),
    F5(SDLScancode.F5.keycode),
    F6(SDLScancode.F6.keycode),
    F7(SDLScancode.F7.keycode),
    F8(SDLScancode.F8.keycode),
    F9(SDLScancode.F9.keycode),
    F10(SDLScancode.F10.keycode),
    F11(SDLScancode.F11.keycode),
    F12(SDLScancode.F12.keycode),

    PRINTSCREEN(SDLScancode.PRINTSCREEN.keycode),
    SCROLLLOCK(SDLScancode.SCROLLLOCK.keycode),
    PAUSE(SDLScancode.PAUSE.keycode),
    INSERT(SDLScancode.INSERT.keycode),
    HOME(SDLScancode.HOME.keycode),
    PAGEUP(SDLScancode.PAGEUP.keycode),
    DELETE(127),
    END(SDLScancode.END.keycode),
    PAGEDOWN(SDLScancode.PAGEDOWN.keycode),
    RIGHT(SDLScancode.RIGHT.keycode),
    LEFT(SDLScancode.LEFT.keycode),
    DOWN(SDLScancode.DOWN.keycode),
    UP(SDLScancode.UP.keycode),

    NUMLOCKCLEAR(SDLScancode.NUMLOCKCLEAR.keycode),
    KP_DIVIDE(SDLScancode.KP_DIVIDE.keycode),
    KP_MULTIPLY(SDLScancode.KP_MULTIPLY.keycode),
    KP_MINUS(SDLScancode.KP_MINUS.keycode),
    KP_PLUS(SDLScancode.KP_PLUS.keycode),
    KP_ENTER(SDLScancode.KP_ENTER.keycode),
    KP_1(SDLScancode.KP_1.keycode),
    KP_2(SDLScancode.KP_2.keycode),
    KP_3(SDLScancode.KP_3.keycode),
    KP_4(SDLScancode.KP_4.keycode),
    KP_5(SDLScancode.KP_5.keycode),
    KP_6(SDLScancode.KP_6.keycode),
    KP_7(SDLScancode.KP_7.keycode),
    KP_8(SDLScancode.KP_8.keycode),
    KP_9(SDLScancode.KP_9.keycode),
    KP_0(SDLScancode.KP_0.keycode),
    KP_PERIOD(SDLScancode.KP_PERIOD.keycode),

    APPLICATION(SDLScancode.APPLICATION.keycode),
    POWER(SDLScancode.POWER.keycode),
    KP_EQUALS(SDLScancode.KP_EQUALS.keycode),
    F13(SDLScancode.F13.keycode),
    F14(SDLScancode.F14.keycode),
    F15(SDLScancode.F15.keycode),
    F16(SDLScancode.F16.keycode),
    F17(SDLScancode.F17.keycode),
    F18(SDLScancode.F18.keycode),
    F19(SDLScancode.F19.keycode),
    F20(SDLScancode.F20.keycode),
    F21(SDLScancode.F21.keycode),
    F22(SDLScancode.F22.keycode),
    F23(SDLScancode.F23.keycode),
    F24(SDLScancode.F24.keycode),
    EXECUTE(SDLScancode.EXECUTE.keycode),
    HELP(SDLScancode.HELP.keycode),
    MENU(SDLScancode.MENU.keycode),
    SELECT(SDLScancode.SELECT.keycode),
    STOP(SDLScancode.STOP.keycode),
    AGAIN(SDLScancode.AGAIN.keycode),
    UNDO(SDLScancode.UNDO.keycode),
    CUT(SDLScancode.CUT.keycode),
    COPY(SDLScancode.COPY.keycode),
    PASTE(SDLScancode.PASTE.keycode),
    FIND(SDLScancode.FIND.keycode),
    MUTE(SDLScancode.MUTE.keycode),
    VOLUMEUP(SDLScancode.VOLUMEUP.keycode),
    VOLUMEDOWN(SDLScancode.VOLUMEDOWN.keycode),
    KP_COMMA(SDLScancode.KP_COMMA.keycode),
    KP_EQUALSAS400(SDLScancode.KP_EQUALSAS400.keycode),

    ALTERASE(SDLScancode.ALTERASE.keycode),
    SYSREQ(SDLScancode.SYSREQ.keycode),
    CANCEL(SDLScancode.CANCEL.keycode),
    CLEAR(SDLScancode.CLEAR.keycode),
    PRIOR(SDLScancode.PRIOR.keycode),
    RETURN2(SDLScancode.RETURN2.keycode),
    SEPARATOR(SDLScancode.SEPARATOR.keycode),
    OUT(SDLScancode.OUT.keycode),
    OPER(SDLScancode.OPER.keycode),
    CLEARAGAIN(SDLScancode.CLEARAGAIN.keycode),
    CRSEL(SDLScancode.CRSEL.keycode),
    EXSEL(SDLScancode.EXSEL.keycode),

    KP_00(SDLScancode.KP_00.keycode),
    KP_000(SDLScancode.KP_000.keycode),
    THOUSANDSSEPARATOR(SDLScancode.THOUSANDSSEPARATOR.keycode),
    DECIMALSEPARATOR(SDLScancode.DECIMALSEPARATOR.keycode),
    CURRENCYUNIT(SDLScancode.CURRENCYUNIT.keycode),
    CURRENCYSUBUNIT(SDLScancode.CURRENCYSUBUNIT.keycode),
    KP_LEFTPAREN(SDLScancode.KP_LEFTPAREN.keycode),
    KP_RIGHTPAREN(SDLScancode.KP_RIGHTPAREN.keycode),
    KP_LEFTBRACE(SDLScancode.KP_LEFTBRACE.keycode),
    KP_RIGHTBRACE(SDLScancode.KP_RIGHTBRACE.keycode),
    KP_TAB(SDLScancode.KP_TAB.keycode),
    KP_BACKSPACE(SDLScancode.KP_BACKSPACE.keycode),
    KP_A(SDLScancode.KP_A.keycode),
    KP_B(SDLScancode.KP_B.keycode),
    KP_C(SDLScancode.KP_C.keycode),
    KP_D(SDLScancode.KP_D.keycode),
    KP_E(SDLScancode.KP_E.keycode),
    KP_F(SDLScancode.KP_F.keycode),
    KP_XOR(SDLScancode.KP_XOR.keycode),
    KP_POWER(SDLScancode.KP_POWER.keycode),
    KP_PERCENT(SDLScancode.KP_PERCENT.keycode),
    KP_LESS(SDLScancode.KP_LESS.keycode),
    KP_GREATER(SDLScancode.KP_GREATER.keycode),
    KP_AMPERSAND(SDLScancode.KP_AMPERSAND.keycode),
    KP_DBLAMPERSAND(SDLScancode.KP_DBLAMPERSAND.keycode),
    KP_VERTICALBAR(SDLScancode.KP_VERTICALBAR.keycode),
    KP_DBLVERTICALBAR(SDLScancode.KP_DBLVERTICALBAR.keycode),
    KP_COLON(SDLScancode.KP_COLON.keycode),
    KP_HASH(SDLScancode.KP_HASH.keycode),
    KP_SPACE(SDLScancode.KP_SPACE.keycode),
    KP_AT(SDLScancode.KP_AT.keycode),
    KP_EXCLAM(SDLScancode.KP_EXCLAM.keycode),
    KP_MEMSTORE(SDLScancode.KP_MEMSTORE.keycode),
    KP_MEMRECALL(SDLScancode.KP_MEMRECALL.keycode),
    KP_MEMCLEAR(SDLScancode.KP_MEMCLEAR.keycode),
    KP_MEMADD(SDLScancode.KP_MEMADD.keycode),
    KP_MEMSUBTRACT(SDLScancode.KP_MEMSUBTRACT.keycode),
    KP_MEMMULTIPLY(SDLScancode.KP_MEMMULTIPLY.keycode),
    KP_MEMDIVIDE(SDLScancode.KP_MEMDIVIDE.keycode),
    KP_PLUSMINUS(SDLScancode.KP_PLUSMINUS.keycode),
    KP_CLEAR(SDLScancode.KP_CLEAR.keycode),
    KP_CLEARENTRY(SDLScancode.KP_CLEARENTRY.keycode),
    KP_BINARY(SDLScancode.KP_BINARY.keycode),
    KP_OCTAL(SDLScancode.KP_OCTAL.keycode),
    KP_DECIMAL(SDLScancode.KP_DECIMAL.keycode),
    KP_HEXADECIMAL(SDLScancode.KP_HEXADECIMAL.keycode),

    LCTRL(SDLScancode.LCTRL.keycode),
    LSHIFT(SDLScancode.LSHIFT.keycode),
    LALT(SDLScancode.LALT.keycode),
    LGUI(SDLScancode.LGUI.keycode),
    RCTRL(SDLScancode.RCTRL.keycode),
    RSHIFT(SDLScancode.RSHIFT.keycode),
    RALT(SDLScancode.RALT.keycode),
    RGUI(SDLScancode.RGUI.keycode),

    MODE(SDLScancode.MODE.keycode),

    AUDIONEXT(SDLScancode.AUDIONEXT.keycode),
    AUDIOPREV(SDLScancode.AUDIOPREV.keycode),
    AUDIOSTOP(SDLScancode.AUDIOSTOP.keycode),
    AUDIOPLAY(SDLScancode.AUDIOPLAY.keycode),
    AUDIOMUTE(SDLScancode.AUDIOMUTE.keycode),
    MEDIASELECT(SDLScancode.MEDIASELECT.keycode),
    WWW(SDLScancode.WWW.keycode),
    MAIL(SDLScancode.MAIL.keycode),
    CALCULATOR(SDLScancode.CALCULATOR.keycode),
    COMPUTER(SDLScancode.COMPUTER.keycode),
    AC_SEARCH(SDLScancode.AC_SEARCH.keycode),
    AC_HOME(SDLScancode.AC_HOME.keycode),
    AC_BACK(SDLScancode.AC_BACK.keycode),
    AC_FORWARD(SDLScancode.AC_FORWARD.keycode),
    AC_STOP(SDLScancode.AC_STOP.keycode),
    AC_REFRESH(SDLScancode.AC_REFRESH.keycode),
    AC_BOOKMARKS(SDLScancode.AC_BOOKMARKS.keycode),

    BRIGHTNESSDOWN(SDLScancode.BRIGHTNESSDOWN.keycode),
    BRIGHTNESSUP(SDLScancode.BRIGHTNESSUP.keycode),
    DISPLAYSWITCH(SDLScancode.DISPLAYSWITCH.keycode),
    KBDILLUMTOGGLE(SDLScancode.KBDILLUMTOGGLE.keycode),
    KBDILLUMDOWN(SDLScancode.KBDILLUMDOWN.keycode),
    KBDILLUMUP(SDLScancode.KBDILLUMUP.keycode),
    EJECT(SDLScancode.EJECT.keycode),
    SLEEP(SDLScancode.SLEEP.keycode),
    APP1(SDLScancode.APP1.keycode),
    APP2(SDLScancode.APP2.keycode),

    AUDIOREWIND(SDLScancode.AUDIOREWIND.keycode),
    AUDIOFASTFORWARD(SDLScancode.AUDIOFASTFORWARD.keycode);

    companion object {
        private val lookup = mutableMapOf<Int, SDLKeyCode>()

        init {
            values().forEach { lookup[it.value] = it }
        }

        fun fromInt(value: Int): SDLKeyCode = lookup[value] ?: UNKNOWN
    }
}