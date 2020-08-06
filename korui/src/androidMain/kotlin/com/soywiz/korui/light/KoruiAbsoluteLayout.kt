package com.soywiz.korui.light

import android.content.Context
import android.util.AttributeSet
import android.widget.AbsoluteLayout

open class KoruiAbsoluteLayout(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
	AbsoluteLayout(context, attrs, defStyle)

open class RootKoruiAbsoluteLayout(context: Context) : KoruiAbsoluteLayout(context)