---
permalink: /targets/android/
group: targets
layout: default
title: "Android"
title_prefix: KorGE Targets
fa-icon: fa-mobile
priority: 30
#status: new
---

The Android target uses the Kotlin JVM. It consumes and generates intermediate `.class` files,
to end generating portable Android `APK`, or Android `AAR` packages
with no external dependencies, nor native per-platform native code, also supports
proguard, so the resulting application is really small.

## Executing

Using gradle tasks on the terminal.

### To installs an APK on all the connected devices (debug/release variants)

Even if cannot install it, it generates the APK file available in `build/platforms/android/build/...`

```bash
./gradlew installAndroidDebug   
./gradlew installAndroidRelease 
```

### To run the application in an available emulator/device (debug/release variants) 

```bash
./gradlew runAndroidDebug          
./gradlew runAndroidEmulatorDebug  
./gradlew runAndroidDeviceDebug    

./gradlew runAndroidRelease         
./gradlew runAndroidEmulatorRelease 
./gradlew runAndroidDeviceRelease   
```

Triggering these tasks, it generates a separate android project into `build/platforms/android`.
You can open it in `Android Studio` for debugging and additional tasks. The KorGE plugin just
delegates gradle tasks to that gradle project.

## Packaging

To generate AAR package files to upload the store:

```bash
./gradlew bundleAndroid
./gradlew bundleDebug
./gradlew bundleRelease
```

## Installing and using the Android SDK

This target requires a separate installation of the Android SDK.
When installed with Android Studio it is usually detected directly, but you can use
the `ANDROID_SDK` environment variable, or the `sdk.dir` on the `local.properties` file.

## Setting Android API Level

In the case you need to change the android API level, you can do that by changing the `build.gradle.kts` file:

```kotlin
korge {
	androidMinSdk = 16
	androidCompileSdk = 28
	androidTargetSdk = 28

	// Shortcut to change all of them at once
	androidSdk(compileSdk = 28, minSdk = 16, targetSdk = 28)
}
```
