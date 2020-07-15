package com.soywiz.korge.gradle.targets.js

import com.soywiz.korge.gradle.targets.*
import kotlin.test.*

class JavaScriptTest {
	@Test
	fun test() {
		val runtime = """
			  function isInheritanceFromInterface(ctor, iface) {
			    if (ctor === iface)
			      return true;
			    var metadata = ctor.${'$'}metadata${'$'};
			    if (metadata != null) {
			      var interfaces = metadata.interfaces;
			      for (var i = 0; i < interfaces.length; i++) {
			        if (isInheritanceFromInterface(interfaces[i], iface)) {
			          return true;
			        }}
			    }var superPrototype = ctor.prototype != null ? Object.getPrototypeOf(ctor.prototype) : null;
			    var superConstructor = superPrototype != null ? superPrototype.constructor : null;
			    return superConstructor != null && isInheritanceFromInterface(superConstructor, iface);
			  }
			  
		""".trimIndent()
		val res = applyPatchesToKotlinRuntime(runtime)
		assertTrue { res.contains("getAllInterfaces") }
		assertEquals(res.lines().count(), runtime.lines().count())
	}

	@Test
	fun test2() {
		val runtime = getResourceString("kotlin.js.raw")
		val res = applyPatchesToKotlinRuntime(runtime)
		assertTrue { res.contains("getAllInterfaces") }
		assertEquals(res.lines().count(), runtime.lines().count())
	}
}