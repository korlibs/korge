package korlibs.image.color

import korlibs.datastructure.*
import korlibs.io.lang.*

@Suppress("MemberVisibilityCanBePrivate")
object Colors {
	val WHITE = RGBA(0xFF, 0xFF, 0xFF, 0xFF)
	val BLACK = RGBA(0x00, 0x00, 0x00, 0xFF)
	val RED = RGBA(0xFF, 0x00, 0x00, 0xFF)
	val GREEN = RGBA(0x00, 0xFF, 0x00, 0xFF)
	val BLUE = RGBA(0x00, 0x00, 0xFF, 0xFF)

    val TRANSPARENT = RGBA(0x00, 0x00, 0x00, 0x00)
	val TRANSPARENT_WHITE = RGBA(0xFF, 0xFF, 0xFF, 0x00)

	val ALICEBLUE = RGBA(240, 248, 255)
	val ANTIQUEWHITE = RGBA(250, 235, 215)
	val AQUA = RGBA(0, 255, 255)
	val AQUAMARINE = RGBA(127, 255, 212)
	val AZURE = RGBA(240, 255, 255)
	val BEIGE = RGBA(245, 245, 220)
	val BISQUE = RGBA(255, 228, 196)
	val BLANCHEDALMOND = RGBA(255, 235, 205)
	val BLUEVIOLET = RGBA(138, 43, 226)
	val BROWN = RGBA(165, 42, 42)
	val BURLYWOOD = RGBA(222, 184, 135)
	val CADETBLUE = RGBA(95, 158, 160)
	val CHARTREUSE = RGBA(127, 255, 0)
	val CHOCOLATE = RGBA(210, 105, 30)
	val CORAL = RGBA(255, 127, 80)
	val CORNFLOWERBLUE = RGBA(100, 149, 237)
	val CORNSILK = RGBA(255, 248, 220)
	val CRIMSON = RGBA(220, 20, 60)
	val DARKBLUE = RGBA(0, 0, 139)
    val CYAN = RGBA(0, 255, 255)
	val DARKCYAN = RGBA(0, 139, 139)
	val DARKGOLDENROD = RGBA(184, 134, 11)
	val DARKGRAY = RGBA(169, 169, 169)
	val DARKGREEN = RGBA(0, 100, 0)
	val DARKGREY = RGBA(169, 169, 169)
	val DARKKHAKI = RGBA(189, 183, 107)
    val MAGENTA = RGBA(255, 0, 255)
	val DARKMAGENTA = RGBA(139, 0, 139)
	val DARKOLIVEGREEN = RGBA(85, 107, 47)
	val DARKORANGE = RGBA(255, 140, 0)
	val DARKORCHID = RGBA(153, 50, 204)
	val DARKRED = RGBA(139, 0, 0)
	val DARKSALMON = RGBA(233, 150, 122)
	val DARKSEAGREEN = RGBA(143, 188, 143)
	val DARKSLATEBLUE = RGBA(72, 61, 139)
	val DARKSLATEGRAY = RGBA(47, 79, 79)
	val DARKSLATEGREY = RGBA(47, 79, 79)
	val DARKTURQUOISE = RGBA(0, 206, 209)
	val DARKVIOLET = RGBA(148, 0, 211)
	val DEEPPINK = RGBA(255, 20, 147)
	val DEEPSKYBLUE = RGBA(0, 191, 255)
	val DIMGRAY = RGBA(105, 105, 105)
	val DIMGREY = RGBA(105, 105, 105)
	val DODGERBLUE = RGBA(30, 144, 255)
	val FIREBRICK = RGBA(178, 34, 34)
	val FLORALWHITE = RGBA(255, 250, 240)
	val FORESTGREEN = RGBA(34, 139, 34)
	val FUCHSIA = RGBA(255, 0, 255)
	val GAINSBORO = RGBA(220, 220, 220)
	val GHOSTWHITE = RGBA(248, 248, 255)
	val GOLD = RGBA(255, 215, 0)
	val GOLDENROD = RGBA(218, 165, 32)
	val GREENYELLOW = RGBA(173, 255, 47)
	val HONEYDEW = RGBA(240, 255, 240)
	val HOTPINK = RGBA(255, 105, 180)
	val INDIANRED = RGBA(205, 92, 92)
	val INDIGO = RGBA(75, 0, 130)
	val IVORY = RGBA(255, 255, 240)
	val KHAKI = RGBA(240, 230, 140)
	val LAVENDER = RGBA(230, 230, 250)
	val LAVENDERBLUSH = RGBA(255, 240, 245)
	val LAWNGREEN = RGBA(124, 252, 0)
	val LEMONCHIFFON = RGBA(255, 250, 205)
	val LIGHTBLUE = RGBA(173, 216, 230)
	val LIGHTCORAL = RGBA(240, 128, 128)
	val LIGHTCYAN = RGBA(224, 255, 255)
	val LIGHTGOLDENRODYELLOW = RGBA(250, 250, 210)
	val LIGHTGRAY = RGBA(211, 211, 211)
	val LIGHTGREEN = RGBA(144, 238, 144)
	val LIGHTGREY = RGBA(211, 211, 211)
	val LIGHTPINK = RGBA(255, 182, 193)
	val LIGHTSALMON = RGBA(255, 160, 122)
	val LIGHTSEAGREEN = RGBA(32, 178, 170)
	val LIGHTSKYBLUE = RGBA(135, 206, 250)
	val LIGHTSLATEGRAY = RGBA(119, 136, 153)
	val LIGHTSLATEGREY = RGBA(119, 136, 153)
	val LIGHTSTEELBLUE = RGBA(176, 196, 222)
	val LIGHTYELLOW = RGBA(255, 255, 224)
	val LIME = RGBA(0, 255, 0)
	val LIMEGREEN = RGBA(50, 205, 50)
	val LINEN = RGBA(250, 240, 230)
	val MAROON = RGBA(128, 0, 0)
	val MEDIUMAQUAMARINE = RGBA(102, 205, 170)
	val MEDIUMBLUE = RGBA(0, 0, 205)
	val MEDIUMORCHID = RGBA(186, 85, 211)
	val MEDIUMPURPLE = RGBA(147, 112, 219)
	val MEDIUMSEAGREEN = RGBA(60, 179, 113)
	val MEDIUMSLATEBLUE = RGBA(123, 104, 238)
	val MEDIUMSPRINGGREEN = RGBA(0, 250, 154)
	val MEDIUMTURQUOISE = RGBA(72, 209, 204)
	val MEDIUMVIOLETRED = RGBA(199, 21, 133)
	val MIDNIGHTBLUE = RGBA(25, 25, 112)
	val MINTCREAM = RGBA(245, 255, 250)
	val MISTYROSE = RGBA(255, 228, 225)
	val MOCCASIN = RGBA(255, 228, 181)
	val NAVAJOWHITE = RGBA(255, 222, 173)
	val NAVY = RGBA(0, 0, 128)
	val OLDLACE = RGBA(253, 245, 230)
	val OLIVE = RGBA(128, 128, 0)
	val OLIVEDRAB = RGBA(107, 142, 35)
	val ORANGE = RGBA(255, 165, 0)
	val ORANGERED = RGBA(255, 69, 0)
	val ORCHID = RGBA(218, 112, 214)
	val PALEGOLDENROD = RGBA(238, 232, 170)
	val PALEGREEN = RGBA(152, 251, 152)
	val PALETURQUOISE = RGBA(175, 238, 238)
	val PALEVIOLETRED = RGBA(219, 112, 147)
	val PAPAYAWHIP = RGBA(255, 239, 213)
	val PEACHPUFF = RGBA(255, 218, 185)
	val PERU = RGBA(205, 133, 63)
	val PINK = RGBA(255, 192, 203)
	val PLUM = RGBA(221, 160, 221)
	val POWDERBLUE = RGBA(176, 224, 230)
	val PURPLE = RGBA(128, 0, 128)
	val ROSYBROWN = RGBA(188, 143, 143)
	val ROYALBLUE = RGBA(65, 105, 225)
	val SADDLEBROWN = RGBA(139, 69, 19)
	val SALMON = RGBA(250, 128, 114)
	val SANDYBROWN = RGBA(244, 164, 96)
	val SEAGREEN = RGBA(46, 139, 87)
	val SEASHELL = RGBA(255, 245, 238)
	val SIENNA = RGBA(160, 82, 45)
	val SILVER = RGBA(192, 192, 192)
	val SKYBLUE = RGBA(135, 206, 235)
	val SLATEBLUE = RGBA(106, 90, 205)
	val SLATEGRAY = RGBA(112, 128, 144)
	val SLATEGREY = RGBA(112, 128, 144)
	val SNOW = RGBA(255, 250, 250)
	val SPRINGGREEN = RGBA(0, 255, 127)
	val STEELBLUE = RGBA(70, 130, 180)
	val TAN = RGBA(210, 180, 140)
	val TEAL = RGBA(0, 128, 128)
	val THISTLE = RGBA(216, 191, 216)
	val TOMATO = RGBA(255, 99, 71)
	val TURQUOISE = RGBA(64, 224, 208)
	val VIOLET = RGBA(238, 130, 238)
	val WHEAT = RGBA(245, 222, 179)
	val WHITESMOKE = RGBA(245, 245, 245)
	val YELLOWGREEN = RGBA(154, 205, 50)
	val YELLOW = RGBA(255, 255, 0)

	val colorsByName = mapOf(
		"black" to BLACK,
		"white" to WHITE,
		"red" to RED,
		"green" to GREEN,
		"blue" to BLUE,

		"aliceblue" to ALICEBLUE,
		"antiquewhite" to ANTIQUEWHITE,
		"aqua" to AQUA,
		"aquamarine" to AQUAMARINE,
		"azure" to AZURE,
		"beige" to BEIGE,
		"bisque" to BISQUE,
		"blanchedalmond" to BLANCHEDALMOND,
		"blueviolet" to BLUEVIOLET,
		"brown" to BROWN,
		"burlywood" to BURLYWOOD,
		"cadetblue" to CADETBLUE,
		"chartreuse" to CHARTREUSE,
		"chocolate" to CHOCOLATE,
		"coral" to CORAL,
		"cornflowerblue" to CORNFLOWERBLUE,
		"cornsilk" to CORNSILK,
		"crimson" to CRIMSON,
		"cyan" to CYAN,
		"darkblue" to DARKBLUE,
		"darkcyan" to DARKCYAN,
		"darkgoldenrod" to DARKGOLDENROD,
		"darkgray" to DARKGRAY,
		"darkgreen" to DARKGREEN,
		"darkgrey" to DARKGREY,
		"darkkhaki" to DARKKHAKI,
		"darkmagenta" to DARKMAGENTA,
		"darkolivegreen" to DARKOLIVEGREEN,
		"darkorange" to DARKORANGE,
		"darkorchid" to DARKORCHID,
		"darkred" to DARKRED,
		"darksalmon" to DARKSALMON,
		"darkseagreen" to DARKSEAGREEN,
		"darkslateblue" to DARKSLATEBLUE,
		"darkslategray" to DARKSLATEGRAY,
		"darkslategrey" to DARKSLATEGREY,
		"darkturquoise" to DARKTURQUOISE,
		"darkviolet" to DARKVIOLET,
		"deeppink" to DEEPPINK,
		"deepskyblue" to DEEPSKYBLUE,
		"dimgray" to DIMGRAY,
		"dimgrey" to DIMGREY,
		"dodgerblue" to DODGERBLUE,
		"firebrick" to FIREBRICK,
		"floralwhite" to FLORALWHITE,
		"forestgreen" to FORESTGREEN,
		"fuchsia" to FUCHSIA,
		"gainsboro" to GAINSBORO,
		"ghostwhite" to GHOSTWHITE,
		"gold" to GOLD,
		"goldenrod" to GOLDENROD,
		"greenyellow" to GREENYELLOW,
		"honeydew" to HONEYDEW,
		"hotpink" to HOTPINK,
		"indianred" to INDIANRED,
		"indigo" to INDIGO,
		"ivory" to IVORY,
		"khaki" to KHAKI,
		"lavender" to LAVENDER,
		"lavenderblush" to LAVENDERBLUSH,
		"lawngreen" to LAWNGREEN,
		"lemonchiffon" to LEMONCHIFFON,
		"lightblue" to LIGHTBLUE,
		"lightcoral" to LIGHTCORAL,
		"lightcyan" to LIGHTCYAN,
		"lightgoldenrodyellow" to LIGHTGOLDENRODYELLOW,
		"lightgray" to LIGHTGRAY,
		"lightgreen" to LIGHTGREEN,
		"lightgrey" to LIGHTGREY,
		"lightpink" to LIGHTPINK,
		"lightsalmon" to LIGHTSALMON,
		"lightseagreen" to LIGHTSEAGREEN,
		"lightskyblue" to LIGHTSKYBLUE,
		"lightslategray" to LIGHTSLATEGRAY,
		"lightslategrey" to LIGHTSLATEGREY,
		"lightsteelblue" to LIGHTSTEELBLUE,
		"lightyellow" to LIGHTYELLOW,
		"lime" to LIME,
		"limegreen" to LIMEGREEN,
		"linen" to LINEN,
		"magenta" to MAGENTA,
		"maroon" to MAROON,
		"mediumaquamarine" to MEDIUMAQUAMARINE,
		"mediumblue" to MEDIUMBLUE,
		"mediumorchid" to MEDIUMORCHID,
		"mediumpurple" to MEDIUMPURPLE,
		"mediumseagreen" to MEDIUMSEAGREEN,
		"mediumslateblue" to MEDIUMSLATEBLUE,
		"mediumspringgreen" to MEDIUMSPRINGGREEN,
		"mediumturquoise" to MEDIUMTURQUOISE,
		"mediumvioletred" to MEDIUMVIOLETRED,
		"midnightblue" to MIDNIGHTBLUE,
		"mintcream" to MINTCREAM,
		"mistyrose" to MISTYROSE,
		"moccasin" to MOCCASIN,
		"navajowhite" to NAVAJOWHITE,
		"navy" to NAVY,
		"oldlace" to OLDLACE,
		"olive" to OLIVE,
		"olivedrab" to OLIVEDRAB,
		"orange" to ORANGE,
		"orangered" to ORANGERED,
		"orchid" to ORCHID,
		"palegoldenrod" to PALEGOLDENROD,
		"palegreen" to PALEGREEN,
		"paleturquoise" to PALETURQUOISE,
		"palevioletred" to PALEVIOLETRED,
		"papayawhip" to PAPAYAWHIP,
		"peachpuff" to PEACHPUFF,
		"peru" to PERU,
		"pink" to PINK,
		"plum" to PLUM,
		"powderblue" to POWDERBLUE,
		"purple" to PURPLE,
		"rosybrown" to ROSYBROWN,
		"royalblue" to ROYALBLUE,
		"saddlebrown" to SADDLEBROWN,
		"salmon" to SALMON,
		"sandybrown" to SANDYBROWN,
		"seagreen" to SEAGREEN,
		"seashell" to SEASHELL,
		"sienna" to SIENNA,
		"silver" to SILVER,
		"skyblue" to SKYBLUE,
		"slateblue" to SLATEBLUE,
		"slategray" to SLATEGRAY,
		"slategrey" to SLATEGREY,
		"snow" to SNOW,
		"springgreen" to SPRINGGREEN,
		"steelblue" to STEELBLUE,
		"tan" to TAN,
		"teal" to TEAL,
		"thistle" to THISTLE,
		"tomato" to TOMATO,
		"turquoise" to TURQUOISE,
		"violet" to VIOLET,
		"wheat" to WHEAT,
		"whitesmoke" to WHITESMOKE,
		"yellowgreen" to YELLOWGREEN,
		"yellow" to YELLOW
	)

	operator fun get(str: String): RGBA = get(str, TRANSPARENT, errorOnDefault = true)

	operator fun get(str: String, default: RGBA, errorOnDefault: Boolean = false): RGBA {
        try {
            when {
                str.startsWith("#") -> {
                    val hex = str.substr(1)
                    if (hex.length !in setOf(3, 4, 6, 8)) return BLACK
                    val chars = if (hex.length < 6) 1 else 2
                    val scale = if (hex.length < 6) (255.0 / 15.0) else 1.0
                    val hasAlpha = (hex.length / chars) >= 4
                    val r = (hex.substr(0 * chars, chars).toInt(0x10) * scale).toInt()
                    val g = (hex.substr(1 * chars, chars).toInt(0x10) * scale).toInt()
                    val b = (hex.substr(2 * chars, chars).toInt(0x10) * scale).toInt()
                    val a = if (hasAlpha) (hex.substr(3 * chars, chars).toInt(0x10) * scale).toInt() else 0xFF
                    return RGBA(r, g, b, a)
                }
                str.startsWith("RGBA(", ignoreCase = true) -> {
                    val parts = str.toUpperCase().removePrefix("RGBA(").removeSuffix(")").split(",")
                    val r = parts.getOrElse(0) { "0" }.toIntOrNull() ?: 0
                    val g = parts.getOrElse(1) { "0" }.toIntOrNull() ?: 0
                    val b = parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0
                    val af = parts.getOrElse(3) { "1.0" }.toDoubleOrNull() ?: 1.0
                    return RGBA(r, g, b, (af * 255).toInt())
                }
                else -> {
                    val col = colorsByName[str.toLowerCase()]
                    //error("Unsupported color '$str'")
                    if (col == null && errorOnDefault) return get("#$str", default)
                    return col ?: default
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            return Colors.RED
        }
	}

    open class WithDefault(val defaultColor: RGBA) {
        operator fun get(str: String): RGBA = get(str, default = defaultColor)
    }

	object Default : WithDefault(Colors.RED)
}

object ColorsExt {
    val namesByColor = Colors.colorsByName.map { it.value.value to it.key }.toIntMap()
}

fun Colors.getName(color: RGBA): String? = ColorsExt.namesByColor[color.value]
fun Colors.getNameOrHex(color: RGBA): String = ColorsExt.namesByColor[color.value] ?: color.hexString

fun RGBA.toHtmlNamedString(): String = Colors.getNameOrHex(this)
