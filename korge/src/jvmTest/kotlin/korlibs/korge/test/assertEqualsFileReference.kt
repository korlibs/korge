package korlibs.korge.test

import korlibs.io.test.*

// Use ./gradlew jvmTestFix to update these files
fun assertEqualsFileReference(path: String, content: String) {
    assertEqualsJvmFileReference(path, content)
}