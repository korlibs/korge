package korlibs.render

import korlibs.io.file.*
import korlibs.io.net.*
import kotlinx.coroutines.*
import platform.Foundation.*
import platform.UIKit.*

actual fun createDialogInterfaceForComponent(nativeComponent: Any?): DialogInterface {
    return DialogInterfaceIos(nativeComponent as IosGameWindow)
}

class DialogInterfaceIos(val gameWindow: IosGameWindow) : DialogInterface {
    override suspend fun browse(url: URL) {
        UIApplication.sharedApplication.openURL(NSURL(string = url.fullUrl))
    }

    override suspend fun alert(message: String) {
        val deferred = CompletableDeferred<Unit>()
        val controller = UIAlertController()
        controller.title = "Alert"
        controller.message = message
        controller.addAction(UIAlertAction.actionWithTitle("Ok", UIAlertActionStyleDefault) {
            deferred.complete(Unit)
        })
        gameWindow.glXViewController?.let { glview ->
            glview.presentViewController(controller, animated = true) {
                deferred.complete(Unit)
            }
            deferred.await()
        }
    }

    override suspend fun confirm(message: String): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        val controller = UIAlertController()
        controller.title = "Confirm"
        controller.message = message
        controller.addAction(UIAlertAction.actionWithTitle("Yes", UIAlertActionStyleDefault) {
            deferred.complete(true)
        })
        controller.addAction(UIAlertAction.actionWithTitle("No", UIAlertActionStyleCancel) {
            deferred.complete(true)
        })
        gameWindow.glXViewController?.let { glview ->
            glview.presentViewController(controller, animated = true, completion = null)
            return deferred.await()
        }
        delay(100L)
        return false
    }

    override suspend fun prompt(message: String, default: String): String {
        return super.prompt(message, default)
    }

    override suspend fun openFileDialog(
        filter: FileFilter?,
        write: Boolean,
        multi: Boolean,
        currentDir: VfsFile?
    ): List<VfsFile> {
        return super.openFileDialog(filter, write, multi, currentDir)
    }
}
