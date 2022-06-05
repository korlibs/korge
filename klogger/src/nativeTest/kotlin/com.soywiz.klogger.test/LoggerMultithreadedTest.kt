package com.soywiz.klogger.test

import com.soywiz.klogger.Logger
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.isFrozen
import kotlin.test.Test
import kotlin.test.assertTrue

class LoggerMultithreadedTest {

    @Test
    fun multithreaded() {
        val worker = Worker.start()
        val logger = Logger("WorkerLogger")
        val changeLoggerLevel: () -> Logger.Level = {
            logger.level = Logger.Level.INFO
            logger.level
        }
        val future = worker.execute(TransferMode.SAFE, { changeLoggerLevel.freeze() }) { it() }
        val result = future.result

        if (Platform.memoryModel != MemoryModel.EXPERIMENTAL) {
            assertTrue(logger.isFrozen)
        }
        assertTrue { result == Logger.Level.INFO }
        assertTrue { logger.level == Logger.Level.INFO }

        val logger2 = Logger("WorkerLogger")
        assertTrue { logger === logger2 }

        val getLoggerLevel: () -> Logger.Level = {
            logger2.level
        }
        val worker2 = Worker.start()
        val future2 = worker2.execute(TransferMode.SAFE, { getLoggerLevel.freeze() }) { it() }
        val result2 = future2.result

        if (Platform.memoryModel != MemoryModel.EXPERIMENTAL) {
            assertTrue(logger2.isFrozen)
        }
        assertTrue { result2 == Logger.Level.INFO }
        assertTrue { logger2.level == Logger.Level.INFO }

        assertTrue { logger.level == logger2.level }

        logger.level = Logger.Level.DEBUG
        assertTrue { logger.level == Logger.Level.DEBUG }
        assertTrue { logger.level == logger2.level }
    }
}
