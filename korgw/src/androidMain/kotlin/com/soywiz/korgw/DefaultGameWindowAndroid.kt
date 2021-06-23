package com.soywiz.korgw

import android.app.*
import android.content.*
import android.content.DialogInterface
import android.net.*
import android.text.*
import android.view.*
import android.view.inputmethod.*
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import com.soywiz.korag.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.android.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.*
import kotlinx.coroutines.*
import java.io.*
import kotlin.coroutines.*

actual fun CreateDefaultGameWindow(): GameWindow = TODO()

abstract class BaseAndroidGameWindow() : GameWindow() {
    abstract val androidContext: Context
    abstract val androidView: View
    val context get() = androidContext
    var coroutineContext: CoroutineContext? = null

    val inputMethodManager get() = androidContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    override var isSoftKeyboardVisible: Boolean = false

    override fun showSoftKeyboard(force: Boolean) {
        isSoftKeyboardVisible = true
        println("Korgw.BaseAndroidGameWindow.showSoftKeyboard:force=$force")
        try {
            //inputMethodManager.showSoftInput(androidView, if (force) InputMethodManager.SHOW_FORCED else 0)
            if (force) {
                inputMethodManager.toggleSoftInput(
                    InputMethodManager.SHOW_FORCED,
                    InputMethodManager.HIDE_IMPLICIT_ONLY
                )
            }

            //val hasHardKeyboard = context.resources.configuration.hasHardKeyboard()
            //val hardKeyboardInUse = context.resources.configuration.isHardKeyboardInUse()
            //if (hasHardKeyboard && !hardKeyboardInUse) {
            //    inputMethodManager.showSoftInput(androidView, InputMethodManager.SHOW_FORCED)
            //} else if (!hasHardKeyboard) {
            //    inputMethodManager.showSoftInput(androidView, InputMethodManager.SHOW_IMPLICIT)
            //}

            //inputMethodManager.showSoftInput(androidView, InputMethodManager.SHOW_FORCED)
            inputMethodManager.showSoftInput(androidView, InputMethodManager.SHOW_IMPLICIT)

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun hideSoftKeyboard() {
        println("Korgw.BaseAndroidGameWindow.hideSoftKeyboard")
        isSoftKeyboardVisible = false
        try {
            inputMethodManager.hideSoftInputFromWindow(androidView.getWindowToken(), 0)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

class AndroidGameWindow(val activity: KorgwActivity) : BaseAndroidGameWindow() {
    override val androidContext get() = activity
    override val androidView: View get() = activity.mGLView ?: error("Can't find mGLView")

    val mainHandler by lazy { android.os.Handler(androidContext.getMainLooper()) }

    override val ag: AG get() = activity.ag

    private var _setTitle: String? = null
    override var title: String; get() = _setTitle ?: activity.title.toString(); set(value) { _setTitle = value; mainHandler.post { activity.title = value } }
    override val width: Int get() = activity.window.decorView.width
    override val height: Int get() = activity.window.decorView.height
    override var icon: Bitmap?
        get() = super.icon
        set(value) {}
    override var fullscreen: Boolean = true
        set(value) {
            field = value
            activity.makeFullscreen(value)
        }
    override var visible: Boolean
        get() = super.visible
        set(value) {}
    override var quality: Quality
        get() = super.quality
        set(value) {}

    fun initializeAndroid() {
        fullscreen = true
    }

    override fun setSize(width: Int, height: Int) {
    }

    override suspend fun browse(url: URL) {
        startActivity(activity, Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())), null)
    }

    override suspend fun alert(message: String) {
        alertConfirm("Information", message, "Accept", null)
    }

    override suspend fun confirm(message: String): Boolean {
        return alertConfirm("Confirm", message, "Yes", "no") == DialogInterface.BUTTON_POSITIVE
    }

    suspend fun alertConfirm(title: String, message: String, yes: String?, no: String?): Int {
        val deferred = CompletableDeferred<Int>()
        val listener = DialogInterface.OnClickListener { dialog, which ->
            deferred.complete(which)
        }
        val dialog = AlertDialog.Builder(activity)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
        if (yes != null) {
            dialog.setPositiveButton(yes, listener)
        }
        if (no != null) {
            dialog.setNegativeButton(no, listener)
        }
        dialog.show()
        return deferred.await()
    }

    override suspend fun prompt(message: String, default: String): String {
        val deferred = CompletableDeferred<String>()
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(message)
        val input = EditText(androidContext)
        input.inputType = InputType.TYPE_CLASS_TEXT // InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.setText(default)
        builder.setView(input)
        builder.setCancelable(false)
        builder.setPositiveButton("OK") { dialog, which ->
            deferred.complete(input.text.toString())
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
            deferred.completeExceptionally(CancellationException())
        }
        builder.show()
        return deferred.await()
    }

    override suspend fun openFileDialog(filter: FileFilter?, write: Boolean, multi: Boolean, currentDir: VfsFile?): List<VfsFile> {
        val result = activity.startActivityWithResult(Intent.createChooser(Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT), "Select a file"))
        val uri = result?.data ?: throw CancellationException()
        return listOf(File(uri.toString()).toVfs())
    }

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        this.coroutineContext = kotlin.coroutines.coroutineContext
        //println("CONTEXT: ${kotlin.coroutines.coroutineContext[AndroidCoroutineContext.Key]?.context}")
        entry(this)
    }
}

class AndroidGameWindowNoActivity(override val width: Int, override val height: Int, override val ag: AG, override val androidContext: Context, val getView: () -> View) : BaseAndroidGameWindow() {
    override val androidView: View get() = getView()
    override var title: String = "Korge"

    override var icon: Bitmap?
        get() = super.icon
        set(value) {}

    override var fullscreen: Boolean
        get() = true
        set(value) {}

    override var visible: Boolean
        get() = super.visible
        set(value) {}

    override var quality: Quality
        get() = super.quality
        set(value) {}

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        this.coroutineContext = kotlin.coroutines.coroutineContext
        entry(this)
    }
}
