# Animora Studio

Animora Studio is the Android-first animation workspace shipped as the sandbox application in this repository. It is built on KorGE so the rendering and animation contracts remain portable while the Android entry point can evolve independently.

## Current vertical slice

- Original Animora brand system, icon, dark studio theme, and accessible text-labelled tool rail
- Drawing canvas with pointer/stylus-compatible mouse input and live stroke rendering
- Pencil, ink, brush, eraser, fill, transform, selection, and type tool contracts
- Brush size controls and project color swatches
- Layer inspector with paint/camera layers
- Exposure timeline with frame stepping, play state, onion-skin affordance, and keyframe-like blocks
- Project and timeline domain models isolated from UI in `AnimoraCore.kt`
- Android-safe package label, app icon, and a small invariant test suite

## Run

```shell
./gradlew :korge-sandbox:androidApp:assembleDebug
./gradlew :korge-sandbox:runJvm
```

The product is intentionally being built as vertical slices rather than a collection of inactive controls. The next seams are `ProjectStore` for versioned `.animora` persistence, a tile-backed `RenderBackend`, and timeline tracks for audio/camera/keyframes. These interfaces are already kept separate from the scene UI so those systems can be added without rewriting the workspace.
