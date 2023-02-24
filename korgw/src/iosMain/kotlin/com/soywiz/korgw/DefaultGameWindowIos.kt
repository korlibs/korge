package com.soywiz.korgw

import com.soywiz.kds.Pool
import com.soywiz.kgl.*
import com.soywiz.klock.measureTime
import com.soywiz.korag.*

import com.soywiz.klogger.Console
import com.soywiz.kmem.KmemGC
import com.soywiz.kmem.hasFlags
import com.soywiz.korag.gl.*
import com.soywiz.korev.ISoftKeyboardConfig
import com.soywiz.korev.Key
import com.soywiz.korev.KeyEvent
import com.soywiz.korev.SoftKeyboardConfig
import com.soywiz.korev.SoftKeyboardReturnKeyType
import com.soywiz.korev.SoftKeyboardType
import com.soywiz.korim.format.cg.*
import com.soywiz.korma.geom.MRectangle
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.EAGL.EAGLContext
import platform.EAGL.kEAGLRenderingAPIOpenGLES2
import platform.Foundation.NSBundle
import platform.Foundation.NSComparisonResult
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotification
import platform.Foundation.NSRange
import platform.GLKit.GLKView
import platform.GLKit.GLKViewController
import platform.GLKit.GLKViewDrawableDepthFormat24
import platform.GLKit.GLKViewDrawableStencilFormat8
import platform.GameController.GCEventViewController
import platform.UIKit.*
import platform.darwin.NSInteger

// @TODO: Do not remove! Called from a generated .kt file : platforms/native-ios/bootstrap.kt
@Suppress("unused", "UNUSED_PARAMETER")
abstract class KorgwBaseNewAppDelegate {
    // Overriden to provide the entry
    abstract fun applicationDidFinishLaunching(app: UIApplication)

    // Keep references to avoid collecting instances
    lateinit var window: UIWindow
    lateinit var entry: suspend () -> Unit
    lateinit var viewController: ViewController

    val gameWindow: IosGameWindow get() = MyIosGameWindow

    fun applicationDidFinishLaunching(app: UIApplication, entry: suspend () -> Unit) {
        Console.info("applicationDidFinishLaunching: entry=$entry")
        this.entry = entry
        window = UIWindow(frame = UIScreen.mainScreen.bounds)
        CreateInitialIosGameWindow(this)
        viewController = ViewController(entry)
        window.rootViewController = viewController
        window.makeKeyAndVisible()
        //window?.windowScene = windowScene
        window.backgroundColor = UIColor.systemBackgroundColor
    }


    fun applicationDidEnterBackground(app: UIApplication) {
        Console.info("applicationDidEnterBackground")
    }
    fun applicationWillEnterForeground(app: UIApplication) {
        Console.info("applicationWillEnterForeground")
    }
    fun applicationWillResignActive(app: UIApplication) {
        Console.info("applicationWillResignActive")
        forceGC()
        gameWindow.dispatchPauseEvent()
    }
    fun applicationDidBecomeActive(app: UIApplication) {
        Console.info("applicationDidBecomeActive")
        gameWindow.dispatchResumeEvent()
    }
    fun applicationWillTerminate(app: UIApplication) {
        Console.info("applicationWillTerminate")
        gameWindow.dispatchStopEvent()
        gameWindow.dispatchDestroyEvent()
    }

    private fun forceGC() {
        Console.info("Collecting GC...")
        val time = measureTime {
            KmemGC.collect() // Forces collection when going to background to release resources to the app
        }
        Console.info("Collected in $time")
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
class ViewController(val entry: suspend () -> Unit) : GCEventViewController(null, null) {
    // Keep references to avoid collecting instances
    lateinit var glXViewController: MyGLKViewController
    val gameWindow: IosGameWindow get() = MyIosGameWindow

    override fun viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.

        Console.info("ViewController!")

        glXViewController = MyGLKViewController(entry)
        //glXViewController.preferredFramesPerSecond = 60

        val glView = glXViewController.view
        Console.info("glView: ${glView}")
        Console.info("glView: ${glView.bounds}")
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
class MyGLKViewController(val entry: suspend () -> Unit)  : GLKViewController(null, null) {
    var value = 0
    var initialized = false
    private var myContext: EAGLContext? = null
    val gameWindow: IosGameWindow get() = MyIosGameWindow
    val touches = arrayListOf<UITouch>()
    val touchesIds = arrayListOf<Int>()
    val freeIds = Pool { it }
    var lastWidth = 0
    var lastHeight = 0
    val darwinGamePad = DarwinGameControllerNative()

    override fun viewDidLoad() {
        val view = this.view as? GLKView?
        view?.multipleTouchEnabled = true
        view?.drawableDepthFormat = GLKViewDrawableDepthFormat24
        view?.drawableStencilFormat = GLKViewDrawableStencilFormat8
        view?.context = EAGLContext(kEAGLRenderingAPIOpenGLES2)
        initialized = false
        lastWidth = 0
        lastHeight = 0
    }

    override fun glkView(view: GLKView, drawInRect: CValue<CGRect>) {
        if (!initialized) {
            initialized = true
            val path = NSBundle.mainBundle.resourcePath
            if (path != null) {
                val rpath = "$path/include/app/resources"
                NSFileManager.defaultManager.changeCurrentDirectoryPath(rpath)
                com.soywiz.korio.file.std.customCwd = rpath
            }
            //self.lastTouchId = 0;

            Console.info("dispatchInitEvent")
            gameWindow.dispatchInitEvent()
            gameWindow.entry {
                Console.info("Executing entry...")
                this.entry()
            }
        }

        // Context changed!
        val currentContext = EAGLContext.currentContext()
        if (myContext != currentContext) {
            Console.info("myContext = $myContext")
            Console.info("currentContext = $currentContext")
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

        //this.value++
        //glDisable(GL_SCISSOR_TEST)
        //glDisable(GL_STENCIL_TEST)
        //glViewport(0, 0, 200, 300)
        //glScissor(0, 0, 200, 300)
        //glClearColor((this.value % 100).toFloat() / 100f, 0f, 1f, 1f)
        //glClear(GL_COLOR_BUFFER_BIT)

        darwinGamePad.updateGamepads(gameWindow)
        gameWindow.frame()
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

    override fun touchesBegan(touches: Set<*>, withEvent: UIEvent?) {
        if (!initialized) return
        this.gameWindow.dispatchTouchEventModeIos()
        this.gameWindow.dispatchTouchEventStartStart()
        //printf("moved.");
        addTouches(touches, type = TouchType.BEGAN)
        this.gameWindow.dispatchTouchEventEnd()
    }

    override fun touchesMoved(touches: Set<*>, withEvent: UIEvent?) {
        if (!initialized) return
        this.gameWindow.dispatchTouchEventModeIos()
        this.gameWindow.dispatchTouchEventStartMove()
        //printf("moved.");
        addTouches(touches, type = TouchType.MOVED)
        this.gameWindow.dispatchTouchEventEnd()
    }

    override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
        if (!initialized) return
        this.gameWindow.dispatchTouchEventModeIos()
        this.gameWindow.dispatchTouchEventStartEnd()
        //printf("ended.");
        addTouches(touches, type = TouchType.ENDED)
        this.gameWindow.dispatchTouchEventEnd()
    }

    private fun addTouches(touches: Set<*>, type: TouchType) {
        //println("addTouches[${touches.size}] type=$type");
        for (touch in touches) {
            if (touch !is UITouch) {
                Console.info("ERROR.addTouches no UITouch")
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

            val pointX = point.useContents { x.toDouble() }
            val pointY = point.useContents { y.toDouble() }
            val px = pointX * this.view.contentScaleFactor
            val py = pointY * this.view.contentScaleFactor
            this.gameWindow.dispatchTouchEventAddTouch(uid, px, py)

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
) : GameWindow() {
    override val dialogInterface = DialogInterfaceIos()

    override val ag: AG = AGOpengl(KmlGlNative(gles = true).checkedIf(checked = false))

    override val pixelsPerInch: Double get() = UIScreen.mainScreen.scale.toDouble() * 160.0

    //override var fps: Int get() = 60; set(value) = Unit
    //override var title: String get() = ""; set(value) = Unit
    //override val width: Int get() = 512
    //override val height: Int get() = 512
    //override var icon: Bitmap? get() = null; set(value) = Unit
    //override var fullscreen: Boolean get() = false; set(value) = Unit
    //override var visible: Boolean get() = false; set(value) = Unit
    //override var quality: Quality get() = Quality.AUTOMATIC; set(value) = Unit

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        println("YAY! IosGameWindow.loop")
        // Trick to reference some classes to make them available on iOS
        //println("loop[0]")
        try {
            entry(this)
            //println("loop[1]")
        } catch (e: Throwable) {
            println("ERROR IosGameWindow.loop:")
            e.printStackTrace()
        }
    }

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
        override fun firstRectForRange(range: UITextRange): CValue<CGRect> =
            CGRectMakeExt(0.0, 0.0, 128.0, 32.0)

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

    val window: UIWindow get() = windowProvider?.invoke()
        ?: UIApplication.sharedApplication.keyWindow
        ?: (UIApplication.sharedApplication.windows.first() as UIWindow)

    override fun setInputRectangle(windowRect: MRectangle) {
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

    val uiSelectionFeedbackGenerator by lazy { UISelectionFeedbackGenerator() }
    val uiImpactFeedbackGenerator by lazy { UIImpactFeedbackGenerator() }

    override val hapticFeedbackGenerateSupport: Boolean get() = true
    override fun hapticFeedbackGenerate(kind: HapticFeedbackKind) {
        when (kind) {
            HapticFeedbackKind.GENERIC -> uiSelectionFeedbackGenerator.selectionChanged()
            HapticFeedbackKind.ALIGNMENT -> uiSelectionFeedbackGenerator.selectionChanged()
            HapticFeedbackKind.LEVEL_CHANGE -> uiImpactFeedbackGenerator.impactOccurred()
        }
    }
}

private lateinit var MyIosGameWindow: IosGameWindow // Creates instance everytime
private fun CreateInitialIosGameWindow(app: KorgwBaseNewAppDelegate): IosGameWindow {
    MyIosGameWindow = IosGameWindow { app.window }
    return MyIosGameWindow
}

actual fun CreateDefaultGameWindow(config: GameWindowCreationConfig): GameWindow = MyIosGameWindow


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
