package com.soywiz.korgw

import com.soywiz.kds.Pool
import com.soywiz.kgl.checkedIf
import com.soywiz.korag.*
import com.soywiz.korag.gl.*

import com.soywiz.klogger.Console
import com.soywiz.kmem.hasFlags
import com.soywiz.korev.GameButton
import com.soywiz.korev.GamePadConnectionEvent
import com.soywiz.korev.GamepadMapping
import com.soywiz.korev.Key
import com.soywiz.korev.KeyEvent
import com.soywiz.korev.StandardGamepadMapping
import com.soywiz.korma.geom.Point
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.EAGL.EAGLContext
import platform.EAGL.kEAGLRenderingAPIOpenGLES2
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.GLKit.GLKView
import platform.GLKit.GLKViewController
import platform.GLKit.GLKViewDrawableDepthFormat24
import platform.GLKit.GLKViewDrawableStencilFormat8
import platform.GameController.GCController
import platform.GameController.GCControllerButtonInput
import platform.GameController.GCControllerDidConnectNotification
import platform.GameController.GCControllerDidDisconnectNotification
import platform.GameController.GCControllerDirectionPad
import platform.GameController.GCEventViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIEvent
import platform.UIKit.UIKeyModifierAlternate
import platform.UIKit.UIKeyModifierCommand
import platform.UIKit.UIKeyModifierControl
import platform.UIKit.UIKeyModifierShift
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIScreen
import platform.UIKit.UITouch
import platform.UIKit.UIWindow
import platform.UIKit.addSubview
import platform.UIKit.backgroundColor
import platform.UIKit.contentScaleFactor
import platform.UIKit.multipleTouchEnabled
import platform.UIKit.systemBackgroundColor

// @TODO: Do not remove! Called from a generated .kt file : platforms/native-ios/bootstrap.kt
@Suppress("unused", "UNUSED_PARAMETER")
abstract class KorgwBaseNewAppDelegate {
    // Overriden to provide the entry
    abstract fun applicationDidFinishLaunching(app: UIApplication)

    // Keep references to avoid collecting instances
    lateinit var window: UIWindow
    lateinit var entry: suspend () -> Unit
    fun applicationDidFinishLaunching(app: UIApplication, entry: suspend () -> Unit) {
        Console.info("applicationDidFinishLaunching: entry=$entry")
        this.entry = entry
        window = UIWindow(frame = UIScreen.mainScreen.bounds)
        viewController = ViewController(entry)
        window.rootViewController = viewController
        window.makeKeyAndVisible()
        //window?.windowScene = windowScene
        window.backgroundColor = UIColor.systemBackgroundColor
    }

    lateinit var viewController: ViewController

    fun applicationWillResignActive(app: UIApplication) {
        Console.info("applicationWillResignActive")
    }
    fun applicationDidEnterBackground(app: UIApplication) {
        Console.info("applicationDidEnterBackground")
    }
    fun applicationWillEnterForeground(app: UIApplication) {
        Console.info("applicationWillEnterForeground")
    }
    fun applicationDidBecomeActive(app: UIApplication) {
        Console.info("applicationDidBecomeActive")
    }
    fun applicationWillTerminate(app: UIApplication) {
        Console.info("applicationWillTerminate")
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

        updateGamepads()
        gameWindow.frame()
    }

    @Suppress("RemoveRedundantCallsOfConversionMethods")
    private fun updateGamepads() {
        val controllers = GCController.controllers().filterIsInstance<GCController>().sortedBy { it.playerIndex.toInt() }
        if (controllers.isNotEmpty() || knownControllers.isNotEmpty()) {
            val addedControllers = controllers - knownControllers
            val removedControllers = knownControllers - controllers
            knownControllers.clear()
            knownControllers.addAll(controllers)
            for (controller in addedControllers) {
                gameWindow.dispatchGamepadConnectionEvent(GamePadConnectionEvent.Type.CONNECTED, controller.playerIndex.toInt())
            }
            for (controller in removedControllers) {
                gameWindow.dispatchGamepadConnectionEvent(
                    GamePadConnectionEvent.Type.DISCONNECTED,
                    controller.playerIndex.toInt()
                )
            }
            gameWindow.dispatchGamepadUpdateStart()
            val mapping = StandardGamepadMapping
            for ((index, controller) in controllers.withIndex()) {
                var buttonMask = 0
                val leftStick = Point()
                val rightStick = Point()
                fun button(button: GameButton, pressed: Boolean) {
                    if (pressed) buttonMask = buttonMask or (1 shl button.ordinal)
                }

                fun button(button: GameButton, gcbutton: GCControllerButtonInput?) {
                    if (gcbutton != null) {
                        button(button, gcbutton.pressed)
                    }
                }
                fun stick(stick: Point, pad: GCControllerDirectionPad) {
                    stick.setTo(pad.xAxis.value, pad.yAxis.value)
                }

                // https://developer.apple.com/documentation/gamecontroller/gcmicrogamepad
                // https://developer.apple.com/documentation/gamecontroller/gcgamepad
                // https://developer.apple.com/documentation/gamecontroller/gcextendedgamepad
                val microGamepad = controller.microGamepad
                val gamepad = controller.gamepad
                val extendedGamepad = controller.extendedGamepad

                when {
                    extendedGamepad != null -> {
                        button(GameButton.SYSTEM, extendedGamepad.buttonHome)
                        button(GameButton.START, extendedGamepad.buttonMenu)
                        button(GameButton.SELECT, extendedGamepad.buttonOptions)
                        button(GameButton.UP, extendedGamepad.dpad.up)
                        button(GameButton.DOWN, extendedGamepad.dpad.down)
                        button(GameButton.LEFT, extendedGamepad.dpad.left)
                        button(GameButton.RIGHT, extendedGamepad.dpad.right)
                        button(GameButton.GENERIC_BUTTON_UP, extendedGamepad.buttonY)
                        button(GameButton.GENERIC_BUTTON_RIGHT, extendedGamepad.buttonB)
                        button(GameButton.GENERIC_BUTTON_DOWN, extendedGamepad.buttonA)
                        button(GameButton.GENERIC_BUTTON_LEFT, extendedGamepad.buttonX)
                        button(GameButton.L1, extendedGamepad.leftShoulder)
                        button(GameButton.L2, extendedGamepad.leftTrigger)
                        button(GameButton.R1, extendedGamepad.rightShoulder)
                        button(GameButton.R2, extendedGamepad.rightTrigger)
                        button(GameButton.L3, extendedGamepad.leftThumbstickButton)
                        button(GameButton.R3, extendedGamepad.rightThumbstickButton)
                        stick(leftStick, extendedGamepad.leftThumbstick)
                        stick(rightStick, extendedGamepad.rightThumbstick)
                    }
                    gamepad != null -> {
                        button(GameButton.UP, gamepad.dpad.up)
                        button(GameButton.DOWN, gamepad.dpad.down)
                        button(GameButton.LEFT, gamepad.dpad.left)
                        button(GameButton.RIGHT, gamepad.dpad.right)
                        button(GameButton.GENERIC_BUTTON_UP, gamepad.buttonY)
                        button(GameButton.GENERIC_BUTTON_RIGHT, gamepad.buttonB)
                        button(GameButton.GENERIC_BUTTON_DOWN, gamepad.buttonA)
                        button(GameButton.GENERIC_BUTTON_LEFT, gamepad.buttonX)
                        button(GameButton.L1, gamepad.leftShoulder)
                        button(GameButton.R1, gamepad.rightShoulder)
                    }
                    microGamepad != null -> {
                        button(GameButton.START, microGamepad.buttonMenu)
                        button(GameButton.UP, microGamepad.dpad.up)
                        button(GameButton.DOWN, microGamepad.dpad.down)
                        button(GameButton.LEFT, microGamepad.dpad.left)
                        button(GameButton.RIGHT, microGamepad.dpad.right)
                        button(GameButton.GENERIC_BUTTON_DOWN, microGamepad.buttonA)
                        button(GameButton.GENERIC_BUTTON_LEFT, microGamepad.buttonX)
                        stick(leftStick, microGamepad.dpad)
                    }
                }

                gameWindow.dispatchGamepadUpdateAdd(
                    leftStick, rightStick,
                    buttonMask,
                    mapping,
                    controller.vendorName,
                    controller.battery?.batteryLevel?.toDouble() ?: 1.0
                )
            }
            gameWindow.dispatchGamepadUpdateEnd()
        }
    }

    val knownControllers = mutableSetOf<GCController>()

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

open class IosGameWindow : GameWindow() {
    override val dialogInterface = DialogInterfaceIos()

    override val ag: AG = IosAGNative()

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

    companion object {
        fun getGameWindow() = MyIosGameWindow
    }
}

val MyIosGameWindow = IosGameWindow() // Creates instance everytime

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
