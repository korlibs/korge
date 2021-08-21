package com.soywiz.korio.util.i18n

import com.soywiz.korio.*
import com.soywiz.korio.lang.Environment
import kotlinx.browser.*

internal actual val systemLanguageStrings: List<String> by lazy {
    when {
        NodeDeno.available -> {
            listOf(
                Environment["LANG"]
                    ?: Environment["LANGUAGE"]
                    ?: Environment["LC_ALL"]
                    ?: Environment["LC_MESSAGES"]
                    ?: "english"
            )
        }
        else -> {
            window.navigator.languages.asList()
        }
    }
}

