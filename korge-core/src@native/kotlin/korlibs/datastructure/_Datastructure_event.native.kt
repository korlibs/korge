@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.event

actual fun createPlatformEventLoop(precise: Boolean): SyncEventLoop =
    SyncEventLoop()
