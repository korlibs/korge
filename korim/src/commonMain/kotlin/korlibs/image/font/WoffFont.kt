package korlibs.image.font

import korlibs.datastructure.linkedHashMapOf
import korlibs.io.compression.deflate.Deflate
import korlibs.io.compression.deflate.ZLib
import korlibs.io.compression.uncompress
import korlibs.io.file.VfsFile
import korlibs.io.file.baseName
import korlibs.io.lang.LATIN1
import korlibs.io.stream.FastByteArrayInputStream
import korlibs.io.stream.openFastStream

class WoffFont(
    d: ByteArray,
    extName: String? = null,
    onlyReadMetadata: Boolean = false,
) : BaseTtfFont(d, extName, onlyReadMetadata) {
    private val tablesByName = LinkedHashMap<String, Table>()

    init {
        doInit()
    }

    override fun readHeaderTables() {
        tablesByName.putAll(readWoffHeaderTables(s.sliceStart()))
    }

    override fun getTable(name: String): Table? = tablesByName[name]
    override fun getTableNames(): Set<String> = tablesByName.keys

    companion object {
        fun readWoffHeaderTables(s: FastByteArrayInputStream): Map<String, Table> {
            val signature = s.readStringz(4, LATIN1)
            if (signature != "wOFF") error("Not a wOFF file")
            val flavor = s.readS32BE() // The "sfnt version" of the input font.
            val length = s.readS32BE() // Total size of the WOFF file.
            if (length > s.length) error("Stream not containing the whole WOFF file")
            val numTables = s.readU16BE() // Number of entries in directory of font tables.
            val reserved = s.readU16BE() // Reserved; set to zero.
            val totalSfntSize = s.readS32BE() // Total size needed for the uncompressed font data, including the sfnt header, directory, and font tables (including padding).
            val majorVersion = s.readU16BE() // Major version of the WOFF file.
            val minorVersion = s.readU16BE() // Minor version of the WOFF file.
            val metaOffset = s.readS32BE() // Offset to metadata block, from beginning of WOFF file.
            val metaLength = s.readS32BE() // Length of compressed metadata block.
            val metaOrigLength = s.readS32BE() // Uncompressed size of metadata block.
            val privOffset = s.readS32BE() // Offset to private data block, from beginning of WOFF file.
            val privLength = s.readS32BE() // Length of private data block.
            //println("numTables=$numTables")
            val tablesByName = linkedHashMapOf<String, Table>()
            repeat(numTables) {
                val tag = s.readStringz(4, LATIN1) // 4-byte sfnt table identifier.
                val offset = s.readS32BE() // Offset to the data, from beginning of WOFF file.
                val compLength = s.readS32BE() // Length of the compressed data, excluding padding.
                val origLength = s.readS32BE() // Length of the uncompressed table, excluding padding.
                val origChecksum = s.readS32BE() // Checksum of the uncompressed table.
                tablesByName[tag] = Table(tag, origChecksum, offset, origLength).also {
                    it.s = {
                        val slice = s.sliceWithSize(offset, compLength)
                        if (compLength == origLength) {
                            slice
                        } else {
                            slice.readAll().uncompress(ZLib).openFastStream()
                        }
                    }
                }
            }
            return tablesByName
        }
    }
}

suspend fun VfsFile.readWoffFont(
    onlyReadMetadata: Boolean = false,
) = WoffFont(this.readAll(), extName = this.baseName, onlyReadMetadata = onlyReadMetadata)
