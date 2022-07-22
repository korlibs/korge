package com.soywiz.korim.format

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
val RegisteredImageFormats: ImageFormatsMutable = ImageFormatsMutable()
