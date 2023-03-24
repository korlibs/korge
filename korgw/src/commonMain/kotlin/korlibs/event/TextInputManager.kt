package korlibs.event

interface TextInputPosition : Comparator<TextInputPosition> {
}

interface TextInputRange {
    val start: TextInputPosition
    val end: TextInputPosition
}

interface TextInputManager {
}