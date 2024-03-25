package korlibs.io.compression.lzo

internal object LzoConstants {
    const val SIZE_OF_SHORT = 2
    const val SIZE_OF_INT = 4
    const val SIZE_OF_LONG = 8

    const val F_ADLER32_D     = 0x00000001
    const val F_ADLER32_C     = 0x00000002
    const val F_STDIN         = 0x00000004
    const val F_STDOUT        = 0x00000008
    const val F_NAME_DEFAULT  = 0x00000010
    const val F_DOSISH        = 0x00000020
    const val F_H_EXTRA_FIELD = 0x00000040
    const val F_H_GMTDIFF     = 0x00000080
    const val F_CRC32_D       = 0x00000100
    const val F_CRC32_C       = 0x00000200
    const val F_MULTIPART     = 0x00000400
    const val F_H_FILTER      = 0x00000800
    const val F_H_CRC32       = 0x00001000
    const val F_H_PATH        = 0x00002000
    const val F_MASK          = 0x00003FFF
}
