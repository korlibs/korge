package com.soywiz.korge.intellij.debug;

import com.intellij.debugger.engine.DebugProcess;
import com.sun.jdi.*;

import java.util.Arrays;
import java.util.List;

public class DebuggerUtilsImplExt {
	// Performance compared to others (128 MB of data):
	// - This: 2.14s
	// - Encoding/Decoding Base64: 2.49s
	// - DebuggerUtilsImpl.readBytesArray (getValues+boxing): 14.3s
	// This should use COMPACT_STRINGS on newer java versions, thus String could be backed by a normal byte[] greatly reducing memory usage too
	static public byte[] fastReadBytesArray(Value that, ThreadReference thread) {
		try {
			final VirtualMachine vm = that.virtualMachine();
			final Type type = that.type();
			if (type == null || !type.signature().equals("[B")) return null;
			ClassType StringClass = (ClassType) that.virtualMachine().classesByName("java.lang.String").get(0);
			Method constructor = StringClass.methodsByName("<init>", "([BI)V").get(0);
			ObjectReference string = StringClass.newInstance(thread, constructor, Arrays.asList(that, vm.mirrorOf(0)), 0);
			String str = ((StringReference)(string)).value();
			byte[] out = new byte[str.length()];
			//noinspection deprecation
			str.getBytes(0, str.length(), out, 0);
			return out;
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
}
