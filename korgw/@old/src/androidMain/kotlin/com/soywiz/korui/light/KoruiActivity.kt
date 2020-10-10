package com.soywiz.korui.light

import android.app.Activity
import android.os.Bundle
import android.os.PersistableBundle
import com.soywiz.korio.Korio

open class KoruiActivity : Activity() {
    lateinit var rootLayout: RootKoruiAbsoluteLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Korio(this) {
            main()
        }
    }

    open suspend fun main() {
    }
}
