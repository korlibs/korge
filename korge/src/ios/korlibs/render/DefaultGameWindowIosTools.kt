package korlibs.render

import platform.GLKit.*
import platform.UIKit.*

actual val iosTvosTools: IosTvosToolsImpl = object : IosTvosToolsImpl() {
    val uiSelectionFeedbackGenerator by lazy { UISelectionFeedbackGenerator() }
    val uiImpactFeedbackGenerator by lazy { UIImpactFeedbackGenerator() }

    override fun applicationDidFinishLaunching(app: UIApplication, window: UIWindow) {
        super.applicationDidFinishLaunching(app, window)
        window.backgroundColor = UIColor.systemBackgroundColor
    }

    override fun viewDidLoad(view: GLKView?) {
        super.viewDidLoad(view)
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
