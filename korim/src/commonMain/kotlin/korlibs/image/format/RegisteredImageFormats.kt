package korlibs.image.format

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
val RegisteredImageFormats: ImageFormatsMutable = ImageFormatsMutable()