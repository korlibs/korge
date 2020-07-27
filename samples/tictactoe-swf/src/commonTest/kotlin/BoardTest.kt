import kotlin.test.*

class BoardTest {
	@Test
	fun test() {
		assertEquals(null, Board("""
			...
    		...
			...
		""").winner)

		assertEquals(Chip.CROSS, Board("""
			.X.
    		.X.
			.X.
		""").winner)

	}
}
