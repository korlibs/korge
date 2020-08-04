package com.soywiz.korge.gradle.targets.windows

import com.soywiz.korge.gradle.KorgeExtension
import com.soywiz.korio.util.*
import java.io.*

object WindowsRC {
    fun generate(info: KorgeExtension): String = Indenter.genString {
        line("1000 ICON \"icon.ico\"")
        line("")
        line("1 VERSIONINFO")
        line("FILEVERSION     1,0,0,0")
        line("PRODUCTVERSION  1,0,0,0")
        line("BEGIN")
        line("  BLOCK \"StringFileInfo\"")
        line("  BEGIN")
        line("    BLOCK \"080904E4\"")
        line("    BEGIN")
        line("      VALUE \"CompanyName\", ${info.authorName.quoted}")
        line("      VALUE \"FileDescription\", ${info.description.quoted}")
        line("      VALUE \"FileVersion\", ${info.version.quoted}")
        line("      VALUE \"FileVersion\", ${info.version.quoted}")
        line("      VALUE \"InternalName\", ${info.name.quoted}")
        line("      VALUE \"LegalCopyright\", ${info.copyright.quoted}")
        line("      VALUE \"OriginalFilename\", ${info.exeBaseName.quoted}")
        line("      VALUE \"ProductName\", ${info.name.quoted}")
        line("      VALUE \"ProductVersion\", ${info.version.quoted}")
        line("    END")
        line("  END")
        line("  BLOCK \"VarFileInfo\"")
        line("  BEGIN")
        line("    VALUE \"Translation\", 0x809, 1252")
        line("  END")
        line("END")
    }
}
