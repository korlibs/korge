package com.soywiz.korgw

import com.soywiz.kds.Pool
import com.soywiz.kgl.checkedIf
import com.soywiz.korag.*
import com.soywiz.korag.gl.*

import com.soywiz.klogger.Console
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRect
import platform.EAGL.EAGLContext
import platform.EAGL.kEAGLRenderingAPIOpenGLES2
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.GLKit.GLKView
import platform.GLKit.GLKViewController
import platform.GLKit.GLKViewDrawableDepthFormat24
import platform.GLKit.GLKViewDrawableStencilFormat8
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIEvent
import platform.UIKit.UIScreen
import platform.UIKit.UITouch
import platform.UIKit.UIViewController
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
class ViewController(val entry: suspend () -> Unit) : UIViewController(null, null) {
    // Keep references to avoid collecting instances
    lateinit var glXViewController: MyGLKViewController

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
    }
}

@OptIn(UnsafeNumber::class)
@ExportObjCClass
class MyGLKViewController(val entry: suspend () -> Unit)  : GLKViewController(null, null) {
    override fun viewDidLoad() {
        val view = this.view as? GLKView?
        view?.multipleTouchEnabled = true
        view?.drawableDepthFormat = GLKViewDrawableDepthFormat24
        view?.drawableStencilFormat = GLKViewDrawableStencilFormat8
        view?.context = EAGLContext(kEAGLRenderingAPIOpenGLES2)
        initialized = false
        reshape = true
    }

    var value = 0

    var initialized = false
    var reshape = false
    private var myContext: EAGLContext? = null
    val gameWindow: IosGameWindow get() = MyIosGameWindow
    val touches = arrayListOf<UITouch>()
    val freeIds = Pool { it }
    val touchesIds = arrayListOf<Int>()
    var lastTouchId = 0

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

            val width = view.frame.useContents { this.size.width.toDouble() }
            val height = view.frame.useContents { this.size.height.toDouble() }

            Console.info("dispatchInitEvent")
            gameWindow.dispatchInitEvent()
            gameWindow.entry {
                Console.info("Executing entry...")
                this.entry()
            }
            this.reshape = true
        }

        // Context changed!
        val currentContext = EAGLContext.currentContext()
        if (myContext != currentContext) {
            Console.info("myContext = $myContext")
            Console.info("currentContext = $currentContext")
            myContext = currentContext
            gameWindow.ag.contextLost()
        }

        val width = view.bounds.useContents { size.width } * view.contentScaleFactor
        val height = view.bounds.useContents { size.height } * view.contentScaleFactor
        if (this.reshape) {
            this.reshape = false
            gameWindow.dispatchReshapeEvent(0, 0, width.toInt(), height.toInt())
        }

        /*
        this.value++
        glDisable(GL_SCISSOR_TEST)
        glDisable(GL_STENCIL_TEST)
        glViewport(0, 0, 200, 300)
        glScissor(0, 0, 200, 300)
        glClearColor((this.value % 100).toFloat() / 100f, 0f, 1f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)
         */

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

open class IosGameWindow : GameWindow() {
    override val dialogInterface = DialogInterfaceIos()

    override val ag: AG = object : AGNative() {
        override val gl: com.soywiz.kgl.KmlGl = com.soywiz.kgl.KmlGlNative(gles = true).checkedIf(checked = false)
    }

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
