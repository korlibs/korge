package korlibs.korge

import korlibs.korge.scene.*
import korlibs.math.geom.*
import korlibs.render.*
import kotlinx.cinterop.*
import platform.UIKit.*

/**
 * Create in `src/iosMain/kotlin/KorgeIosUIViewProvider.kt` this file:
 *
 * ```kotlin
 * @Suppress("unused")
 * @ExportObjCClass(name = "KorgeIosUIViewProvider")
 * class KorgeIosUIViewProvider : BaseKorgeIosUIViewProvider()
 * ```
 *
 * Then in SwiftUI:
 *
 * ```swift
 * struct MyKorgeGameView: UIViewRepresentable {
 *     func makeUIView(context: Context) -> UIView {
 *         return KorgeIosUIViewProvider().createViewController(scene: MyScene(), width: 100, height: 100)
 *     }
 *
 *     func sizeThatFits(_ proposal: ProposedViewSize, uiView: UIView, context: Context) -> CGSize? {
 *         return CGSize(width: 100, height: 100)
 *     }
 *
 *     func updateUIView(_ uiView: UIView, context: Context) {
 *     }
 * }
 * ```
 */
// Used in SwiftUI/View integrations
@Suppress("unused")
@ExportObjCClass(name = "BaseKorgeIosUIViewProvider")
open class BaseKorgeIosUIViewProvider {
    @OptIn(kotlin.experimental.ExperimentalObjCName::class)
    @ObjCName("createViewInfo")
    fun createViewInfo(scene: Scene, width: Int = 512, height: Int = 512): KorgeIosUIViewInfo {
        val controller = ViewController {
            Korge(virtualSize = Size(width, height), windowSize = Size(width, height), gameWindow = gameWindow) {
                val sceneContainer = sceneContainer()
                sceneContainer.changeTo({ scene })
            }
        }
        controller.ensureDefaultGameWindow()
        return KorgeIosUIViewInfo(controller, KorgeUIView(controller))
    }
}

class KorgeUIView(val controller: ViewController) : UIView(controller.view.bounds) {
    init {
        addSubview(controller.view)
    }
}

@ExportObjCClass(name = "KorgeIosUIViewInfo")
data class KorgeIosUIViewInfo(
    val controller: ViewController,
    val view: UIView
)
