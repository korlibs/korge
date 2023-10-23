package korlibs.render

import platform.GLKit.*
import platform.UIKit.*

actual val iosTvosTools: IosTvosToolsImpl = object : IosTvosToolsImpl() {
    val uiSelectionFeedbackGenerator by lazy { UISelectionFeedbackGenerator() }
    val uiImpactFeedbackGenerator by lazy { UIImpactFeedbackGenerator() }

    override fun applicationDidFinishLaunching(app: UIApplication, window: UIWindow) {
        window.backgroundColor = UIColor.systemBackgroundColor
    }

    override fun viewDidLoad(view: GLKView?) {
        view?.multipleTouchEnabled = true
    }

    override fun hapticFeedbackGenerate(kind: HapticFeedbackKind) {
        when (kind) {
            HapticFeedbackKind.GENERIC -> uiSelectionFeedbackGenerator.selectionChanged()
            HapticFeedbackKind.ALIGNMENT -> uiSelectionFeedbackGenerator.selectionChanged()
            HapticFeedbackKind.LEVEL_CHANGE -> uiImpactFeedbackGenerator.impactOccurred()
        }
    }
}
