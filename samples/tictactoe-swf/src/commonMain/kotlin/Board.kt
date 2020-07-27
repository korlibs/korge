import com.soywiz.kds.*
import com.soywiz.korma.geom.*

enum class Chip { EMPTY, CROSS, CIRCLE }

class Board(val width: Int = 3, val height: Int = width, val lineSize: Int = width) {
	class Cell(x: Int, y: Int) : Extra by Extra.Mixin() {
		val pos = PointInt(x, y)
		var value = Chip.EMPTY
	}

	val cells = Array2(width, height) { Cell(it % width, it / width) }

	fun inside(x: Int, y: Int) = cells.inside(x, y)

	fun select(x: Int, y: Int, dx: Int, dy: Int, size: Int): List<Cell>? {
		if (!inside(x, y)) return null
		if (!inside(x + dx * (size - 1), y + dy * (size - 1))) return null
		return (0 until size).map { cells[x + dx * it, y + dy * it] }
	}

	val lines = ArrayList<List<Cell>>()

	init {
		fun addLine(line: List<Cell>?) {
			if (line != null) lines += line
		}
		for (y in 0..height) {
			for (x in 0..width) {
				addLine(select(x, y, 1, 0, lineSize))
				addLine(select(x, y, 0, 1, lineSize))
				addLine(select(x, y, 1, 1, lineSize))
				addLine(select(width - x - 1, y, -1, 1, lineSize))
			}
		}
	}

	operator fun get(x: Int, y: Int) = cells[x, y]
	operator fun set(x: Int, y: Int, value: Chip) = run { cells[x, y].value = value }

	val Iterable<Cell>.chipLine: Chip?
		get() {
			val expected = this.first().value
			return if (expected == Chip.EMPTY) null else if (this.all { it.value == expected }) expected else null
		}

	val moreMovements: Boolean get() = cells.any { it.value == Chip.EMPTY }

	val winnerLine: List<Cell>?
		get() {
			val out = ArrayList<Cell>()
			for (line in lines) if (line.chipLine != null) out += line
			return if (out.isEmpty()) null else out.toSet().toList()
		}

	val winner: Chip?
		get() = winnerLine?.firstOrNull()?.value
}

fun Board(str: String): Board {
	val lines = str.trim().lines().map { it.trim() }
	return Board(lines.size, lines.size).also { board ->
		for (y in lines.indices) {
			val line = lines[y]
			for (x in line.indices) {
				board[x, y] = when (line[x]) {
					'O' ->  Chip.CIRCLE
					'X' ->  Chip.CROSS
					else -> Chip.EMPTY
				}
			}
		}
	}
}
