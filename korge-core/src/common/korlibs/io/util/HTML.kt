package korlibs.io.util

import korlibs.io.serialization.xml.Xml

fun String.htmlspecialchars() = Xml.Entities.encode(this)
