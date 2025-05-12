package korlibs.korge.gradle

enum class GameCategory {
    ACTION, ADVENTURE, ARCADE, BOARD, CARD,
    CASINO, DICE, EDUCATIONAL, FAMILY, KIDS,
    MUSIC, PUZZLE, RACING, ROLE_PLAYING, SIMULATION,
    SPORTS, STRATEGY, TRIVIA, WORD;

    companion object {
        val VALUES: Map<String, GameCategory> = GameCategory.values().toList().associateBy { it.name.uppercase() }
        operator fun get(key: String): GameCategory? = VALUES[key.uppercase().trim()]
    }
}
