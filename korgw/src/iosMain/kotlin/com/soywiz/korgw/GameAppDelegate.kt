package com.soywiz.korgw

import kotlinx.cinterop.*
import platform.UIKit.*

@ExportObjCClass
class GameAppDelegate: UIResponder(), UIApplicationDelegateProtocol {
    override fun application(application: UIApplication, didFinishLaunchingWithOptions: Map<Any?, *>?): Boolean {
        return true
    }

    override fun applicationWillResignActive(application: UIApplication) {
    }

    override fun applicationDidEnterBackground(application: UIApplication) {
    }

    override fun applicationWillEnterForeground(application: UIApplication) {
    }

    override fun applicationDidBecomeActive(application: UIApplication) {
    }

    override fun applicationWillTerminate(application: UIApplication) {
    }
}
