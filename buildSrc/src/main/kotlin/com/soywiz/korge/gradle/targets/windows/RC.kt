package com.soywiz.korge.gradle.targets.windows

import com.soywiz.korge.gradle.KorgeExtension
import java.io.*
import com.soywiz.korge.gradle.util.*

object WindowsRC {
    fun generate(info: KorgeExtension): String = kotlin.text.buildString {
        appendLine("1000 ICON \"icon.ico\"")
        appendLine("")
        // https://docs.microsoft.com/en-us/windows/win32/menurc/versioninfo-resource
        appendLine("1 VERSIONINFO")
        appendLine("FILEVERSION     1,0,0,0")
        appendLine("PRODUCTVERSION  1,0,0,0")
        appendLine("BEGIN")
        appendLine("  BLOCK \"StringFileInfo\"")
        appendLine("  BEGIN")
        appendLine("    BLOCK \"080904E4\"")
        appendLine("    BEGIN")
        appendLine("      VALUE \"CompanyName\", ${info.authorName.quoted}")
        appendLine("      VALUE \"FileDescription\", ${info.description.quoted}")
        appendLine("      VALUE \"FileVersion\", ${info.version.quoted}")
        appendLine("      VALUE \"FileVersion\", ${info.version.quoted}")
        appendLine("      VALUE \"InternalName\", ${info.name.quoted}")
        appendLine("      VALUE \"LegalCopyright\", ${info.copyright.quoted}")
        appendLine("      VALUE \"OriginalFilename\", ${info.exeBaseName.quoted}")
        appendLine("      VALUE \"ProductName\", ${info.name.quoted}")
        appendLine("      VALUE \"ProductVersion\", ${info.version.quoted}")
        appendLine("    END")
        appendLine("  END")
        appendLine("  BLOCK \"VarFileInfo\"")
        appendLine("  BEGIN")
        appendLine("    VALUE \"Translation\", 0x809, 1252")
        appendLine("  END")
        appendLine("END")
    }
}

private fun StringBuilder.appendLine(value: String?): StringBuilder {
    if (value != null) append(value)
    append('\n')
    return this
}
