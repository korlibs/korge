package com.soywiz.korge.build.lipsync

import com.soywiz.korau.format.*
import com.soywiz.korge.animate.serialization.*
import com.soywiz.korge.build.*
import com.soywiz.korge.build.swf.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.serialization.json.*
import kotlin.test.*

class LipsyncResourceProcessorTest {
	@Ignore
	@Test
	fun test1() = suspendTest {
		LipsyncResourceProcessor.processAudioData(ResourcesVfs["simple.wav"].readAudioData())
	}

	@Test
	fun test2() = suspendTest {
		val json = """
	{
	  "metadata": {
		"soundFile": "C:/Users/soywiz/AppData/Local/Temp/rhubarb-file.wav",
		"duration": 3.49
	  },
	  "mouthCues": [
		{ "start": 0.00, "end": 0.01, "value": "X" },
		{ "start": 0.01, "end": 0.07, "value": "A" },
		{ "start": 0.07, "end": 0.19, "value": "C" },
		{ "start": 0.19, "end": 0.33, "value": "B" },
		{ "start": 0.33, "end": 0.68, "value": "F" },
		{ "start": 0.68, "end": 1.08, "value": "B" },
		{ "start": 1.08, "end": 1.14, "value": "C" },
		{ "start": 1.14, "end": 1.26, "value": "B" },
		{ "start": 1.26, "end": 1.43, "value": "C" },
		{ "start": 1.43, "end": 1.47, "value": "E" },
		{ "start": 1.47, "end": 1.68, "value": "F" },
		{ "start": 1.68, "end": 1.96, "value": "B" },
		{ "start": 1.96, "end": 2.31, "value": "F" },
		{ "start": 2.31, "end": 2.45, "value": "B" },
		{ "start": 2.45, "end": 2.66, "value": "C" },
		{ "start": 2.66, "end": 2.73, "value": "B" },
		{ "start": 2.73, "end": 2.80, "value": "G" },
		{ "start": 2.80, "end": 3.24, "value": "B" },
		{ "start": 3.24, "end": 3.45, "value": "C" },
		{ "start": 3.45, "end": 3.49, "value": "X" }
	  ]
	}
	""".trim()

		val file = Json.decodeToType<LipsyncResourceProcessor.RhubarbFile>(json)
		assertEquals(null, file.findCue(-0.01))
		assertEquals(LipsyncResourceProcessor.MouthCue(0.0, 0.01, "X"), file.findCue(0.00))
		assertEquals(LipsyncResourceProcessor.MouthCue(0.01, 0.07, "A"), file.findCue(0.04))
		assertEquals(LipsyncResourceProcessor.MouthCue(1.47, 1.68, "F"), file.findCue(1.50))
		assertEquals(null, file.findCue(3.49))
		assertEquals(null, file.findCue(4.0))
		assertEquals(
			"XAAAACCCCCCCBBBBBBBBBFFFFFFFFFFFFFFFFFFFFFFBBBBBBBBBBBBBBBBBBBBBBBBBCCCCBBBBBBBCCCCCCCCCCCEEFFFFFFFFFFFFFBBBBBBBBBBBBBBBBBBFFFFFFFFFFFFFFFFFFFFFFBBBBBBBBBCCCCCCCCCCCCCBBBBGGGGBBBBBBBBBBBBBBBBBBBBBBBBBBBBCCCCCCCCCCCCCXXX",
			file.toLipString()
		)
	}

	@Test
	@Ignore
	fun name() = suspendTest {
		val memoryVfs = MemoryVfs()
		val processed1 = LipsyncResourceProcessor.process(ResourcesVfs["wav1.voice.wav"], memoryVfs)
		assertEquals(true, processed1)
		println(memoryVfs.listRecursive().toList())
		assertEquals(true, memoryVfs["shapes.ani"].exists())
		assertEquals(true, memoryVfs["shapes.ani.meta"].exists())
		assertEquals(
			ResourceVersion(
				name = "shapes.swf",
				loaderVersion = AniFile.VERSION,
				sha1 = "7b8d9db612be09d0fc6f92e9eb2278bf00a62da3",
				configSha1 = "c012fd0b2067923af7d69c7bb690534c0ec7d246"
			),
			ResourceVersion.readMeta(memoryVfs["shapes.ani.meta"])
		)
		val processed2 = SwfResourceProcessor.process(ResourcesVfs["shapes.swf"], memoryVfs)
		assertEquals(false, processed2)
	}
}
