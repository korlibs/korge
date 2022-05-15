package com.soywiz.korge.lipsync

class RhubarbLipSyncTest {
    /*
	@Test
	fun test1() = suspendTest {
		RhubarbLipSyncProcessor.process(ResourcesVfs["simple.wav"].readAudioData())
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

        val file = Json.decodeToType<RhubarbLipSyncProcessor.RhubarbFile>(json)
        Assert.assertEquals(null, file.findCue(-0.01))
        Assert.assertEquals(RhubarbLipSyncProcessor.MouthCue(0.0, 0.01, "X"), file.findCue(0.00))
        Assert.assertEquals(RhubarbLipSyncProcessor.MouthCue(0.01, 0.07, "A"), file.findCue(0.04))
        Assert.assertEquals(RhubarbLipSyncProcessor.MouthCue(1.47, 1.68, "F"), file.findCue(1.50))
        Assert.assertEquals(null, file.findCue(3.49))
        Assert.assertEquals(null, file.findCue(4.0))
        Assert.assertEquals(
            "XAAAACCCCCCCBBBBBBBBBFFFFFFFFFFFFFFFFFFFFFFBBBBBBBBBBBBBBBBBBBBBBBBBCCCCBBBBBBBCCCCCCCCCCCEEFFFFFFFFFFFFFBBBBBBBBBBBBBBBBBBFFFFFFFFFFFFFFFFFFFFFFBBBBBBBBBCCCCCCCCCCCCCBBBBGGGGBBBBBBBBBBBBBBBBBBBBBBBBBBBBCCCCCCCCCCCCCXXX",
            file.toLipString()
        )
    }
    */
}
