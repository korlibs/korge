package com.soywiz.korge.intellij.debug

import com.intellij.debugger.impl.*
import com.sun.jdi.*
import com.sun.jdi.event.*
import com.sun.jdi.request.*
import org.junit.*
import java.io.*
import kotlin.time.*

class DebugExtTest {
	@UseExperimental(ExperimentalTime::class)
	@Test
	@Ignore
	fun test() {
		val manager: VirtualMachineManager = Bootstrap.virtualMachineManager()
		val connector = manager.defaultConnector()
		val args = connector.defaultArguments()
		val debugPath = File(File(".").absoluteFile, "debug")

		val MainClass = "HelloWorld"

		val vm = connector.launch(args.apply {
			this["suspend"]?.setValue("true")
			//this["suspend"]?.setValue("false")
			this["options"]?.setValue("-cp $debugPath")
			this["main"]?.setValue(MainClass)
			this["cwd"]?.setValue(debugPath.absolutePath)
		})
		val erm = vm.eventRequestManager()

		erm.createClassPrepareRequest().also {
			it.addClassFilter(MainClass)
			it.setSuspendPolicy(EventRequest.SUSPEND_ALL)
			it.enable()
		}

		val outStream = vm.process().inputStream
		val errStream = vm.process().errorStream

		Thread { outStream.copyTo(System.out) }.also { it.isDaemon = true }.start()
		Thread { errStream.copyTo(System.err) }.also { it.isDaemon = true }.start()

		//vm.resume()

		try {
			loop@while (true) {
				val evSet = vm.eventQueue().remove(10000L)

				println("evSet: $evSet")

				if (evSet != null) {
					for (ev in evSet) {
						println("ev: $ev")
						when (ev) {
							is VMStartEvent -> {
							}
							is VMDisconnectEvent -> {
								break@loop
							}
							is ClassPrepareEvent -> {
								val mainClass = vm.classesByName(MainClass).firstOrNull() ?: error("Can't find $MainClass class")
								val locations = mainClass.methodsByName("main").first().allLineLocations()
								val location = locations.last()
								erm.createBreakpointRequest(location).also {
									it.setSuspendPolicy(EventRequest.SUSPEND_ALL)
									it.enable()
								}
							}
							is BreakpointEvent -> {
								val thread = ev.thread()
								val frame = thread.frame(0)
								println("Stopped at : $thread, ${thread.frames()}")
								println(" -->" + frame.visibleVariables())
								val data = frame.visibleVariableByName("data")
								val dataValue = frame.getValue(data)

								val bytes0 = measureTimedValue { (dataValue as ArrayReference).readBytesArray3(thread) }
								println(bytes0.value!![0])
								println(bytes0.value!![10000])
								println(bytes0.value!![bytes0.value!!.size - 1])
								println("$bytes0")

								val bytes0b = measureTimedValue { DebuggerUtilsImplExt.fastReadBytesArray(dataValue, thread) }
								println(bytes0b.value!![0])
								println(bytes0b.value!![10000])
								println(bytes0b.value!![bytes0.value!!.size - 1])
								println("$bytes0b")

								val bytes1 = measureTimedValue { (dataValue as ArrayReference).convertToLocalBytes() }.duration
								//val bytes1b = measureTimedValue { (dataValue as ArrayReference).convertToLocalBytes() }.duration
								println("$bytes1")
								val bytes2 = measureTimedValue { DebuggerUtilsImpl.readBytesArray(dataValue) }.duration
								//val bytes2b = measureTimedValue { DebuggerUtilsImpl.readBytesArray(dataValue) }.duration
								println("$bytes2")
								//val bytes3 = measureTimedValue { readBytesArray2(dataValue) }.duration
								//println("$bytes3")
							}
						}
					}
					if (evSet.suspendPolicy() == EventRequest.SUSPEND_ALL) {
						evSet.resume()
					}
				}

				//val helloWorld = vm.getRemoteClass("HelloWorld") ?: error("Can't find HelloWorld class")
			}
		} catch (e: VMDisconnectedException) {

		}

		/*

		//vm.eventRequestManager()
		println(erm.classPrepareRequests())
		Thread.sleep(1000L)
		println(erm.classPrepareRequests())

		val helloWorld = vm.classesByName("HelloWorld").firstOrNull() ?: error("Can't find HelloWorld class")

		//val helloWorld = vm.getRemoteClass("HelloWorld") ?: error("Can't find HelloWorld class")

		//erm.createBreakpointRequest()

		println(vm.allThreads().first().frames())
		*/
		//vm.allThreads().first().
		//vm.resume()
	}


	// Performance compared to others (128 MB of data):
	// - This: 2.14s
	// - Encoding/Decoding Base64: 2.49s
	// - DebuggerUtilsImpl.readBytesArray (getValues+boxing): 14.3s
	// This should use COMPACT_STRINGS on newer java versions, thus String could be backed by a normal byte[] greatly reducing memory usage too
	fun ArrayReference.readBytesArray3(thread: ThreadReference): ByteArray? {
		// java.lang.String(byteArrayOf(1, 2, 3, 4), 0).getBytes(0, 4, ByteArray(4), 0)
		val vm = virtualMachine()
		val type = this.type()
		if (type.signature() != "[B") return null
		val StringClass = this.virtualMachine().classesByName("java.lang.String").firstOrNull() as? ClassType? ?: error("Can't get java.lang.String")
		val constructor = StringClass.methodsByName("<init>", "([BI)V").firstOrNull() ?: error("Can't get java.lang.String(byte[], int) constructor")
		val string = StringClass.newInstance(thread, constructor, listOf(this, vm.mirrorOf(0)), 0)
		val str = ((string as StringReference).value() as java.lang.String)
		val out = ByteArray(str.length)
		str.getBytes(0, str.length, out, 0)
		return out
		//println(string)
	}

}
