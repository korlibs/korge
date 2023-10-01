---
permalink: /targets/ios/
group: targets
layout: default
title: "iOS"
title_prefix: KorGE Targets
fa-icon: fa-mobile
priority: 40
#status: new
---

The iOS target uses Kotlin/Native.
Generates a small application with additional dependencies.
It uses OpenGL (future versions will use metal) for rendering,
and CoreAudio for audio output.

It includes a thin Objective-C wrapper
for the entry point of the application so it doesn't have to include the Swift runtime on
older iOS versions.



## Executing

```bash
./gradlew iosRunSimulatorDebug       # Runs the APP in the simulator
```

## Packaging

```bash
./gradlew iosBuildSimulatorDebug     # Creates an APP file
./gradlew iosInstallSimulatorDebug   # Installs an APP file in the simulator
```

The `build/platforms/ios` should be created with an XCode project.
You can open the project with XCode to properly package, sign and upload
your application to the iOS AppStore.

## Installing XCode

The iOS target requires XCode and the iOS SDKs to be installed to work properly.
You can install XCode from the MacOS App Store.
