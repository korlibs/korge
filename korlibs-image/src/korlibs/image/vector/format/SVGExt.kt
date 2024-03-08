package korlibs.image.vector.format

import korlibs.io.file.VfsFile
import korlibs.io.serialization.xml.readXml

suspend fun VfsFile.readSVG() = SVG(this.readXml())
