package korlibs.render

import korlibs.datastructure.*
import korlibs.datastructure.event.*
import korlibs.datastructure.thread.*
import korlibs.event.*
import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.image.format.cg.*
import korlibs.io.*
import korlibs.kgl.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.math.geom.Point
import korlibs.memory.*
import korlibs.time.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.EAGL.*
import platform.Foundation.*
import platform.GLKit.*
import platform.GameController.*
import platform.UIKit.*
import platform.darwin.*

actual fun CreateDefaultGameWindow(config: GameWindowCreationConfig): GameWindow = MyIosGameWindow

expect val iosTvosTools: IosTvosToolsImpl

open class IosTvosToolsImpl {
    open fun applicationDidFinishLaunching(app: UIApplication, window: UIWindow) {
        //window?.windowScene = windowScene
    }

    open fun viewDidLoad(view: GLKView?) {
    }

    open fun hapticFeedbackGenerate(kind: HapticFeedbackKind) {
    }
}

// @TODO: Do not remove! Called from a generated .kt file : platforms/native-ios/bootstrap.kt
@Suppress("unused", "UNUSED_PARAMETER")
abstract class KorgwBaseNewAppDelegate {
    private val logger = Logger("KorgwBaseNewAppDelegate")
    // Overriden to provide the entry
    abstract fun applicationDidFinishLaunching(app: UIApplication)

    // Keep references to avoid collecting instances
    lateinit var window: UIWindow
    lateinit var entry: suspend () -> Unit
    lateinit var viewController: ViewController

    val gameWindow: IosGameWindow get() = MyIosGameWindow

    fun applicationDidFinishLaunching(app: UIApplication, entry: suspend () -> Unit) {
        logger.info { "applicationDidFinishLaunching: entry=$entry" }
        this.entry = entry
        window = UIWindow(frame = UIScreen.mainScreen.bounds)
        CreateInitialIosGameWindow(this)
        viewController = ViewController { entry() }
        window.rootViewController = viewController
        window.makeKeyAndVisible()
        iosTvosTools.applicationDidFinishLaunching(app, window)
    }

    fun applicationDidEnterBackground(app: UIApplication) {
        logger.info {"applicationDidEnterBackground" }
    }
    fun applicationWillEnterForeground(app: UIApplication) {
        logger.info {"applicationWillEnterForeground" }
    }
    fun applicationWillResignActive(app: UIApplication) {
        logger.info {"applicationWillResignActive" }
        forceGC()
        gameWindow.dispatchPauseEvent()
    }
    fun applicationDidBecomeActive(app: UIApplication) {
        logger.info {"applicationDidBecomeActive" }
        gameWindow.dispatchResumeEvent()
    }
    fun applicationWillTerminate(app: UIApplication) {
        logger.info {"applicationWillTerminate" }
        gameWindow.dispatchStopEvent()
        gameWindow.dispatchDestroyEvent()
    }

    private fun forceGC() {
        logger.info {"Collecting GC..." }
        val time = measureTime {
            KmemGC.collect() // Forces collection when going to background to release resources to the app
        }
        logger.info {"Collected in $time" }
    }
}

// @TODO: Can't call NSStringFromClass from a kotlin class
//@ExportObjCClass
//class KorgwMainApplicationDelegate(val entry: suspend () -> Unit) : UIResponder(), UIApplicationDelegateProtocol {
//    override fun application(application: UIApplication, didFinishLaunchingWithOptions: Map<Any?, *>?): Boolean {
//        window = UIWindow(frame = UIScreen.mainScreen.bounds)
//        val viewController = ViewController(entry)
//        window?.rootViewController = viewController
//        window?.makeKeyAndVisible()
//        //window?.windowScene = windowScene
//        window?.backgroundColor = UIColor.systemBackgroundColor
//        return true
//    }
//}

@ExportObjCClass
class ViewController(
    val entry: suspend ViewController.() -> Unit
) : GCEventViewController(null, null) {
    private val logger = Logger("ViewController")
    // Keep references to avoid collecting instances
    lateinit var glXViewController: MyGLKViewController
    var _gameWindow: IosGameWindow? = null
    val gameWindow: IosGameWindow get() = _gameWindow ?: MyIosGameWindow

    fun ensureDefaultGameWindow(): ViewController {
        val gwin = IosGameWindow(null, { glXViewController })
        _gameWindow = gwin
        SetInitialIosGameWindow(gwin)
        return this
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.

        logger.info { "ViewController!" }

        glXViewController = MyGLKViewController({ gameWindow }) { entry() }
        glXViewController.preferredFramesPerSecond = GameWindow.DEFAULT_PREFERRED_FPS.convert()

        val glView = glXViewController.view
        logger.info { "glView: ${glView}" }
        logger.info { "glView: ${glView.bounds}" }
        view.addSubview(glView)

        //NSNotificationCenter.defaultCenter.addObserver(this, NSSelectorFromString(this::controllerDidConnect.name), GCControllerDidConnectNotification, null)
        //NSNotificationCenter.defaultCenter.addObserver(this, NSSelectorFromString(this::controllerDidDisconnect.name), GCControllerDidDisconnectNotification, null)
    }

    @ObjCAction
    fun controllerDidConnect(notification: NSNotification) {
        println("controllerDidConnect: $notification")
    }

    @ObjCAction
    fun controllerDidDisconnect(notification: NSNotification) {
        println("controllerDidDisconnect: $notification")
    }

    @Suppress("RemoveRedundantCallsOfConversionMethods")
    @OptIn(UnsafeNumber::class)
    private fun pressesHandler(type: KeyEvent.Type, presses: Set<*>, withEvent: UIPressesEvent?) {
        super.pressesBegan(presses, withEvent)
        for (press in presses) {
            if (press !is UIPress) continue
            val uiKey = press.key ?: continue
            val keyCode = uiKey.keyCode.toInt()
            val modifierFlags = uiKey.modifierFlags.toInt()
            val key = IosKeyMap.KEY_MAP[keyCode.toInt()] ?: Key.UNKNOWN
            //println("pressesHandler[$type]: ${keyCode}, ${modifierFlags}, $key, ${uiKey.charactersIgnoringModifiers}")

            gameWindow.dispatchKeyEventEx(
                type,
                0,
                uiKey.charactersIgnoringModifiers.firstOrNull() ?: '\u0000',
                key,
                keyCode.toInt(),
                shift = modifierFlags.hasFlags(UIKeyModifierShift.toInt()),
                ctrl = modifierFlags.hasFlags(UIKeyModifierControl.toInt()),
                alt = modifierFlags.hasFlags(UIKeyModifierAlternate.toInt()),
                meta = modifierFlags.hasFlags(UIKeyModifierCommand.toInt()),
            )
        }
    }

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        super.pressesBegan(presses, withEvent)
        pressesHandler(KeyEvent.Type.DOWN, presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        super.pressesBegan(presses, withEvent)
        pressesHandler(KeyEvent.Type.UP, presses, withEvent)
    }

    //override fun pressesCancelled(presses: Set<*>, withEvent: UIPressesEvent?) {
    //    super.pressesBegan(presses, withEvent)
    //    pressesHandler(KeyEvent.Type.UP, presses, withEvent)
    //}
    //override fun pressesChanged(presses: Set<*>, withEvent: UIPressesEvent?) {
    //    super.pressesBegan(presses, withEvent)
    //    pressesHandler(KeyEvent.Type.UP, presses, withEvent)
    //}
}

@OptIn(UnsafeNumber::class)
@ExportObjCClass
class MyGLKViewController(
    val gameWindowProvider: () -> IosGameWindow = { MyIosGameWindow },
    val entry: suspend () -> Unit
)  : GLKViewController(null, null) {
    private val logger = Logger("MyGLKViewController")
    var value = 0
    var initialized = false
    private var myContext: EAGLContext? = null
    val gameWindow: IosGameWindow get() = gameWindowProvider()
    val touches = arrayListOf<UITouch>()
    val touchesIds = arrayListOf<Int>()
    val freeIds = Pool { it }
    var lastWidth = 0
    var lastHeight = 0
    val darwinGamePad = DarwinGameControllerNative()
    var eventLoopThread: NativeThread? = null

    override fun viewDidLoad() {
        val view = this.view as? GLKView?
        iosTvosTools.viewDidLoad(view)
        view?.drawableDepthFormat = GLKViewDrawableDepthFormat24
        view?.drawableStencilFormat = GLKViewDrawableStencilFormat8
        view?.context = EAGLContext(kEAGLRenderingAPIOpenGLES2)
        initialized = false
        lastWidth = 0
        lastHeight = 0
    }

    override fun viewWillAppear(animated: Boolean) {
        eventLoopThread = nativeThread { thread -> (gameWindow.eventLoop as SyncEventLoop).runTasksForever { thread.threadSuggestRunning } }
    }

    override fun viewWillDisappear(animated: Boolean) {
        eventLoopThread?.threadSuggestRunning = false
    }

    override fun glkView(view: GLKView, drawInRect: CValue<CGRect>) {
        if (!initialized) {
            initialized = true
            val path = nativeCwdOrNull()
            if (path != null) {
                val rpath = "$path/include/app/resources"
                if (NSFileManager().contentsOfDirectoryAtPath(rpath, null) != null) {
                    println("glkView: Switching CWD to $rpath...")
                    NSFileManager.defaultManager.changeCurrentDirectoryPath(rpath)
                    korlibs.io.file.std.customCwd = rpath
                } else {
                    println("glkView: NOT switching CWD ($path doesn't exists)...")
                }
            }
            //self.lastTouchId = 0;

            logger.info { "dispatchInitEvent" }
            gameWindow.dispatchInitEvent()
            gameWindow.queueSuspend {
                logger.info { "Executing entry..." }
                this.entry()
            }
        }

        // Context changed!
        val currentContext = EAGLContext.currentContext()
        if (myContext != currentContext) {
            logger.info {"myContext = $myContext" }
            logger.info {"currentContext = $currentContext" }
            myContext = currentContext
            gameWindow.ag.contextLost()
        }

        val width = (view.bounds.useContents { size.width } * view.contentScaleFactor).toInt()
        val height = (view.bounds.useContents { size.height } * view.contentScaleFactor).toInt()
        if (lastWidth != width || lastHeight != height) {
            println("RESHAPE: $lastWidth, $lastHeight -> $width, $height")
            this.lastWidth = width
            this.lastHeight = height
            gameWindow.dispatchReshapeEvent(0, 0, width, height)
        }

        darwinGamePad.updateGamepads(gameWindow)
        if (gameWindow.continuousRenderMode.shouldRender()) {
            gameWindow.dispatchNewRenderEvent()
        }
    }

    override fun didReceiveMemoryWarning() {
        //super.didReceiveMemoryWarning()
        //if (this.isViewLoaded && this.view.window != null) {
        //    this.view = null
        //    if (EAGLContext.currentContext == self.context) {
        //        [EAGLContext setCurrentContext:nil]
        //    }
        //    self.context = nil
        //}
    }

    enum class TouchType { BEGAN, MOVED, ENDED }

    protected val touchBuilder = TouchBuilder()
    protected val touchEvent get() = touchBuilder.new

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        if (!initialized) return
        this.dispatchTouchEventModeIos()
        this.dispatchTouchEventStartStart()
        //printf("moved.");
        addTouches(touches, type = TouchType.BEGAN)
        this.dispatchTouchEventEnd()
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        if (!initialized) return
        this.dispatchTouchEventModeIos()
        this.dispatchTouchEventStartMove()
        //printf("moved.");
        addTouches(touches, type = TouchType.MOVED)
        this.dispatchTouchEventEnd()
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        if (!initialized) return
        this.dispatchTouchEventModeIos()
        this.dispatchTouchEventStartEnd()
        //printf("ended.");
        addTouches(touches, type = TouchType.ENDED)
        this.dispatchTouchEventEnd()
    }

    // iOS tools
    fun dispatchTouchEventModeIos() { touchBuilder.mode = TouchBuilder.Mode.IOS }
    fun dispatchTouchEventStartStart() = touchBuilder.startFrame(TouchEvent.Type.START)
    fun dispatchTouchEventStartMove() = touchBuilder.startFrame(TouchEvent.Type.MOVE)
    fun dispatchTouchEventStartEnd() = touchBuilder.startFrame(TouchEvent.Type.END)
    fun dispatchTouchEventAddTouch(id: Int, x: Float, y: Float) = touchBuilder.touch(id, Point(x, y))
    fun dispatchTouchEventEnd() = gameWindow.dispatch(touchBuilder.endFrame().reset())

    private fun addTouches(touches: Set<*>, type: TouchType) {
        //println("addTouches[${touches.size}] type=$type");
        for (touch in touches) {
            if (touch !is UITouch) {
                logger.info { "ERROR.addTouches no UITouch" }
                continue
            }
            val point = touch.locationInView(this.view)
            var index = this.touches.indexOf(touch)

            if (index < 0) {
                index = this.touches.size
                this.touches.add(touch)
                this.touchesIds.add(freeIds.alloc())
            }

            val uid = touchesIds[index]

            //printf(" - %d: %d, %d\n", (int)num.intValue, (int)point.x, (int)point.y);

            val pointX = point.useContents { x.toFloat() }
            val pointY = point.useContents { y.toFloat() }
            val px = pointX * this.view.contentScaleFactor.toFloat()
            val py = pointY * this.view.contentScaleFactor.toFloat()
            this.dispatchTouchEventAddTouch(uid, px, py)

            //println(" - TOUCH. index=$index, uid=$uid, px=$px, py=$py")

            if (type == TouchType.ENDED) {
                freeIds.free(uid)
                this.touches.removeAt(index)
                this.touchesIds.removeAt(index)
            }
        }
    }
}

open class IosGameWindow(
    val windowProvider: (() -> UIWindow?)? = null,
    val glXViewControllerProvider: (() -> MyGLKViewController?)? = null,
) : GameWindow() {
    override val ag: AG = AGOpengl(KmlGlNative().checkedIf(checked = false))
    //override val ag: AG = AGOpengl(KmlGlNative().checkedIf(checked = true, printStackTrace = true))

    override val pixelsPerInch: Double get() = UIScreen.mainScreen.scale * 160.0

    val window: UIWindow get() = windowProvider?.invoke()
        ?: UIApplication.sharedApplication.keyWindow
        ?: (UIApplication.sharedApplication.windows.first() as UIWindow)

    override val dialogInterface = DialogInterfaceIos(this)

    val glXViewController: MyGLKViewController? get() = glXViewControllerProvider?.invoke()

    override fun close(exitCode: Int) {
        println("Not closing with exitCode=$exitCode")
        //UIApplication.sharedApplication.terminate
        super.close(exitCode)
    }

    override var preferredFps: Int = GameWindow.DEFAULT_PREFERRED_FPS
        get() = glXViewController?.preferredFramesPerSecond?.toInt() ?: field
        set(value) {
            field = value
            glXViewController?.preferredFramesPerSecond = value.convert()
        }
    override var fps: Int get() = 60; set(value) = Unit

    override val isSoftKeyboardVisible: Boolean get() = super.isSoftKeyboardVisible
    lateinit var textField: MyUITextComponent

    override var keepScreenOn: Boolean
        set(value) {
            UIApplication.sharedApplication.idleTimerDisabled = value
        }
        get() = UIApplication.sharedApplication.isIdleTimerDisabled()

    class MyUITextComponent(val gw: IosGameWindow,  rect: CValue<CGRect>) : UIView(rect), UITextInputProtocol, UITextInputTraitsProtocol {
        override fun canBecomeFirstResponder(): Boolean = true
        override fun canBecomeFocused(): Boolean = true
        override fun baseWritingDirectionForPosition(
            position: UITextPosition,
            inDirection: UITextStorageDirection
        ): NSWritingDirection = NSWritingDirectionNatural
        val bodPos = UITextPosition()
        val eodPos = UITextPosition()

        override fun beginningOfDocument(): UITextPosition = bodPos
        override fun endOfDocument(): UITextPosition = eodPos
        override fun caretRectForPosition(position: UITextPosition): CValue<CGRect> = CGRectMakeExt(0.0, 0.0, 1.0, 32.0)
        override fun characterRangeAtPoint(point: CValue<CGPoint>): UITextRange? = null
        override fun characterRangeByExtendingPosition(position: UITextPosition, inDirection: UITextLayoutDirection): UITextRange? = null
        override fun closestPositionToPoint(point: CValue<CGPoint>): UITextPosition? = null
        override fun closestPositionToPoint(point: CValue<CGPoint>, withinRange: UITextRange): UITextPosition? = null
        override fun comparePosition(position: UITextPosition, toPosition: UITextPosition): NSComparisonResult = 0
        override fun firstRectForRange(range: UITextRange): CValue<CGRect> = CGRectMakeExt(0.0, 0.0, 128.0, 32.0)
        override fun inputDelegate(): UITextInputDelegateProtocol? = null
        override fun markedTextRange(): UITextRange? = null
        override fun markedTextStyle(): Map<Any?, *>? = null
        override fun offsetFromPosition(from: UITextPosition, toPosition: UITextPosition): NSInteger = 0
        override fun positionFromPosition(position: UITextPosition, offset: NSInteger): UITextPosition? = null
        override fun positionFromPosition(
            position: UITextPosition,
            inDirection: UITextLayoutDirection,
            offset: NSInteger
        ): UITextPosition? = null

        override fun positionWithinRange(
            range: UITextRange,
            farthestInDirection: UITextLayoutDirection
        ): UITextPosition? = null

        override fun replaceRange(range: UITextRange, withText: String) {
            //println("replaceRange: range=$range, $withText")
        }

        override fun selectedTextRange(): UITextRange? = null
        override fun selectionRectsForRange(range: UITextRange): List<*> = listOf<Any>()
        override fun setBaseWritingDirection(writingDirection: NSWritingDirection, forRange: UITextRange) {}
        override fun setInputDelegate(inputDelegate: UITextInputDelegateProtocol?) = Unit
        override fun setMarkedText(markedText: String?, selectedRange: CValue<NSRange>) = Unit
        override fun setMarkedTextStyle(markedTextStyle: Map<Any?, *>?) = Unit
        override fun setSelectedTextRange(selectedTextRange: UITextRange?) = Unit
        override fun textInRange(range: UITextRange): String? = null
        override fun textRangeFromPosition(fromPosition: UITextPosition, toPosition: UITextPosition): UITextRange? = null
        val myTokenizer = UITextInputStringTokenizer()
        override fun tokenizer(): UITextInputTokenizerProtocol = myTokenizer
        override fun unmarkText() = Unit

        override fun hasText(): Boolean = false

        override fun deleteBackward() {
            //println("deleteBackward")
            gw.dispatchKeyEventDownUp(0, '\b', Key.BACKSPACE, '\b'.code, null)
        }

        override fun insertText(text: String) {
            //println("insertText=$text")
            gw.dispatchKeyEvent(KeyEvent.Type.TYPE, 0, '\u0000', Key.BACKSPACE, 0, text)
        }

        var _autocapitalizationType: UITextAutocapitalizationType = UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences
        var _autocorrectionType: UITextAutocorrectionType = UITextAutocorrectionType.UITextAutocorrectionTypeDefault
        var _isSecureTextEntry: Boolean = false
        var _keyboardAppearance: UIKeyboardAppearance = UIKeyboardAppearanceDefault
        //var _keyboardType: UIKeyboardType = UIKeyboardTypeDefault
        var _keyboardType: UIKeyboardType = UIKeyboardTypeDecimalPad
        var _returnKeyType: UIReturnKeyType = UIReturnKeyType.UIReturnKeyDefault
        var _enablesReturnKeyAutomatically: Boolean = false
        var _passwordRules: UITextInputPasswordRules? = null
        var _textContentType: UITextContentType? = null
        var _smartDashesType: UITextSmartDashesType = UITextSmartDashesType.UITextSmartDashesTypeDefault
        var _smartInsertDeleteType: UITextSmartInsertDeleteType = UITextSmartInsertDeleteType.UITextSmartInsertDeleteTypeDefault
        var _smartQuotesType: UITextSmartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeDefault
        var _spellCheckingType: UITextSpellCheckingType = UITextSpellCheckingType.UITextSpellCheckingTypeDefault

        override fun autocapitalizationType(): UITextAutocapitalizationType = _autocapitalizationType
        override fun autocorrectionType(): UITextAutocorrectionType = _autocorrectionType
        override fun enablesReturnKeyAutomatically(): Boolean = _enablesReturnKeyAutomatically
        override fun isSecureTextEntry(): Boolean = _isSecureTextEntry
        override fun keyboardAppearance(): UIKeyboardAppearance = _keyboardAppearance
        override fun keyboardType(): UIKeyboardType = _keyboardType
        override fun smartDashesType(): UITextSmartDashesType = _smartDashesType
        override fun smartInsertDeleteType(): UITextSmartInsertDeleteType = _smartInsertDeleteType
        override fun smartQuotesType(): UITextSmartQuotesType = _smartQuotesType
        override fun spellCheckingType(): UITextSpellCheckingType = _spellCheckingType
        override fun textContentType(): UITextContentType? = _textContentType
        override fun passwordRules(): UITextInputPasswordRules? = _passwordRules
        override fun returnKeyType(): UIReturnKeyType = _returnKeyType

        override fun setAutocapitalizationType(autocapitalizationType: UITextAutocapitalizationType) = run { _autocapitalizationType = autocapitalizationType }
        override fun setAutocorrectionType(autocorrectionType: UITextAutocorrectionType) = run { _autocorrectionType = autocorrectionType }
        override fun setEnablesReturnKeyAutomatically(enablesReturnKeyAutomatically: Boolean) = run { _enablesReturnKeyAutomatically = enablesReturnKeyAutomatically }
        override fun setKeyboardAppearance(keyboardAppearance: UIKeyboardAppearance) = run { _keyboardAppearance = keyboardAppearance }
        override fun setKeyboardType(keyboardType: UIKeyboardType) = run { _keyboardType = keyboardType }
        override fun setPasswordRules(passwordRules: UITextInputPasswordRules?) = run { _passwordRules = passwordRules }
        override fun setReturnKeyType(returnKeyType: UIReturnKeyType) = run { _returnKeyType = returnKeyType }
        override fun setSecureTextEntry(secureTextEntry: Boolean) = run { _isSecureTextEntry = secureTextEntry }
        override fun setSmartDashesType(smartDashesType: UITextSmartDashesType) = run { _smartDashesType = smartDashesType }
        override fun setSmartInsertDeleteType(smartInsertDeleteType: UITextSmartInsertDeleteType) = run { _smartInsertDeleteType = smartInsertDeleteType }
        override fun setSmartQuotesType(smartQuotesType: UITextSmartQuotesType) = run { _smartQuotesType = smartQuotesType }
        override fun setSpellCheckingType(spellCheckingType: UITextSpellCheckingType) = run { _spellCheckingType = spellCheckingType }
        override fun setTextContentType(textContentType: UITextContentType?) = run { _textContentType = textContentType }

        /*
        override fun beginFloatingCursorAtPoint(point: CValue<CGPoint>) = Unit
        override fun characterOffsetOfPosition(position: UITextPosition, withinRange: UITextRange): NSInteger = 0
        override fun dictationRecognitionFailed() = Unit
        override fun dictationRecordingDidEnd() = Unit
        override fun endFloatingCursor() = Unit
        override fun frameForDictationResultPlaceholder(placeholder: Any): CValue<CGRect> =
            CGRectMake(0.cg, 0.cg, 100.cg, 32.cg)

        override fun insertDictationResult(dictationResult: List<*>) = Unit
        override fun insertDictationResultPlaceholder(): Any = Unit
        override fun insertText(text: String, alternatives: List<*>, style: UITextAlternativeStyle) = Unit
        override fun insertTextPlaceholderWithSize(size: CValue<CGSize>): UITextPlaceholder = UITextPlaceholder()
        override fun removeDictationResultPlaceholder(placeholder: Any, willInsertResult: Boolean) = Unit
        override fun removeTextPlaceholder(textPlaceholder: UITextPlaceholder) = Unit
        override fun selectionAffinity(): UITextStorageDirection = UITextStorageDirectionForward
        override fun setAttributedMarkedText(markedText: NSAttributedString?, selectedRange: CValue<NSRange>) = Unit
        override fun setSelectionAffinity(selectionAffinity: UITextStorageDirection) = Unit
        override fun shouldChangeTextInRange(range: UITextRange, replacementText: String): Boolean = false
        override fun textInputView(): UIView = this
        override fun textStylingAtPosition(
            position: UITextPosition,
            inDirection: UITextStorageDirection
        ): Map<Any?, *>? = null

        override fun updateFloatingCursorAtPoint(point: CValue<CGPoint>) = Unit

         */
    }

    private fun prepareSoftKeyboardOnce() = memScoped {
        if (::textField.isInitialized) return@memScoped
        val rect = CGRectMakeExt(-1.0, 0.0, 1.0, 32.0)
        textField = MyUITextComponent(this@IosGameWindow, rect)
    }

    override fun setInputRectangle(windowRect: Rectangle) {
        println("IosGameWindow.setInputRectangle: windowRect=$windowRect")
        prepareSoftKeyboardOnce()
        textField.setBounds(windowRect.toCG())

        super.setInputRectangle(windowRect)
    }

    private val defaultSoftKeyboardConfig = SoftKeyboardConfig()

    // https://developer.apple.com/documentation/uikit/uitextinput
    // https://developer.apple.com/documentation/uikit/uikeyinput
    override fun showSoftKeyboard(force: Boolean, config: ISoftKeyboardConfig?) {
        val conf = config ?: defaultSoftKeyboardConfig
        println("IosGameWindow.showSoftKeyboard: force=$force")
        prepareSoftKeyboardOnce()
        textField.keyboardType = conf.softKeyboardType.toIOS()
        textField.returnKeyType = conf.softKeyboardReturnKeyType.toIOS()
        textField.enablesReturnKeyAutomatically = conf.softKeyboardEnablesReturnKeyAutomatically
        textField.smartDashesType = conf.softKeyboardSmartDashes.toIOS(UITextSmartDashesType.UITextSmartDashesTypeDefault, UITextSmartDashesType.UITextSmartDashesTypeNo, UITextSmartDashesType.UITextSmartDashesTypeYes)
        textField.smartQuotesType = conf.softKeyboardSmartQuotes.toIOS(UITextSmartQuotesType.UITextSmartQuotesTypeDefault, UITextSmartQuotesType.UITextSmartQuotesTypeNo, UITextSmartQuotesType.UITextSmartQuotesTypeYes)
        textField.spellCheckingType = conf.softKeyboardSpellChecking.toIOS(UITextSpellCheckingType.UITextSpellCheckingTypeDefault, UITextSpellCheckingType.UITextSpellCheckingTypeNo, UITextSpellCheckingType.UITextSpellCheckingTypeYes)
        textField.textContentType = conf.softKeyboardTextContentType
        UIReturnKeyType.UIReturnKeyDefault
        window.addSubview(textField)
        textField.becomeFirstResponder()
    }

    fun SoftKeyboardReturnKeyType.toIOS(): UIReturnKeyType = when (this) {
        SoftKeyboardReturnKeyType.DEFAULT -> UIReturnKeyType.UIReturnKeyDefault
        SoftKeyboardReturnKeyType.GO -> UIReturnKeyType.UIReturnKeyGo
        SoftKeyboardReturnKeyType.JOIN -> UIReturnKeyType.UIReturnKeyJoin
        SoftKeyboardReturnKeyType.NEXT -> UIReturnKeyType.UIReturnKeyNext
        SoftKeyboardReturnKeyType.ROUTE -> UIReturnKeyType.UIReturnKeyRoute
        SoftKeyboardReturnKeyType.SEARCH -> UIReturnKeyType.UIReturnKeySearch
        SoftKeyboardReturnKeyType.DONE -> UIReturnKeyType.UIReturnKeyDone
        SoftKeyboardReturnKeyType.EMERGENCY_CALL -> UIReturnKeyType.UIReturnKeyEmergencyCall
        SoftKeyboardReturnKeyType.CONTINUE -> UIReturnKeyType.UIReturnKeyContinue
        else -> UIReturnKeyType.UIReturnKeyDefault
    }

    @OptIn(UnsafeNumber::class)
    fun SoftKeyboardType?.toIOS(): UIKeyboardType = when (this) {
        SoftKeyboardType.DEFAULT -> UIKeyboardTypeDefault
        SoftKeyboardType.ASCII_CAPABLE -> UIKeyboardTypeASCIICapable
        SoftKeyboardType.NUMBERS_AND_PUNCTUATION -> UIKeyboardTypeNumbersAndPunctuation
        SoftKeyboardType.URL -> UIKeyboardTypeURL
        SoftKeyboardType.NUMBER_PAD -> UIKeyboardTypeNumberPad
        SoftKeyboardType.PHONE_PAD -> UIKeyboardTypePhonePad
        SoftKeyboardType.NAME_PHONE_PAD -> UIKeyboardTypeNamePhonePad
        SoftKeyboardType.EMAIL_ADDRESS -> UIKeyboardTypeEmailAddress
        SoftKeyboardType.DECIMAL_PAD -> UIKeyboardTypeDecimalPad
        SoftKeyboardType.TWITTER -> UIKeyboardTypeTwitter
        SoftKeyboardType.WEB_SEARCH -> UIKeyboardTypeWebSearch
        SoftKeyboardType.ASCII_CAPABLE_NUMBER_PAD -> UIKeyboardTypeASCIICapable
        SoftKeyboardType.ALPHABET -> UIKeyboardTypeAlphabet
        else -> UIKeyboardTypeDefault
    }

    fun <T> Boolean?.toIOS(default: T, no: T, yes: T): T = when (this) {
        null -> default
        false -> no
        true -> yes
    }

    override fun hideSoftKeyboard() {
        println("IosGameWindow.hideSoftKeyboard")
        prepareSoftKeyboardOnce()
        textField.removeFromSuperview()
        textField.resignFirstResponder()
    }

    override val hapticFeedbackGenerateSupport: Boolean get() = true
    override fun hapticFeedbackGenerate(kind: HapticFeedbackKind) {
        iosTvosTools.hapticFeedbackGenerate(kind)
    }
}

private lateinit var MyIosGameWindow: IosGameWindow // Creates instance everytime
private fun CreateInitialIosGameWindow(app: KorgwBaseNewAppDelegate): IosGameWindow {
    MyIosGameWindow = IosGameWindow({ app.window }, { app.viewController.glXViewController })
    return MyIosGameWindow
}
fun SetInitialIosGameWindow(gameWindow: IosGameWindow): IosGameWindow {
    MyIosGameWindow = gameWindow
    return gameWindow
}

/*

import UIKit
import GLKit
import Foundation

@main
struct App {
    static func main() {
        //return UIApplicationMain(argc, argv, nil, NSStringFromClass([AppDelegate class]));
        print("Starting.")
        UIApplicationMain(CommandLine.argc, CommandLine.unsafeArgv, nil,
                          NSStringFromClass(AppDelegate.self))
    }
}

//@main
class AppDelegate: UIResponder, UIApplicationDelegate {


    var window: UIWindow?
    var glview: GLKView?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        print("[1]")

        window = UIWindow(frame: UIScreen.main.bounds)
        let viewController = ViewController()
        window?.rootViewController = viewController
        window?.makeKeyAndVisible()
        //window?.windowScene = windowScene
        window?.backgroundColor = .systemBackground

        // Override point for customization after application launch.
        return true
    }
}

class ViewController: UIViewController {
    var loginButton: UIButton!
    var glXViewController: MyGLKViewController!

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.

        print("ViewController!")

        glXViewController = MyGLKViewController()

        let glView = glXViewController.view
        print("glView: \(glView!)")
        print("glView: \(glView!.bounds)")
        view.addSubview(glView!)


        loginButton = UIButton(type: .system)
        loginButton.setTitle("Login", for: .normal)
        loginButton.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(loginButton)

        constraintsInit()
    }


    func constraintsInit() {
        let glView = glXViewController.view!
        //NSLayoutConstraint.activate([
        //    glView.centerXAnchor.constraint(equalTo: view.centerXAnchor),
        //    glView.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        //])
        NSLayoutConstraint.activate([
          loginButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
          loginButton.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])
    }
}

class MyGLKViewController : GLKViewController {
    override func viewDidLoad() {
        let view = self.view! as? GLKView
        view!.context = EAGLContext(api: .openGLES2)!
    }

    override func glkView(_ view: GLKView, drawIn rect: CGRect) {
        glClearColor(1, 0, 1, 1)
        glClear(GLbitfield(GL_COLOR_BUFFER_BIT))
        print("RENDER!")
    }
}

 */
