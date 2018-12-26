package com.soywiz.korge.tween

import kotlin.math.*

@Suppress("unused")
interface Easing {
	operator fun invoke(it: Double): Double

	companion object {
		fun cubic(f: (t: Double, b: Double, c: Double, d: Double) -> Double): Easing = Easing { f(it, 0.0, 1.0, 1.0) }
		fun combine(start: Easing, end: Easing) =
			Easing { if (it < 0.5) 0.5 * start(it * 2.0) else 0.5 * end((it - 0.5) * 2.0) + 0.5 }

		operator fun invoke(f: (Double) -> Double) = object : Easing {
			override fun invoke(it: Double): Double = f(it)
		}

		val EASE_IN_ELASTIC get() = Easings.EASE_IN_ELASTIC
		val EASE_OUT_ELASTIC get() = Easings.EASE_OUT_ELASTIC
		val EASE_OUT_BOUNCE get() = Easings.EASE_OUT_BOUNCE
		val LINEAR get() = Easings.LINEAR
		val EASE_IN get() = Easings.EASE_IN
		val EASE_OUT get() = Easings.EASE_OUT
		val EASE_IN_OUT get() = Easings.EASE_IN_OUT
		val EASE_OUT_IN get() = Easings.EASE_OUT_IN
		val EASE_IN_BACK get() = Easings.EASE_IN_BACK
		val EASE_OUT_BACK get() = Easings.EASE_OUT_BACK
		val EASE_IN_OUT_BACK get() = Easings.EASE_IN_OUT_BACK
		val EASE_OUT_IN_BACK get() = Easings.EASE_OUT_IN_BACK
		val EASE_IN_OUT_ELASTIC get() = Easings.EASE_IN_OUT_ELASTIC
		val EASE_OUT_IN_ELASTIC get() = Easings.EASE_OUT_IN_ELASTIC
		val EASE_IN_BOUNCE get() = Easings.EASE_IN_BOUNCE
		val EASE_IN_OUT_BOUNCE get() = Easings.EASE_IN_OUT_BOUNCE
		val EASE_OUT_IN_BOUNCE get() = Easings.EASE_OUT_IN_BOUNCE
		val EASE_IN_QUAD get() = Easings.EASE_IN_QUAD
		val EASE_OUT_QUAD get() = Easings.EASE_OUT_QUAD
		val EASE_IN_OUT_QUAD get() = Easings.EASE_IN_OUT_QUAD
		val EASE_SINE get() = Easings.EASE_SINE
	}
}

private object Easings {
	private const val BOUNCE_10 = 1.70158

	val EASE_IN_ELASTIC = Easing {
		if (it == 0.0 || it == 1.0) it else {
			val p = 0.3
			val s = p / 4.0
			val inv = it - 1
			-1.0 * 2.0.pow(10.0 * inv) * sin((inv - s) * (2.0 * PI) / p)
		}
	}

	val EASE_OUT_ELASTIC = Easing {
		if (it == 0.0 || it == 1.0) it else {
			val p = 0.3
			val s = p / 4.0
			2.0.pow(-10.0 * it) * sin((it - s) * (2.0 * PI) / p) + 1
		}
	}

	val EASE_OUT_BOUNCE = Easing {
		val s = 7.5625
		val p = 2.75
		if (it < (1.0 / p)) {
			s * it.pow(2.0)
		} else if (it < (2.0 / p)) {
			s * (it - 1.5 / p).pow(2.0) + 0.75
		} else if (it < 2.5 / p) {
			s * (it - 2.25 / p).pow(2.0) + 0.9375
		} else {
			s * (it - 2.625 / p).pow(2.0) + 0.984375
		}
	}

	val LINEAR = Easing { it }
	val EASE_IN = Easing { it * it * it }
	val EASE_OUT = Easing { val inv = it - 1.0; inv * inv * inv + 1 }
	val EASE_IN_OUT = Easing.combine(EASE_IN, EASE_OUT)
	val EASE_OUT_IN = Easing.combine(EASE_OUT, EASE_IN)
	val EASE_IN_BACK = Easing { it.pow(2.0) * ((BOUNCE_10 + 1.0) * it - BOUNCE_10) }
	val EASE_OUT_BACK = Easing { val inv = it - 1.0; inv.pow(2.0) * ((BOUNCE_10 + 1.0) * inv + BOUNCE_10) + 1.0 }
	val EASE_IN_OUT_BACK = Easing.combine(EASE_IN_BACK, EASE_OUT_BACK)
	val EASE_OUT_IN_BACK = Easing.combine(EASE_OUT_BACK, EASE_IN_BACK)
	val EASE_IN_OUT_ELASTIC = Easing.combine(EASE_IN_ELASTIC, EASE_OUT_ELASTIC)
	val EASE_OUT_IN_ELASTIC = Easing.combine(EASE_OUT_ELASTIC, EASE_IN_ELASTIC)
	val EASE_IN_BOUNCE = Easing { 1.0 - EASE_OUT_BOUNCE(1.0 - it) }
	val EASE_IN_OUT_BOUNCE = Easing.combine(EASE_IN_BOUNCE, EASE_OUT_BOUNCE)
	val EASE_OUT_IN_BOUNCE = Easing.combine(EASE_OUT_BOUNCE, EASE_IN_BOUNCE)
	val EASE_IN_QUAD = Easing { 1.0 * it * it }
	val EASE_OUT_QUAD = Easing { -1.0 * it * (it - 2) }
	val EASE_IN_OUT_QUAD =
		Easing { val t = it * 2.0; if (t < 1) (1.0 / 2 * t * t) else (-1.0 / 2 * ((t - 1) * ((t - 1) - 2) - 1)) }

	val EASE_SINE = Easing { sin(it) }
}

/*

in another module:

launchImmediately {
	//pe.simulator.emitterPos = IPoint(-10, 0)
	//pe.tween(pe::x[400], time = 3.seconds, easing = Easing.EASE_OUT)
	//pe.tween(pe::x[400], time = 3.seconds)
	//pe.tween(pe::x[400], time = 3.seconds, easing = easing)
	pe.tween(pe::x[400], time = 3.seconds, easing = Easing.EASE_OUT)
}

Potential problem with `inline val EASE_OUT get() = Easings.EASE_OUT` trying to resolve since Easing.Companion links to Easings and Easings links to Easing?
Kotlin 1.3.11

e: java.lang.NullPointerException
        at org.jetbrains.kotlin.com.google.gwt.dev.js.JsAstMapper.mapFunction(JsAstMapper.java:562)
        at org.jetbrains.kotlin.js.parser.ParserUtilsKt.parseFunction(parserUtils.kt:74)
        at org.jetbrains.kotlin.js.inline.FunctionReader.readFunctionFromSource(FunctionReader.kt:236)
        at org.jetbrains.kotlin.js.inline.FunctionReader.readFunction(FunctionReader.kt:199)
        at org.jetbrains.kotlin.js.inline.FunctionReader.access$readFunction(FunctionReader.kt:58)
        at org.jetbrains.kotlin.js.inline.FunctionReader$functionCache$1.createValue(FunctionReader.kt:185)
        at org.jetbrains.kotlin.js.inline.FunctionReader$functionCache$1.createValue(FunctionReader.kt:183)
        at org.jetbrains.kotlin.com.intellij.util.containers.SLRUCache.get(SLRUCache.java:46)
        at org.jetbrains.kotlin.js.inline.FunctionReader.get(FunctionReader.kt:189)
        at org.jetbrains.kotlin.js.inline.context.FunctionContext.lookUpFunctionExternal(FunctionContext.kt:108)
        at org.jetbrains.kotlin.js.inline.context.FunctionContext.getFunctionDefinitionImpl(FunctionContext.kt:76)
        at org.jetbrains.kotlin.js.inline.context.FunctionContext.hasFunctionDefinition(FunctionContext.kt:39)
        at org.jetbrains.kotlin.js.inline.JsInliner.hasToBeInlined(JsInliner.java:570)
        at org.jetbrains.kotlin.js.inline.JsInliner.lambda$new$0(JsInliner.java:61)
        at org.jetbrains.kotlin.js.inline.ExpressionDecomposerKt$match$visitor$1.doTraverse(ExpressionDecomposer.kt:480)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl$ListContext.traverse(JsVisitorWithContextImpl.java:88)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doAcceptList(JsVisitorWithContextImpl.java:177)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContext.acceptList(JsVisitorWithContext.java:48)
        at org.jetbrains.kotlin.js.backend.ast.JsInvocation.traverse(JsInvocation.java:56)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doTraverse(JsVisitorWithContextImpl.java:187)
        at org.jetbrains.kotlin.js.inline.ExpressionDecomposerKt$match$visitor$1.doTraverse(ExpressionDecomposer.kt:478)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl$NodeContext.traverse(JsVisitorWithContextImpl.java:136)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doAccept(JsVisitorWithContextImpl.java:147)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContext.accept(JsVisitorWithContext.java:38)
        at org.jetbrains.kotlin.js.backend.ast.JsExpressionStatement.traverse(JsExpressionStatement.java:50)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doTraverse(JsVisitorWithContextImpl.java:187)
        at org.jetbrains.kotlin.js.inline.ExpressionDecomposerKt$match$visitor$1.doTraverse(ExpressionDecomposer.kt:478)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl$NodeContext.traverse(JsVisitorWithContextImpl.java:136)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doAccept(JsVisitorWithContextImpl.java:147)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContext.accept(JsVisitorWithContext.java:38)
        at org.jetbrains.kotlin.js.inline.ExpressionDecomposerKt.match(ExpressionDecomposer.kt:486)
        at org.jetbrains.kotlin.js.inline.ExpressionDecomposerKt.access$match(ExpressionDecomposer.kt:1)
        at org.jetbrains.kotlin.js.inline.ExpressionDecomposer$Companion.preserveEvaluationOrder(ExpressionDecomposer.kt:64)
        at org.jetbrains.kotlin.js.inline.ExpressionDecomposer.preserveEvaluationOrder(ExpressionDecomposer.kt)
        at org.jetbrains.kotlin.js.inline.JsInliner.doAcceptStatementList(JsInliner.java:329)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContext.acceptStatementList(JsVisitorWithContext.java:59)
        at org.jetbrains.kotlin.js.backend.ast.JsBlock.traverse(JsBlock.java:63)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doTraverse(JsVisitorWithContextImpl.java:187)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl$ListContext.traverse(JsVisitorWithContextImpl.java:88)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doAcceptStatementList(JsVisitorWithContextImpl.java:171)
        at org.jetbrains.kotlin.js.inline.JsInliner.doAcceptStatementList(JsInliner.java:335)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContext.acceptStatementList(JsVisitorWithContext.java:59)
        at org.jetbrains.kotlin.js.backend.ast.JsBlock.traverse(JsBlock.java:63)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doTraverse(JsVisitorWithContextImpl.java:187)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl$ListContext.traverse(JsVisitorWithContextImpl.java:88)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doAcceptStatementList(JsVisitorWithContextImpl.java:171)
        at org.jetbrains.kotlin.js.inline.JsInliner.doAcceptStatementList(JsInliner.java:335)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doAcceptStatement(JsVisitorWithContextImpl.java:158)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContext.acceptStatement(JsVisitorWithContext.java:55)
        at org.jetbrains.kotlin.js.backend.ast.JsFunction.traverse(JsFunction.java:89)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doTraverse(JsVisitorWithContextImpl.java:187)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl$NodeContext.traverse(JsVisitorWithContextImpl.java:136)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doAccept(JsVisitorWithContextImpl.java:147)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContext.accept(JsVisitorWithContext.java:38)
        at org.jetbrains.kotlin.js.backend.ast.JsReturn.traverse(JsReturn.java:47)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doTraverse(JsVisitorWithContextImpl.java:187)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl$ListContext.traverse(JsVisitorWithContextImpl.java:88)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doAcceptStatementList(JsVisitorWithContextImpl.java:171)
        at org.jetbrains.kotlin.js.inline.JsInliner.doAcceptStatementList(JsInliner.java:335)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContext.acceptStatementList(JsVisitorWithContext.java:59)
        at org.jetbrains.kotlin.js.backend.ast.JsBlock.traverse(JsBlock.java:63)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doTraverse(JsVisitorWithContextImpl.java:187)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl$ListContext.traverse(JsVisitorWithContextImpl.java:88)
        at org.jetbrains.kotlin.js.inline.JsInliner.visit(JsInliner.java:262)
        at org.jetbrains.kotlin.js.inline.JsInliner.visit(JsInliner.java:165)
        at org.jetbrains.kotlin.js.backend.ast.JsFunction.traverse(JsFunction.java:87)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doTraverse(JsVisitorWithContextImpl.java:187)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl$NodeContext.traverse(JsVisitorWithContextImpl.java:136)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doAccept(JsVisitorWithContextImpl.java:147)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContext.accept(JsVisitorWithContext.java:38)
        at org.jetbrains.kotlin.js.backend.ast.JsExpressionStatement.traverse(JsExpressionStatement.java:50)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doTraverse(JsVisitorWithContextImpl.java:187)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl$ListContext.traverse(JsVisitorWithContextImpl.java:88)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doAcceptStatementList(JsVisitorWithContextImpl.java:171)
        at org.jetbrains.kotlin.js.inline.JsInliner.doAcceptStatementList(JsInliner.java:335)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContext.acceptStatementList(JsVisitorWithContext.java:59)
        at org.jetbrains.kotlin.js.backend.ast.JsBlock.traverse(JsBlock.java:63)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doTraverse(JsVisitorWithContextImpl.java:187)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl$ListContext.traverse(JsVisitorWithContextImpl.java:88)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doAcceptStatementList(JsVisitorWithContextImpl.java:171)
        at org.jetbrains.kotlin.js.inline.JsInliner.doAcceptStatementList(JsInliner.java:335)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContextImpl.doAcceptStatement(JsVisitorWithContextImpl.java:158)
        at org.jetbrains.kotlin.js.backend.ast.JsVisitorWithContext.acceptStatement(JsVisitorWithContext.java:55)
        at org.jetbrains.kotlin.js.inline.JsInliner.process(JsInliner.java:102)
        at org.jetbrains.kotlin.js.facade.K2JSTranslator.translateUnits(K2JSTranslator.java:145)
        at org.jetbrains.kotlin.js.facade.K2JSTranslator.translate(K2JSTranslator.java:99)
        at org.jetbrains.kotlin.cli.js.K2JSCompiler.translate(K2JSCompiler.java:152)
        at org.jetbrains.kotlin.cli.js.K2JSCompiler.doExecute(K2JSCompiler.java:286)
        at org.jetbrains.kotlin.cli.js.K2JSCompiler.doExecute(K2JSCompiler.java:82)
        at org.jetbrains.kotlin.cli.common.CLICompiler.execImpl(CLICompiler.java:96)
        at org.jetbrains.kotlin.cli.common.CLICompiler.execImpl(CLICompiler.java:52)
        at org.jetbrains.kotlin.cli.common.CLITool.exec(CLITool.kt:93)
        at org.jetbrains.kotlin.daemon.CompileServiceImpl$compile$1$1$2.invoke(CompileServiceImpl.kt:442)
        at org.jetbrains.kotlin.daemon.CompileServiceImpl$compile$1$1$2.invoke(CompileServiceImpl.kt:102)
        at org.jetbrains.kotlin.daemon.CompileServiceImpl$doCompile$$inlined$ifAlive$lambda$2.invoke(CompileServiceImpl.kt:1029)
        at org.jetbrains.kotlin.daemon.CompileServiceImpl$doCompile$$inlined$ifAlive$lambda$2.invoke(CompileServiceImpl.kt:102)
        at org.jetbrains.kotlin.daemon.common.DummyProfiler.withMeasure(PerfUtils.kt:137)
        at org.jetbrains.kotlin.daemon.CompileServiceImpl.checkedCompile(CompileServiceImpl.kt:1071)
        at org.jetbrains.kotlin.daemon.CompileServiceImpl.doCompile(CompileServiceImpl.kt:1028)
        at org.jetbrains.kotlin.daemon.CompileServiceImpl.compile(CompileServiceImpl.kt:441)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)
        at sun.rmi.server.UnicastServerRef.dispatch(UnicastServerRef.java:357)
        at sun.rmi.transport.Transport$1.run(Transport.java:200)
        at sun.rmi.transport.Transport$1.run(Transport.java:197)
        at java.security.AccessController.doPrivileged(Native Method)
        at sun.rmi.transport.Transport.serviceCall(Transport.java:196)
        at sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:573)
        at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:834)
        at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.lambda$run$0(TCPTransport.java:688)
        at java.security.AccessController.doPrivileged(Native Method)
        at sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:687)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
        at java.lang.Thread.run(Thread.java:748)

@Suppress("unused")
interface Easing {
	operator fun invoke(it: Double): Double

	companion object {
		fun cubic(f: (t: Double, b: Double, c: Double, d: Double) -> Double): Easing = Easing { f(it, 0.0, 1.0, 1.0) }
		fun combine(start: Easing, end: Easing) =
			Easing { if (it < 0.5) 0.5 * start(it * 2.0) else 0.5 * end((it - 0.5) * 2.0) + 0.5 }

		operator fun invoke(f: (Double) -> Double) = object : Easing {
			override fun invoke(it: Double): Double = f(it)
		}

		inline val EASE_IN_ELASTIC get() = Easings.EASE_IN_ELASTIC
		inline val EASE_OUT_ELASTIC get() = Easings.EASE_OUT_ELASTIC
		inline val EASE_OUT_BOUNCE get() = Easings.EASE_OUT_BOUNCE
		inline val LINEAR get() = Easings.LINEAR
		inline val EASE_IN get() = Easings.EASE_IN
		inline val EASE_OUT get() = Easings.EASE_OUT
		inline val EASE_IN_OUT get() = Easings.EASE_IN_OUT
		inline val EASE_OUT_IN get() = Easings.EASE_OUT_IN
		inline val EASE_IN_BACK get() = Easings.EASE_IN_BACK
		inline val EASE_OUT_BACK get() = Easings.EASE_OUT_BACK
		inline val EASE_IN_OUT_BACK get() = Easings.EASE_IN_OUT_BACK
		inline val EASE_OUT_IN_BACK get() = Easings.EASE_OUT_IN_BACK
		inline val EASE_IN_OUT_ELASTIC get() = Easings.EASE_IN_OUT_ELASTIC
		inline val EASE_OUT_IN_ELASTIC get() = Easings.EASE_OUT_IN_ELASTIC
		inline val EASE_IN_BOUNCE get() = Easings.EASE_IN_BOUNCE
		inline val EASE_IN_OUT_BOUNCE get() = Easings.EASE_IN_OUT_BOUNCE
		inline val EASE_OUT_IN_BOUNCE get() = Easings.EASE_OUT_IN_BOUNCE
		inline val EASE_IN_QUAD get() = Easings.EASE_IN_QUAD
		inline val EASE_OUT_QUAD get() = Easings.EASE_OUT_QUAD
		inline val EASE_IN_OUT_QUAD get() = Easings.EASE_IN_OUT_QUAD
		inline val EASE_SINE get() = Easings.EASE_SINE
	}
}

private object Easings {
	private const val BOUNCE_10 = 1.70158

	val EASE_IN_ELASTIC = Easing {
		if (it == 0.0 || it == 1.0) it else {
			val p = 0.3
			val s = p / 4.0
			val inv = it - 1
			-1.0 * 2.0.pow(10.0 * inv) * sin((inv - s) * (2.0 * PI) / p)
		}
	}

	val EASE_OUT_ELASTIC = Easing {
		if (it == 0.0 || it == 1.0) it else {
			val p = 0.3
			val s = p / 4.0
			2.0.pow(-10.0 * it) * sin((it - s) * (2.0 * PI) / p) + 1
		}
	}

	val EASE_OUT_BOUNCE = Easing {
		val s = 7.5625
		val p = 2.75
		if (it < (1.0 / p)) {
			s * it.pow(2.0)
		} else if (it < (2.0 / p)) {
			s * (it - 1.5 / p).pow(2.0) + 0.75
		} else if (it < 2.5 / p) {
			s * (it - 2.25 / p).pow(2.0) + 0.9375
		} else {
			s * (it - 2.625 / p).pow(2.0) + 0.984375
		}
	}

	val LINEAR = Easing { it }
	val EASE_IN = Easing { it * it * it }
	val EASE_OUT = Easing { val inv = it - 1.0; inv * inv * inv + 1 }
	val EASE_IN_OUT = Easing.combine(EASE_IN, EASE_OUT)
	val EASE_OUT_IN = Easing.combine(EASE_OUT, EASE_IN)
	val EASE_IN_BACK = Easing { it.pow(2.0) * ((BOUNCE_10 + 1.0) * it - BOUNCE_10) }
	val EASE_OUT_BACK = Easing { val inv = it - 1.0; inv.pow(2.0) * ((BOUNCE_10 + 1.0) * inv + BOUNCE_10) + 1.0 }
	val EASE_IN_OUT_BACK = Easing.combine(EASE_IN_BACK, EASE_OUT_BACK)
	val EASE_OUT_IN_BACK = Easing.combine(EASE_OUT_BACK, EASE_IN_BACK)
	val EASE_IN_OUT_ELASTIC = Easing.combine(EASE_IN_ELASTIC, EASE_OUT_ELASTIC)
	val EASE_OUT_IN_ELASTIC = Easing.combine(EASE_OUT_ELASTIC, EASE_IN_ELASTIC)
	val EASE_IN_BOUNCE = Easing { 1.0 - EASE_OUT_BOUNCE(1.0 - it) }
	val EASE_IN_OUT_BOUNCE = Easing.combine(EASE_IN_BOUNCE, EASE_OUT_BOUNCE)
	val EASE_OUT_IN_BOUNCE = Easing.combine(EASE_OUT_BOUNCE, EASE_IN_BOUNCE)
	val EASE_IN_QUAD = Easing { 1.0 * it * it }
	val EASE_OUT_QUAD = Easing { -1.0 * it * (it - 2) }
	val EASE_IN_OUT_QUAD =
		Easing { val t = it * 2.0; if (t < 1) (1.0 / 2 * t * t) else (-1.0 / 2 * ((t - 1) * ((t - 1) - 2) - 1)) }

	val EASE_SINE = Easing { sin(it) }
}

 */

