## Using with gradle

Requires `Gradle {{ site.data.versions.gradle }}` (`JVM 11~17`) for building and `Kotlin >={{ site.data.versions.kotlin }}` for running:

`build.gradle.kts`

```kotlin
val {{ include.name }}Version = "{{ site.data.versions[include.name] }}"

// For multiplatform projects
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("com.soywiz.korlibs.{{ include.name }}:{{ include.name }}:${{ include.name }}Version") 
            }
        }
    }
}

dependencies {
    // For JVM only
    implementation("com.soywiz.korlibs.{{ include.name }}:{{ include.name }}-jvm:${{ include.name }}Version") 
    // For Android only
    implementation("com.soywiz.korlibs.{{ include.name }}:{{ include.name }}-android:${{ include.name }}Version") 
    // For JS only
    implementation("com.soywiz.korlibs.{{ include.name }}:{{ include.name }}-js:${{ include.name }}Version") 
}

```
