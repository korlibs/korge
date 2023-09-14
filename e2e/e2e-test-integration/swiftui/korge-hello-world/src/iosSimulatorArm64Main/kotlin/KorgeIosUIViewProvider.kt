import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.math.geom.*
import korlibs.render.*
import kotlinx.cinterop.*
import platform.GLKit.*
import platform.UIKit.*

// Used in SwiftUI/View integrations
@Suppress("unused")
@ExportObjCClass(name = "KorgeIosUIViewProvider")
class KorgeIosUIViewProvider : BaseKorgeIosUIViewProvider()
