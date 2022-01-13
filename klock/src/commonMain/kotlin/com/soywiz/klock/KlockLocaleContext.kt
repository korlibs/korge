package com.soywiz.klock

data class KlockLocaleContext(val gender: KlockLocaleGender = KlockLocaleGender.Neuter) {

    companion object {

        val Default = KlockLocaleContext()
    }
}

enum class KlockLocaleGender {
    Neuter,
    Masculine,
}
