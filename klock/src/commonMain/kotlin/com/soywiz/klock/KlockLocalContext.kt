package com.soywiz.klock

data class KlockLocalContext(val gender: KlockLocaleGender) {

    companion object {

        val empty = KlockLocalContext(KlockLocaleGender.Neuter)
    }
}

fun KlockLocalContext.withGender(gender: KlockLocaleGender): KlockLocalContext = this.copy(gender = gender)

enum class KlockLocaleGender {
    Neuter,
    Masculine,
}
