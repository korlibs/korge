package korlibs.io.file.std

import korlibs.io.posix.posixGetcwd

open class StandardBasePathsNative : StandardPathsBase {
    override val cwd: String get() = customCwd ?: posixGetcwd()
}
