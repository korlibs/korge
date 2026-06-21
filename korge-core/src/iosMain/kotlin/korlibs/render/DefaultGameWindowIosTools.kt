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

    override fun hapticFeedbackGenerate(kind: GameWindow.HapticFeedbackKind) {
        when (kind) {
            GameWindow.HapticFeedbackKind.GENERIC -> uiSelectionFeedbackGenerator.selectionChanged()
            GameWindow.HapticFeedbackKind.ALIGNMENT -> uiSelectionFeedbackGenerator.selectionChanged()
            GameWindow.HapticFeedbackKind.LEVEL_CHANGE -> uiImpactFeedbackGenerator.impactOccurred()
        }
    }
}
