package korlibs.graphics.shader.gl

import korlibs.memory.*

inline class GLVariant(val data: Int) {
    val isES: Boolean get() = data.extractBool(0)
    val isWebGL: Boolean get() = data.extractBool(1)
    val isCore: Boolean get() = data.extractBool(2)
    val version: Int get() = data.extract8(8)
    val os: Os get() = Os.VALUES.getOrNull(data.extract8(16)) ?: Os.UNKNOWN

    val supportTextureLevel: Boolean get() = !isWebGL && !isES

    companion object {
        operator fun invoke(
            isES: Boolean = false,
            isWebGL: Boolean = false,
            isCore: Boolean = false,
            version: Int = 0,
            os: Os = Os.UNKNOWN,
        ): GLVariant =
            GLVariant(0.insert(isES, 0).insert(isWebGL, 1).insert(isCore, 2).insert8(version, 8).insert8(os.ordinal, 16))

        val DESKTOP_GENERIC get() = GLVariant(isES = false, version = 1, os = Os.UNKNOWN)
        val DESKTOP get() = GLVariant(isES = false, version = 1, os = Os.CURRENT)
        val IOS get() = GLVariant(isES = true, version = 1)
        val JVM_SDL get() = DESKTOP
        val JVM_X11 get() = GLVariant(isES = false, version = 1, os = Os.LINUX)
        val JVM get() = DESKTOP
        val ANDROID get() = GLVariant(isES = true, version = 1)
        val LINUX_DESKTOP get() = GLVariant(isES = false, version = 1)
        fun JS_WEBGL(version: Int): GLVariant = GLVariant(isES = true, isWebGL = true, version = version)
    }
}
