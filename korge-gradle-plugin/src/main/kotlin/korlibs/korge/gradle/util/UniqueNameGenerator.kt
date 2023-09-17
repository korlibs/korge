package korlibs.korge.gradle.util

class UniqueNameGenerator {
    val nameToSuffix = LinkedHashMap<String, Int>()

    operator fun get(name: String): String {
        if (name !in nameToSuffix) {
            nameToSuffix[name] = -1
            return name
        } else {
            val index = nameToSuffix[name]!! + 1
            nameToSuffix[name] = index
            return get("$name$index")
        }
    }
}
