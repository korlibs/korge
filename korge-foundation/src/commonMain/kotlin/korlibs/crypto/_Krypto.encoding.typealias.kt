@file:Suppress("PackageDirectoryMismatch")

package korlibs.crypto.encoding

// ASCII

@Deprecated("", ReplaceWith("korlibs.encoding.ASCII", "korlibs.encoding.ASCII"), DeprecationLevel.WARNING)
typealias ASCII = korlibs.encoding.ASCII
@Deprecated("", ReplaceWith("korlibs.encoding.ASCII(this)", "korlibs.encoding.ASCII"), DeprecationLevel.WARNING)
fun String.fromAscii(): ByteArray = korlibs.encoding.ASCII(this)
@Deprecated("", ReplaceWith("korlibs.encoding.ASCII(this)", "korlibs.encoding.ASCII"), DeprecationLevel.WARNING)
val ByteArray.ascii: String get() = korlibs.encoding.ASCII(this)

// BASE64

@Deprecated("", ReplaceWith("korlibs.encoding.Base64", "korlibs.encoding.Base64"), DeprecationLevel.WARNING)
typealias Base64 = korlibs.encoding.Base64

@Deprecated("", ReplaceWith("korlibs.encoding.Base64.decodeIgnoringSpaces(this, url)", "korlibs"))
fun String.fromBase64IgnoreSpaces(url: Boolean = false): ByteArray = korlibs.encoding.Base64.decodeIgnoringSpaces(this, url)
@Deprecated("", ReplaceWith("korlibs.encoding.Base64.decode(ignoreSpaces, this, url)", "korlibs"))
fun String.fromBase64(ignoreSpaces: Boolean = false, url: Boolean = false): ByteArray = korlibs.encoding.Base64.decode(ignoreSpaces, this, url)
@Deprecated("", ReplaceWith("korlibs.encoding.Base64.encode(this, url, doPadding)", "korlibs"))
fun ByteArray.toBase64(url: Boolean = false, doPadding: Boolean = false): String = korlibs.encoding.Base64.encode(this, url, doPadding)
@Deprecated("", ReplaceWith("korlibs.encoding.Base64.encode(this)", "korlibs"))
val ByteArray.base64: String get() = korlibs.encoding.Base64.encode(this)
@Deprecated("", ReplaceWith("korlibs.encoding.Base64.encode(this, true)", "korlibs"))
val ByteArray.base64Url: String get() = korlibs.encoding.Base64.encode(this, true)

// HEX

@Deprecated("", ReplaceWith("korlibs.encoding.Hex", "korlibs.encoding.Hex"))
typealias Hex = korlibs.encoding.Hex

@Deprecated("", ReplaceWith("korlibs.encoding.Hex.appendHexByte(this, value)", "korlibs"))
fun Appendable.appendHexByte(value: Int) = korlibs.encoding.Hex.appendHexByte(this, value)
@Deprecated("", ReplaceWith("korlibs.encoding.Hex.decode(this)", "korlibs"))
fun String.fromHex(): ByteArray = korlibs.encoding.Hex.decode(this)
@Deprecated("", ReplaceWith("korlibs.encoding.Hex.encodeLower(this)", "korlibs"))
val ByteArray.hexLower: String get() = korlibs.encoding.Hex.encodeLower(this)
@Deprecated("", ReplaceWith("korlibs.encoding.Hex.encodeUpper(this)", "korlibs"))
val ByteArray.hexUpper: String get() = korlibs.encoding.Hex.encodeUpper(this)
@Deprecated("", ReplaceWith("korlibs.encoding.Hex.isHexDigit(this)", "korlibs"))
fun Char.isHexDigit() = korlibs.encoding.Hex.isHexDigit(this)
@Deprecated("", ReplaceWith("korlibs.encoding.Hex.decodeIgnoreSpaces(joinToString(\"\"))", "korlibs"))
val List<String>.unhexIgnoreSpaces get() = korlibs.encoding.Hex.decodeIgnoreSpaces(joinToString(""))
@Deprecated("", ReplaceWith("korlibs.encoding.Hex.decodeIgnoreSpaces(this)", "korlibs"))
val String.unhexIgnoreSpaces: ByteArray get() = korlibs.encoding.Hex.decodeIgnoreSpaces(this)
@Deprecated("", ReplaceWith("korlibs.encoding.Hex.decode(this)", "korlibs"))
val String.unhex get() = korlibs.encoding.Hex.decode(this)
@Deprecated("", ReplaceWith("korlibs.encoding.Hex.encodeLower(this)", "korlibs"))
val ByteArray.hex get() = korlibs.encoding.Hex.encodeLower(this)
@Deprecated("", ReplaceWith("\"0x\$shex\""))
val Int.hex: String get() = "0x$shex"
@Deprecated("", ReplaceWith("korlibs.encoding.Hex.shex(this)", "korlibs"))
val Int.shex: String get() = korlibs.encoding.Hex.shex(this)
@Deprecated("", ReplaceWith("\"0x\$shex\""))
val Byte.hex: String get() = "0x$shex"
@Deprecated("", ReplaceWith("korlibs.encoding.Hex.shex(this)", "korlibs"))
val Byte.shex: String get() = korlibs.encoding.Hex.shex(this)
