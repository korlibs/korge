package korlibs.render.osx

import com.sun.jna.*
import korlibs.memory.dyn.*
import korlibs.render.platform.*

//open class MacKmlGL : NativeKgl(MacGL)
open class MacKmlGL : NativeKgl(DirectGL)

interface MacGL : INativeGL, Library {
    companion object : MacGL by NativeLoad(nativeOpenGLLibraryPath) {
    }

    //fun CGLSetParameter(vararg args: Any?): Int
    fun CGLEnable(ctx: Pointer?, enable: Int): Int
    fun CGLDisable(ctx: Pointer?, enable: Int): Int
    fun CGLChoosePixelFormat(attributes: Pointer, pix: Pointer, num: Pointer): Int
    fun CGLCreateContext(pix: Pointer?, sharedCtx: Pointer?, ctx: Pointer?): Int
    fun CGLDestroyPixelFormat(ctx: Pointer?): Int
    fun CGLSetCurrentContext(ctx: Pointer?): Int
    fun CGLGetCurrentContext(): Pointer?
    fun CGLDestroyContext(ctx: Pointer?): Int
    fun CGLGetPixelFormat(ctx: Pointer?): Pointer?

    enum class Error(val id: Int) {
        kCGLNoError            (0),        /* no error */
        kCGLBadAttribute       (10000),	/* invalid pixel format attribute  */
        kCGLBadProperty        (10001),	/* invalid renderer property       */
        kCGLBadPixelFormat     (10002),	/* invalid pixel format            */
        kCGLBadRendererInfo    (10003),	/* invalid renderer info           */
        kCGLBadContext         (10004),	/* invalid context                 */
        kCGLBadDrawable        (10005),	/* invalid drawable                */
        kCGLBadDisplay         (10006),	/* invalid graphics device         */
        kCGLBadState           (10007),	/* invalid context state           */
        kCGLBadValue           (10008),	/* invalid numerical value         */
        kCGLBadMatch           (10009),	/* invalid share context           */
        kCGLBadEnumeration     (10010),	/* invalid enumerant               */
        kCGLBadOffScreen       (10011),	/* invalid offscreen drawable      */
        kCGLBadFullScreen      (10012),	/* invalid fullscreen drawable     */
        kCGLBadWindow          (10013),	/* invalid window                  */
        kCGLBadAddress         (10014),	/* invalid pointer                 */
        kCGLBadCodeModule      (10015),	/* invalid code module             */
        kCGLBadAlloc           (10016),	/* invalid memory allocation       */
        kCGLBadConnection      (10017), /* invalid CoreGraphics connection */
        kUnknownError      (-1);

        companion object {
            val VALUES = values().associateBy { it.id }

            operator fun get(id: Int): Error = VALUES[id] ?: kUnknownError
        }
    }
}
