@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korui.light

import android.R
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.soywiz.korag.AG
import com.soywiz.korag.AGConfig
import com.soywiz.korag.AGFactoryAndroid
import com.soywiz.korag.AGOpenglFactory
import com.soywiz.korev.Event
import com.soywiz.korev.EventDispatcher
import com.soywiz.korim.format.toAndroidBitmap
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.DummyCloseable
import kotlinx.coroutines.CompletableDeferred
import kotlin.reflect.KClass

class AndroidLightComponents(val activity: Activity) : LightComponents() {
    //val scale = KorioAndroidContext.resources.displayMetrics.density
    //fun scaled(v: Double): Double = v * scale + 0.5f
    //fun scaled(v: Int): Double = v * scale + 0.5f
    //fun scaled_rev(v: Double): Double = v / scale
    //fun scaled_rev(v: Int): Int = v / scale

    fun scaled(v: Double): Double = v
    fun scaled(v: Int): Int = v
    fun scaled_rev(v: Double): Double = v
    fun scaled_rev(v: Int): Int = v

    override fun create(type: LightType, config: Any?): LightComponentInfo {
        var agg: AG? = null
        val handle = when (type) {
            LightType.FRAME -> {
                val view = RootKoruiAbsoluteLayout(activity)
                view.layoutParams =
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT)
                activity.setContentView(view)
                view
            }
            LightType.BUTTON -> {
                Button(activity)
            }
            LightType.CONTAINER -> {
                KoruiAbsoluteLayout(activity)
            }
            LightType.PROGRESS -> {
                ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal)
            }
            LightType.LABEL -> {
                TextView(activity)
            }
            LightType.TEXT_FIELD -> {
                EditText(activity).apply {
                    setSingleLine()
                }
            }
            LightType.TEXT_AREA -> {
                EditText(activity).apply {
                    setSingleLine(false)
                }
            }
            LightType.CHECK_BOX -> {
                CheckBox(activity)
            }
            LightType.SCROLL_PANE -> {
                ScrollView2(activity)
            }
            LightType.IMAGE -> {
                ImageView(activity)
            }
            LightType.AGCANVAS -> {
                val ag = AGOpenglFactory.create(activity).create(activity, AGConfig())
                agg = ag
            }
            else -> {
                View(activity)
            }
        }
        return LightComponentInfo(handle).apply {
            if (agg != null) this.ag = agg!!
        }
    }

    override fun setParent(c: Any, parent: Any?) {
        println("$parent.addView($c)")
        val actualParent = (parent as? ChildContainer)?.group ?: parent
        (actualParent as ViewGroup).addView(c as View)
        //(parent as ViewGroup).requestLayout()
    }


    override fun setBounds(c: Any, x: Int, y: Int, width: Int, height: Int) {
        //println("--------------------------")
        //println("setBounds[${c.javaClass.simpleName}]($x, $y, $width, $height)")
        if (c is View) {
            if (c is RootKoruiAbsoluteLayout) {

            } else {
                //println(" :::::::::::: $x,$y,$width,$height")
                val layoutParams = c.layoutParams
                if (layoutParams is AbsoluteLayout.LayoutParams) {
                    layoutParams.x = scaled(x)
                    layoutParams.y = scaled(y)
                }
                layoutParams.width = scaled(width)
                layoutParams.height = scaled(height)
                c.requestLayout()
            }
        }
    }

    override fun repaint(c: Any) {
        (c as View).requestLayout()
    }

    override fun <T> setProperty(c: Any, key: LightProperty<T>, value: T) {
        val cc = c as View
        when (key) {
            LightProperty.TEXT -> {
                val v = key[value]
                (cc as? TextView)?.text = v
            }
            LightProperty.CHECKED -> {
                val v = key[value]
                (cc as? CheckBox)?.isChecked = v
            }
            LightProperty.BGCOLOR -> {
            }
            LightProperty.PROGRESS_CURRENT -> {
                val v = key[value]
                (cc as? ProgressBar)?.progress = v
            }
            LightProperty.PROGRESS_MAX -> {
                val v = key[value]
                (cc as? ProgressBar)?.max = v
            }
            LightProperty.IMAGE -> {
                val v = key[value]
                (cc as? ImageView)?.setImageBitmap(v?.toAndroidBitmap())
            }
        }
    }

    override fun <T> getProperty(c: Any, key: LightProperty<T>): T {
        val cc = c as View
        return key[when (key) {
            LightProperty.CHECKED -> (cc as? CheckBox)?.isChecked
            LightProperty.TEXT -> (cc as? TextView)?.text?.toString()
            else -> super.getProperty(c, key)
        }]
    }

    override fun <T : Event> registerEventKind(c: Any, clazz: KClass<T>, ed: EventDispatcher): Closeable {
        TODO()
        val cc = c as View
        when (clazz) {
            com.soywiz.korev.MouseEvent::class -> {
                cc.setOnClickListener {
                    //insideEventHandler { ed.dispatch(com.soywiz.korev.MouseEvent()) }
                    TODO()
                }
                return DummyCloseable
            }
        }
        /*
override fun addHandler(c: Any, listener: LightResizeHandler): Closeable {
        val cc = (c as RootKoruiAbsoluteLayout)
        //val ctx = activity as KoruiActivity

        fun send() {
            val sizeX = scaled_rev((cc.parent as View).width)
            val sizeY = scaled_rev((cc.parent as View).height)
            println("LightResizeEvent($sizeX, $sizeY)")
            insideEventHandler { listener.resized(LightResizeHandler.Info(sizeX, sizeY)) }
        }

        KorioApp.resized { send() }
        send()
        return DummyCloseable
    }

    override fun addHandler(c: Any, listener: LightKeyHandler): Closeable {
        return super.addHandler(c, listener)
    }

    override fun addHandler(c: Any, listener: LightGamepadHandler): Closeable {
        return super.addHandler(c, listener)
    }

    override fun addHandler(c: Any, listener: LightTouchHandler): Closeable {
        return super.addHandler(c, listener)
    }


         */
    }


    override suspend fun dialogAlert(c: Any, message: String): Unit {
        val deferred = CompletableDeferred<Unit>()
        activity.runOnUiThread {
            val dialog = AlertDialog.Builder(activity)
                .setTitle(message)
                .setMessage(message)
                .setPositiveButton(R.string.ok) { _, _ ->
                    deferred.complete(Unit)
                }
                //.setNegativeButton(android.R.string.no, android.content.DialogInterface.OnClickListener { dialog, which ->
                //	c.resume(false)
                //})
                .setIcon(R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show()

            dialog.show()
        }
        deferred.await()
    }

    override fun getDpi(): Double {
        val metrics = activity.resources.displayMetrics
        return metrics.densityDpi.toDouble()
    }
}

interface ChildContainer {
    val group: ViewGroup
}

class ScrollView2(context: Context, override val group: KoruiAbsoluteLayout = KoruiAbsoluteLayout(context)) :
    ScrollView(context), ChildContainer {
    init {
        addView(group)
    }
}
