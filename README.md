# korge-next

KorGE and all the other korlibs in a single monorepo.

To use this version in other projects,
you have to publish it locally to mavenLocal,
and then use `2.0.0.999` as version: 

```shell script
./gradlew publishToMavenLocal
```

If you want to make changes and easily try things.
You can run the `korge-sandbox` module that runs
the `src/commonMain/kotlin/Main.kt` file;
you can make experiments there:

```shell script
./gradlew :korge-sandbox:runJvm
./gradlew :korge-sandbox:runJs
./gradlew :korge-sandbox:runNativeDebug
./gradlew :korge-sandbox:runNativeRelease
./gradlew :korge-sandbox:runAndroidRelease
./gradlew :korge-sandbox:runIosDeviceRelease
```
