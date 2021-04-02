package com.soywiz.klock

data class KlockLocalContext(val gender: KlockLocaleGender = KlockLocaleGender.Neuter)

enum class KlockLocaleGender {
    Neuter,
    Masculine,
}
