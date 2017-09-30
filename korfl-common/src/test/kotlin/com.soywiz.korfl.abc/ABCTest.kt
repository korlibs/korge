package com.soywiz.korfl.abc

import com.soywiz.korio.stream.openSync
import com.soywiz.korio.util.fromHexChunks
import org.junit.Test

class ABCTest {
	@Test
	fun name() {
		val data = listOf(
			"10 00 2e 00 00",        // *************.** |
			"00 00 11 0b 61 73 33 74 65 73 74 5f 66 6c 61 0c",        // ****as3test_fla* |
			"4d 61 69 6e 54 69 6d 65 6c 69 6e 65 0d 66 6c 61",        // MainTimeline*fla |
			"73 68 2e 64 69 73 70 6c 61 79 09 4d 6f 76 69 65",        // sh.display*Movie |
			"43 6c 69 70 18 61 73 33 74 65 73 74 5f 66 6c 61",        // Clip*as3test_fla |
			"3a 4d 61 69 6e 54 69 6d 65 6c 69 6e 65 06 66 72",        // :MainTimeline*fr |
			"61 6d 65 32 00 04 73 74 6f 70 0e 61 64 64 46 72",        // ame2**stop*addFr |
			"61 6d 65 53 63 72 69 70 74 06 4f 62 6a 65 63 74",        // ameScript*Object |
			"0c 66 6c 61 73 68 2e 65 76 65 6e 74 73 0f 45 76",        // *flash.events*Ev |
			"65 6e 74 44 69 73 70 61 74 63 68 65 72 0d 44 69",        // entDispatcher*Di |
			"73 70 6c 61 79 4f 62 6a 65 63 74 11 49 6e 74 65",        // splayObject*Inte |
			"72 61 63 74 69 76 65 4f 62 6a 65 63 74 16 44 69",        // ractiveObject*Di |
			"73 70 6c 61 79 4f 62 6a 65 63 74 43 6f 6e 74 61",        // splayObjectConta |
			"69 6e 65 72 06 53 70 72 69 74 65 07 16 01 16 03",        // iner*Sprite***** |
			"18 05 17 01 16 07 16 0b 00 0c 07 01 02 07 02 04",        // **************** |
			"07 04 06 07 05 08 07 05 09 07 05 0a 07 06 0c 07",        // **************** |
			"02 0d 07 02 0e 07 02 0f 07 02 10 04 00 00 00 00",        // **************** |
			"00 00 00 00 00 00 00 00 00 00 00 00 00 01 01 02",        // **************** |
			"08 03 00 02 01 03 01 00 01 00 00 01 03 01 01 04",        // **************** |
			"01 00 04 00 01 01 09 0a 03 d0 30 47 00 00 01 01",        // **********0G**** |
			"01 0a 0b 08 d0 30 5d 04 4f 04 00 47 00 00 02 03",        // *****0]*O**G**** |
			"01 0a 0b 10 d0 30 d0 49 00 5d 05 24 01 d0 66 03",        // *****0*I*]*$**f* |
			"4f 05 02 47 00 00 03 02 01 01 09 27 d0 30 65 00",        // O**G*******'*0e* |
			"60 06 30 60 07 30 60 08 30 60 09 30 60 0a 30 60",        // `*0`*0`*0`*0`*0` |
			"0b 30 60 02 30 60 02 58 00 1d 1d 1d 1d 1d 1d 1d",        // *0`*0`*X******** |
			"68 01 47 00 00 		                        "       //  h*G** |
		).fromHexChunks()

		val abc = ABC().readFile(data.openSync())
		for (type in abc.typesInfo) {
			for (trait in type.instanceTraits) {
				println(trait.name)
			}
		}
	}
}
