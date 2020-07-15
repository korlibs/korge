package com.soywiz.korgw

import android.app.*
import android.content.*
import android.content.DialogInterface
import android.net.*
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.content.ContextCompat.*
import android.support.v7.app.*
import android.support.v7.app.AlertDialog
import android.text.*
import android.view.*
import android.widget.*
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

class AndroidGameWindow(val activity: KorgwActivity) : GameWindow() {
    init {
        activity.gameWindow = this
    }

    val androidContext get() = activity

    override val ag: AG get() = activity.ag

    override var title: String; get() = activity.title.toString(); set(value) = run { activity.title = value }
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

    init {
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

    override suspend fun openFileDialog(filter: String?, write: Boolean, multi: Boolean): List<VfsFile> {
        val deferred = CompletableDeferred<List<VfsFile>>()
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        val requestCode = activity.registerActivityResult { result, data ->
            if (result == Activity.RESULT_OK) {
                val uri = data?.data
                if (uri != null) {
                    deferred.complete(listOf(File(uri.toString()).toVfs()))
                } else {
                    deferred.completeExceptionally(CancellationException())
                }
            } else {
                deferred.completeExceptionally(CancellationException())
            }
        }

        startActivityForResult(activity, Intent.createChooser(intent, "Select a file"), requestCode, null)

        return deferred.await()
    }

    lateinit var coroutineContext: CoroutineContext
    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        this.coroutineContext = kotlin.coroutines.coroutineContext
        //println("CONTEXT: ${kotlin.coroutines.coroutineContext[AndroidCoroutineContext.Key]?.context}")
        entry(this)
    }
}

