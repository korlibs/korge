package platform.foundation

import kotlinx.cinterop.*
import platform.Foundation.*


val CValue<NSRect>.width get() = this.useContents { size.width }
val CValue<NSRect>.height get() = this.useContents { size.height }
