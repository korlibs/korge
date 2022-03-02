package com.soywiz.korgw

import com.soywiz.korio.file.std.customCwd
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.EAGL.*
import platform.Foundation.*
import platform.GLKit.*
import platform.UIKit.*

interface GameEntryPoint {
    fun runMain()
}

private var globalGameEntryPoint: GameEntryPoint? = null

@ExportObjCClass
object GameEntryPointSet {
    fun set(entryPoint: GameEntryPoint) {
        com.soywiz.korgw.globalGameEntryPoint = entryPoint
    }
}

@ExportObjCClass
//class GameViewController() : GLKViewController() {
class GameViewController(nibName: kotlin.String?, bundle: platform.Foundation.NSBundle?) : GLKViewController(nibName, bundle) {
    //constructor() : this(null, null)
    //@OverrideInit constructor(coder: NSCoder) : super(coder)

    var context: EAGLContext? = null

    //deinit {
    //    self.tearDownGL()
    //    if EAGLContext.current() === self.context {
    //        EAGLContext.setCurrent(nil)
    //    }
    //}

    override fun viewDidLoad() {
        super.viewDidLoad()
        context = EAGLContext(kEAGLRenderingAPIOpenGLES2)
        if (context == null) {
            println("Failed to create ES context")
        }

        val view = this.view as GLKView
        view.context = this.context!!
        view.drawableDepthFormat = GLKViewDrawableDepthFormat24
        view.drawableStencilFormat = GLKViewDrawableStencilFormat8
        view.drawableMultisample = GLKViewDrawableMultisample4X
        this.setupGL()
    }

    override fun didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()

        if (this.isViewLoaded() && this.view.window != null) {
            //this.view = nil

            this.tearDownGL()


            if (EAGLContext.currentContext() === this.context) {
                EAGLContext.setCurrentContext(null)
            }
            this.context = null
        }
    }

    open fun setupGL() {
        EAGLContext.setCurrentContext(this.context)

        // Change the working directory so that we can use C code to grab resource files
        val path = NSBundle.mainBundle.resourcePath
        if (path != null) {
            val rpath = "$path/include/app/resources"
            NSFileManager.defaultManager.changeCurrentDirectoryPath(rpath)
            customCwd = rpath
            //gameWindow2?.setCustomCwd(rpath)

        }

        engineInitialize()

        var width = 0.0
        var height = 0.0

        view.frame.useContents {
            width = this.size.width.toDouble()
            height = this.size.width.toDouble()
        }
        engineResize(width, height)
    }

    open fun tearDownGL() {
        EAGLContext.setCurrentContext(context)
        engineFinalize()
    }

    open fun engineInitialize() {
    }

    open fun engineFinalize() {
    }

    open fun engineResize(width: Double, height: Double) {
    }

    var initialized = false
    var reshape = true
    //@ObjCMethod
    override fun glkView(view: GLKView, drawInRect: CValue<CGRect>) {
        if (!initialized) {
            initialized = true
            MyIosGameWindow.dispatchInitEvent()
            globalGameEntryPoint?.runMain()
            reshape = true
        }
        var width: Int = 0
        var height: Int = 0
        view.bounds.useContents {
            width = ((this.size.width * view.contentScaleFactor).toInt())
            height = ((this.size.height * view.contentScaleFactor).toInt())
        }
        if (reshape) {
            reshape = false
            MyIosGameWindow.dispatchReshapeEvent(0, 0, width, height)
        }
        MyIosGameWindow.handleReshapeEventIfRequired(0, 0, width, height)
        //MyIosGameWindow.ag.setViewport(0, 0, width, height)
        //gameWindow2?.gameWindow.ag.setViewport(0, 0, width, height)
        //gameWindow2?.gameWindow.frame()
        MyIosGameWindow.frame()
    }
}

/*
class ViewController: GLKViewController {
	var context: EAGLContext? = nil
	var gameWindow2: MyIosGameWindow2? = nil
	var rootGameMain: RootGameMain? = nil
	deinit {
		self.tearDownGL()
		if EAGLContext.current() === self.context {
			EAGLContext.setCurrent(nil)
		}
	}
	override func viewDidLoad() {
		super.viewDidLoad()
		self.gameWindow2 = MyIosGameWindow2.init()
		self.rootGameMain = RootGameMain.init()
		context = EAGLContext(api: .openGLES2)
		if context == nil {
			print("Failed to create ES context")
		}
		let view = self.view as! GLKView
		view.context = self.context!
		view.drawableDepthFormat = .format24
		self.setupGL()
	}
	override func didReceiveMemoryWarning() {
		super.didReceiveMemoryWarning()
		if self.isViewLoaded && self.view.window != nil {
			self.view = nil
			self.tearDownGL()
			if EAGLContext.current() === self.context {
				EAGLContext.setCurrent(nil)
			}
			self.context = nil
		}
	}
	func setupGL() {
		EAGLContext.setCurrent(self.context)
		if let path = Bundle.main.resourcePath {
			let rpath = "\(path)/include/app/resources"
			FileManager.default.changeCurrentDirectoryPath(rpath)
			self.gameWindow2?.setCustomCwd(cwd: rpath)
		}
		engineInitialize()
		let width = Float(view.frame.size.width) // * view.contentScaleFactor)
		let height = Float(view.frame.size.height) // * view.contentScaleFactor)
		engineResize(width: width, height: height)
	}
	func tearDownGL() {
		EAGLContext.setCurrent(self.context)
		engineFinalize()
	}
	var initialized = false
	var reshape = true
	override func glkView(_ view: GLKView, drawIn rect: CGRect) {
		if !initialized {
			initialized = true
			gameWindow2?.gameWindow.dispatchInitEvent()
			rootGameMain?.runMain()
			reshape = true
		}
		let width = Int32(view.bounds.width * view.contentScaleFactor)
		let height = Int32(view.bounds.height * view.contentScaleFactor)
		if reshape {
			reshape = false
			gameWindow2?.gameWindow.dispatchReshapeEvent(x: 0, y: 0, width: width, height: height)
		}
		gameWindow2?.gameWindow.ag.setViewport(x: 0, y: 0, width: width, height: height)
		gameWindow2?.gameWindow.frame()
	}
	private func engineInitialize() {
	}
	private func engineFinalize() {
	}
	private func engineResize(width: Float, height: Float) {
	}
}

 */
