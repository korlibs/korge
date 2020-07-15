package com.soywiz.klogger

import android.util.Log

actual object DefaultLogOutput : Logger.Output {
    override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
        if (level == Logger.Level.NONE) return
        Log.println(when (level) {
            Logger.Level.NONE -> Log.VERBOSE
            Logger.Level.FATAL -> Log.ERROR
            Logger.Level.ERROR -> Log.ERROR
            Logger.Level.WARN -> Log.WARN
            Logger.Level.INFO -> Log.INFO
            Logger.Level.DEBUG -> Log.DEBUG
            Logger.Level.TRACE -> Log.VERBOSE
        }, logger.name, msg.toString())
    }
}
