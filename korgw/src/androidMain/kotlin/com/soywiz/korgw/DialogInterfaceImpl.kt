package com.soywiz.korgw

import android.app.*
import android.content.*
import android.net.*
import android.os.*
import android.text.*
import android.widget.*
import com.soywiz.kds.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.*
import kotlinx.coroutines.*
import java.io.*

actual fun createDialogInterfaceForComponent(nativeComponent: Any?): DialogInterface =
    DialogInterfaceAndroid { nativeComponent as Context }

class DialogInterfaceAndroid(val contextProvider: () -> Context) : DialogInterface {
    val context get() = contextProvider()

    override suspend fun browse(url: URL) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()))
        context.startActivity(intent, null)
    }

    override suspend fun alert(message: String) {
        alertConfirm("Information", message, "Accept", null)
    }

    override suspend fun confirm(message: String): Boolean {
        return alertConfirm("Confirm", message, "Yes", "no") == android.content.DialogInterface.BUTTON_POSITIVE
    }

    suspend fun alertConfirm(title: String, message: String, yes: String?, no: String?): Int {
        val deferred = CompletableDeferred<Int>()
        val listener = android.content.DialogInterface.OnClickListener { dialog, which ->
            deferred.complete(which)
        }
        val dialog = AlertDialog.Builder(context)
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
        val builder = AlertDialog.Builder(context)
        builder.setTitle(message)
        val input = EditText(context)
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
        val result = (context as ActivityWithResult).startActivityWithResult(Intent.createChooser(Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT), "Select a file"))
        val uri = result?.data ?: throw CancellationException()
        return listOf(File(uri.toString()).toVfs())
    }
}

interface ActivityWithResult {
    suspend fun startActivityWithResult(intent: Intent, options: Bundle? = null): Intent?

    open class Mixin : ActivityWithResult {
        data class ResultHandler(val request: Int) {
            var handler: (result: Int, data: Intent?) -> Unit = { result, data -> }
        }

        val resultHandlers = Pool { ResultHandler(it) }
        val handlers = LinkedHashMap<Int, ResultHandler>()
        var activity: Activity? = null

        fun registerActivityResult(handler: (result: Int, data: Intent?) -> Unit): Int {
            return resultHandlers.alloc().also {
                it.handler = handler
            }.request
        }

        fun tryHandleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
            val handler = handlers.remove(requestCode)
            if (handler != null) {
                val callback = handler.handler
                resultHandlers.free(handler)
                callback(resultCode, data)
                return true
            } else {
                return false
            }
        }

        override suspend fun startActivityWithResult(intent: Intent, options: Bundle?): Intent? {
            val deferred = CompletableDeferred<Intent?>()
            val requestCode = registerActivityResult { result, data ->
                if (result == Activity.RESULT_OK) {
                    deferred.complete(data)
                } else {
                    deferred.completeExceptionally(CancellationException())
                }
            }
            activity?.startActivityForResult(intent, requestCode, options)
            return deferred.await()
        }
    }
}
