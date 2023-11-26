package korlibs.image.format

import korlibs.io.stream.AsyncStream

object JPEGInfo : ImageFormatSuspend("jpeg") {
    override suspend fun decodeHeaderSuspend(s: AsyncStream, props: ImageDecodingProps): ImageInfo? {
        return kotlin.runCatching { EXIF.readExifFromJpeg(s) }
            .also {
                //if (it.isFailure) {
                //    println("FAILED! EXIF.readExifFromJpeg")
                //    it.exceptionOrNull()?.printStackTrace()
                //}
            }
            .getOrNull()
    }
}
