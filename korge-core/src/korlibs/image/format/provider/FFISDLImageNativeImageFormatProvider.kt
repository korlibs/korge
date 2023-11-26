package korlibs.image.format.provider

import korlibs.ffi.*
import korlibs.image.bitmap.*
import korlibs.image.format.*

object FFISDLImageNativeImageFormatProvider : BaseNativeImageFormatProvider() {
    private object SDL : FFILib(
        "SDL2",
    ) {
        val SDL_RWFromMem: (ByteArray, Int) -> FFIPointer? by func()
        val SDL_FreeSurface: (FFIPointer?) -> Unit by func()
        val SDL_ConvertSurfaceFormat: (FFIPointer?, Int, Int) -> FFIPointer? by func()

        const val SDL_PIXELFORMAT_RGBA8888 = 373694468
        const val SDL_PIXELFORMAT_BGRA8888 = 377888772
        const val SDL_PIXELFORMAT_ARGB8888 = 372645892
        const val SDL_PIXELFORMAT_ABGR8888 = 376840196
    }

    private object SDL_Image : FFILib(
        "SDL2_image",
    ) {
        val IMG_Load_RW: (FFIPointer?, freesrc: Boolean) -> FFIPointer? by func()
        // SDL_PixelFormat* format - the format of the pixels stored in the surface; see SDL_PixelFormat for details (read-only)
        // int w, h - the width and height in pixels (read-only)
        // int pitch - the length of a row of pixels in bytes (read-only)
        // void* pixels - the pointer to the actual pixel data; see Remarks for details (read-write)
    }

    //internal class SDL_Surface(pointer: KPointer? = null) : KStructure(pointer) {
    //    var wType by int()
    //    var values by int()
    //}

    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
        val surface = SDL_Image.IMG_Load_RW(SDL.SDL_RWFromMem(data, data.size), true) ?: error("Can't load image")
        val nsurface =
            SDL.SDL_ConvertSurfaceFormat(surface, SDL.SDL_PIXELFORMAT_ABGR8888, 0) ?: error("Can't convert image")
        SDL.SDL_FreeSurface(surface)

        //val ptr = surface.getUnalignedFFIPointer(0) ?: error("Can't load PTR")
        //println(nsurface.getIntArray(16).toList())
        val width = nsurface.getS32(4)
        val height = nsurface.getS32(5)
        //println(nsurface.getI32(8))
        //println(nsurface.getI64(4))
        val pixels = nsurface.getAlignedFFIPointer(4) ?: error("Pixels is null in size=${width}x$height")
        val out = Bitmap32(
            width,
            height,
            pixels.getIntArray(width * height)
        )
        //println("SURFACE: ${nsurface.address}")
        SDL.SDL_FreeSurface(nsurface)
        return NativeImageResult(BitmapNativeImage(out))
    }

}
