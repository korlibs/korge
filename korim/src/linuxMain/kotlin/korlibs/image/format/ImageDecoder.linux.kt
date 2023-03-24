package korlibs.image.format

actual val nativeImageFormatProvider: NativeImageFormatProvider get() = LinuxBaseNativeImageFormatProvider

open class LinuxBaseNativeImageFormatProvider : StbImageNativeImageFormatProvider() {
    companion object : LinuxBaseNativeImageFormatProvider()
}