package com.soywiz.klock

data class KlockLocaleContext(val gender: KlockLocaleGender = KlockLocaleGender.Neuter)

enum class KlockLocaleGender {
    Neuter,
    Masculine,
}
