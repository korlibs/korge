package platform.foundation

import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.Foundation.NSRect


val CValue<NSRect>.width get() = this.useContents { size.width }
val CValue<NSRect>.height get() = this.useContents { size.height }
