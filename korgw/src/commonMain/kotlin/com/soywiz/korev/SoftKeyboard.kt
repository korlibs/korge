package com.soywiz.korev

enum class SoftKeyboardType {
    /** Specifies the default keyboard for the current input method. */
    DEFAULT,
    /** Specifies a keyboard that displays standard ASCII characters. */
    ASCII_CAPABLE,
    /** Specifies the numbers and punctuation keyboard. */
    NUMBERS_AND_PUNCTUATION,
    /** Specifies a keyboard for URL entry. */
    URL,
    /** Specifies a numeric keypad for PIN entry. */
    NUMBER_PAD,
    /** Specifies a keypad for entering telephone numbers. */
    PHONE_PAD,
    /** Specifies a keypad for entering a person’s name or phone number. */
    NAME_PHONE_PAD,
    /** Specifies a keyboard for entering email addresses. */
    EMAIL_ADDRESS,
    /** Specifies a keyboard with numbers and a decimal point. */
    DECIMAL_PAD,
    /** Specifies a keyboard for Twitter text entry, with easy access to the at (“@”) and hash (“#”) characters. */
    TWITTER,
    /** Specifies a keyboard for web search terms and URL entry. */
    WEB_SEARCH,
    /** Specifies a number pad that outputs only ASCII digits. */
    ASCII_CAPABLE_NUMBER_PAD,
    /** Specifies a keyboard for alphabetic entry. */
    ALPHABET,
}

enum class SoftKeyboardReturnKeyType {
    /** Specifies that the visible title of the Return key is return. */
    DEFAULT,
    /** Specifies that the visible title of the Return key is Go. */
    GO,
    /** Specifies that the visible title of the Return key is Join. */
    JOIN,
    /** Specifies that the visible title of the Return key is Next. */
    NEXT,
    /** Specifies that the visible title of the Return key is Route. */
    ROUTE,
    /** Specifies that the visible title of the Return key is Search. */
    SEARCH,
    /** Specifies that the visible title of the Return key is Done. */
    DONE,
    /** Specifies that the visible title of the Return key is Emergency Call. */
    EMERGENCY_CALL,
    /** Specifies that the visible title of the Return key is Continue. */
    CONTINUE,
}

interface ISoftKeyboardConfig {
    var softKeyboardType: SoftKeyboardType
    var softKeyboardSmartDashes: Boolean?
    var softKeyboardSmartQuotes: Boolean?
    var softKeyboardSpellChecking: Boolean?
    var softKeyboardTextContentType: String?
    var softKeyboardSmartInsertDelete: Boolean?
    var softKeyboardPassword: Boolean
    var softKeyboardReturnKeyType: SoftKeyboardReturnKeyType
    var softKeyboardAutocapitalization: Boolean?
    var softKeyboardAutocorrection: Boolean?
    var softKeyboardEnablesReturnKeyAutomatically: Boolean
    //var softKeyboardAppearance: Boolean
}

data class SoftKeyboardConfig(
    override var softKeyboardType: SoftKeyboardType = SoftKeyboardType.DEFAULT,
    override var softKeyboardSmartDashes: Boolean? = null,
    override var softKeyboardSmartQuotes: Boolean? = null,
    override var softKeyboardSpellChecking: Boolean? = null,
    override var softKeyboardTextContentType: String? = null,
    override var softKeyboardSmartInsertDelete: Boolean? = null,
    override var softKeyboardPassword: Boolean = false,
    override var softKeyboardReturnKeyType: SoftKeyboardReturnKeyType = SoftKeyboardReturnKeyType.DEFAULT,
    override var softKeyboardAutocapitalization: Boolean? = null,
    override var softKeyboardAutocorrection: Boolean? = null,
    override var softKeyboardEnablesReturnKeyAutomatically: Boolean = false,
    //override var softKeyboardAppearance: Boolean = null,
) : ISoftKeyboardConfig
