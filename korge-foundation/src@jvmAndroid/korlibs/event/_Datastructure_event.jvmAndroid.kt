@file:Suppress("PackageDirectoryMismatch")

package korlibs.event

actual fun createPlatformEventLoop(precise: Boolean): SyncEventLoop =
    SyncEventLoop(precise)
