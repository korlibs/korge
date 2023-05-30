---
layout: default
title: "Changelog"
title_prefix: KorGE
fa-icon: fa-newspaper
priority: 10000
status: outdated
---

{% include toc_include.md %}

## [1.13.1.0 (2020-06-11)](https://github.com/korlibs/korge-plugins/releases/tag/1.13.1.0)

### KorIM:
* Optimized ColorTransform to prevent recomputing when values not changed
* Implemented remaining SVG path commands: `T` and `A` (arcs)
* Some SVG path fixes

### KorGE:
* Support an additional XML-based atlas format
* Adds `Ellipse` View (similar to `Circle`)
* Fixes `View.onCollisionShape`
* Unifies hitTest and supports hitShape on all the cases
* Added `View.hitShape { }` extension extension using the buildPath builder
* TiledMap supports image and object layers visualization
* TiledMap supports point objects
* TiledMap fixes object layer position with decimals
* TiledMap sets properties: name, position, alpha, visibility, type, custom props and rotation

## [1.13.0.1 (2020-06-09)](https://github.com/korlibs/korge-plugins/releases/tag/1.13.0.1)

* Fixes issue with `lineWidth` + scale context when `stroke` on all the targets except JVM (that have fixed). Now it should be consisent with JS's context2d
* Fiixed an issue with Macos that caused inconsistent and super fast framerate

## [1.13.0.0 (2020-06-08)](https://github.com/korlibs/korge-plugins/releases/tag/1.13.0.0)

* Reduced micro stuttering
* Support `addHrUpdater` with microsecond precission
* Improved `readAtlas`
* Allow to construct a `SpriteAnimation` from an `Atlas` with `Atlas.getSpriteAnimation`
* Allow to get mouse button on `onClick` and other mouse events
* Fixed `Circle` and `Graphics` anchor that was ignored
* Added `View.hitShape: VectorPath?`
* `Graphics.hitTest` now uses the defined shapes instead of bounding box by default
* Added `collidesWithShape` that uses shape information to check if two or more views are colliding
* Fixes a few issues with Tillesets
* Now tilesets margin and spacing is taking into account
* Fixes a render bug with tic-tac-toe-swf sample. `strokeWidth` was not being scaled with the current transform

## [1.12.14.0 (2020-06-04)](https://github.com/korlibs/korge-plugins/releases/tag/1.12.14.0)

* `Text` hitTest implementation to be able to add mouse events to Text views
* `Text` .textSize, .font and .color extensions simplifying its format usage

## [1.5.6.2 (2020-02-09)](https://github.com/korlibs/korge-plugins/releases/tag/1.5.6.2)

* Some work on <https://github.com/korlibs/korge.soywiz.com/issues/1>

## [1.5.4.0 (2020-01-12)](https://github.com/korlibs/korge-plugins/releases/tag/1.5.4.0)

* This version fixes a few issues (`ColorMatrixFilter`, issues with PNGs with bpp < 8)
* Support icons in most targets
* Do not require a `launchImmediately` in the main
* Includes a `androidPermission`  and `supportVibration`  to the gradle plugin
* Includes lots of new api documentation
* Includes box2d `convertPixelToWorld` and `convertWorldToPixel`
* Include `Filter` api adjustments (mostly to simplify implementors) and deprecates the old `EffectView`s  (now only `Filter`s should be used)
* Supports tweening / interpolating `TimeSpan`
* Includes new code from korim including the new `buildShape` API

## 1.2.0 (2019-03-17)
{:#v120}

This version includes a preview of KorGE-3D 🎉.

KorGE 3D is a new extension of KorGE allowing to mix 3D and 2D content, define 3D scenes with a DSL and load Collada
scenes and models.

The code is hold in the korge main repository as a library, but their classes are marked as experimental until stabilized:
<https://github.com/korlibs/korge/tree/master/korge-3d>
 
Please, try it out, and give feedback! :) PRs are also welcome.

Check the sample here: <https://github.com/korlibs/korge-samples/tree/master/sample-3d>

### Highlights

* [New plugin system](/korge/plugin/): Now it is possible to create custom resource processors in your libraries and define android 
* [New experimental KorGE-3D](/korge/3d). You can include it with `korge { supportExperimental3d() }` in your `build.gradle`

### New Features

* [Klock 1.3.1: Now supports generating local dates for several languages](https://github.com/korlibs/klock/pull/29) [[2]](https://github.com/korlibs/klock/pull/30)
* [Kds: FastIdentityMap and IndexedTable classes](https://github.com/korlibs/kds/commit/4556965209409642ce4b658106a3b032866da1b6)  
* [Korma: Added Quaternion and EulerRotation](https://github.com/korlibs/korma/commit/2793e3eda7bdb9c583bab4ee785bc938024556cc) [[2]](https://github.com/korlibs/korma/commit/c022f03873b27192ef0cb9d1eb38f5e75be48b13)
* [Korma: Added (next/prev/is)MultipleOf tools](https://github.com/korlibs/korma/commit/c022f03873b27192ef0cb9d1eb38f5e75be48b13)

### Fixes

* [Klock: Fixes formatting with negative offsets](https://github.com/korlibs/klock/commit/f91b95008c731504d8e382492e59429cf45343ae)
* Some viewport with retina display, and fixes for preventing scroll and pinching issues on iOS 

### Performance improvements

* [Korio: Greatly improved performance of Xml parsing](https://github.com/korlibs/korio/commit/12289d91c9886c4e5d0903c2f3234ac68c0c0cbf) [[2]](https://github.com/korlibs/korio/commit/d4e28b87234a5374a2f6139c58e16c9a014a5b1d) [[3]](https://github.com/korlibs/korio/commit/c2a6a40ad344c6492c935f37375746dca7b7a45b)

### Improvements

* [Korio: New methods for Xml reading and StrReader](https://github.com/korlibs/korio/commit/9492924357d4d6c811153817d443e350d50f21c6)
* [Korma: Added Matrix3D setColumns*, setRows* variants using FloatArray](https://github.com/korlibs/korma/commit/c022f03873b27192ef0cb9d1eb38f5e75be48b13)
* [Korgw: Support multisampling in the JVM and Native MacOS](https://github.com/korlibs/korui/commit/f3fae3f7b3f41cd350f5ec50ef735fb7a3c73151)
* [Korag: Support uniform matrix arrays](https://github.com/korlibs/korui/commit/adf464a2c17be5d5f1fd6e90637a21ff33a5f577)
* [Korim: Tools for converting RGBA to Vector3D](https://github.com/korlibs/korim/commit/c013e8131daadaf0ba622ab462aae35f97e8af59)

## 1.1.3 (2019-03-03)
{:#v113}

This version is focused on performance and also fixes a few bugs.

### Fixes

* [KorIM: Fixes bounds computing when using thickness in vector graphics](https://github.com/korlibs/korge/issues/17) [[Fix]](https://github.com/korlibs/korim/commit/0f880ddf3c65797e9ea6528c14f0e4b0b1df6b6b)
* [KorAU: Now supports volume and panning properties on JS](https://github.com/korlibs/korau/commit/092f861e65ed875bbd40f7cd36f71b615fcc3ece)
* [KorAU: Fixes audio leak on windows that produced crashes after a few audio playings](https://github.com/korlibs/korau/commit/88bb7120694c0542a9c67c645ec023ef3660a47b)
* [KorGE: Fixes Android and iOS when using preprocessed resources, and android proper escaping when generating local.properties with the sdk path](https://github.com/korlibs/korge/commit/f5980c92afa228bbf8d56850d1cb934f3eac5283)

### Optimizations

* [KMEM: Reduces allocation on JVM when copying arrays](https://github.com/korlibs/kmem/commit/3bbc9d030738c699bb8c3c360553d84619183900)
* Korio: Greatly reduces allocation pressure on all targets when using Signal and AsyncSignal [[1]](https://github.com/korlibs/korio/commit/b757d5b6e47e7d9790465fb7b2cbe35578b64b83) [[2]](https://github.com/korlibs/korio/commit/0e43f7d17a122ae6d78ebf0d48036b8d9f61150a) [[3]](https://github.com/korlibs/korio/commit/58a489cf4f87bc6e2093cfcf2d962a0472f812f2)
* [Korio: General optimizations reducing allocations](https://github.com/korlibs/korio/commit/c23d736d8111b93b7f57bc46f85824a01f24ac5d)
* KorGW and KorEV: Allocation optimizations [[1]](https://github.com/korlibs/korau/commit/88bb7120694c0542a9c67c645ec023ef3660a47b) [[2]](https://github.com/korlibs/korui/commit/eb69c417f20bd5527eaef7c955449aeb4090b5f3)
* [KorGE and KorGE Dragonbones optimizations reducing allocations when iterating](https://github.com/korlibs/korge/commit/7dc52f449a73c2fba0117b9632cc8932235216c8)
* [KorGE Plugin patches kotlin.js to improve isInheritanceFromInterface performance](https://github.com/korlibs/korge/commit/89b70963122fe54298431d5413d78fc6fff35a27)

### Changes

* [KorAU: Now uses joau on the JVM for audio, fixing performance problems when playing lots of sounds](https://github.com/korlibs/korau/commit/abf3388bb7e738e7da138c5fc489d3e771fbb57b)

### New Features

* [KorIM: Support encoding windows ICO and BMP files](https://github.com/korlibs/korim/commit/87a8aa23a4c47ef1abafbee4b5b06a5662996a0f)
* [KDS: Added `getCyclic` variants for Two Dimensional structures](https://github.com/korlibs/kds/commit/13caf27a291771a494b11529c91ddfb3a20cf8d5)
* KDS: Added allocation-free `fastForEach`, `fastForEachReverse`, `fastIterateRemove`, `fastForEachWithIndex` [[1]](https://github.com/korlibs/kds/commit/4faaea12859e8d3ab6b2680538ea6c88322e7c27) [[2]](https://github.com/korlibs/kds/commit/4ebe19c598e5e2099a931b360ea5a4a1970a9928)
* KDS: Added allocation-free `fastKeyForEach`, `fastValueForEach`, `fastForEach` for `Fast*Map<T>` and `IntMap` variants [[1]](https://github.com/korlibs/kds/commit/b14e781fba8147faa7da889969e9186d05633809) [[2]](https://github.com/korlibs/kds/commit/1c51ff2321253c041ccd6fe397394c173a926b76)
* [Korio: Added String.eachBuilder](https://github.com/korlibs/korio/commit/c23d736d8111b93b7f57bc46f85824a01f24ac5d)
* [KorAU: Support panning property in AudioChannel with values between -1 and +1](https://github.com/korlibs/korau/commit/092f861e65ed875bbd40f7cd36f71b615fcc3ece)
* [KorGE: Tilemaps now support repeating modes (NONE, REPEATING and MIRROR) per axis](https://github.com/korlibs/korge/commit/e7338a1825dd41942cfc0584fa6c6dee70213dab)
* [KorGE: Tilemaps now support using IntArray2 from Kds](https://github.com/korlibs/korge/commit/45a2b8bba1e39bc8ae94275591827457ec4e848a)
* [KorGE: Added color parameter when building text views](https://github.com/korlibs/korge/commit/e0e2a5c5219224be5d932e0343dcdd194f8ace57)

### Extra

* Kotlin [1.3.30-eap-1 was released](https://discuss.kotlinlang.org/t/kotlin-1-3-30-early-access-preview/11780/2) including async stacktraces.
You can use the 1.3.30-eap-1 IDE version along any version of KorGE to debug with asynchronous stacktraces as long as you use the gradle plugin.


## 1.1.1 (2019-02-19)
{:#v111}

### Improvements

* [KorIM: Native antialiased vector rendering on iOS and macOS](https://github.com/korlibs/korim/compare/aa4f37d5884696613c1eb1fa006fdf599d454be5..34dfdd8eddad137aa132020e6ad909ded5970376)
* [KorAU: Support 8-bits per sample WAV files](https://github.com/korlibs/korau/commit/ff6c16cf24903582d563b6fe3e5340a34d0f1eb6)
* [KorGE: Improved Graphics view supporting strokes and optimizing rendering](https://github.com/korlibs/korge/commit/cc228e3ebb8a26d03aa63ce254c24c5e7b743639)
* [KorGE: Make ViewsForTesting as fast as possible when testing tweens or time-related tasks](https://github.com/korlibs/korge/commit/4b28e49f04f3798b4e458c438c2514ed38ce76b1)
* [KorGE plugin: support mp3 and ogg on lipsync](https://github.com/korlibs/korge/commit/81834cd3c6e8ac61dcbafcf30490896988fe8491)

### Optimizations

* [KorGE: Fixed scene not being able to launch new coroutine jobs except when using the dispatcher of GameWindow](https://github.com/korlibs/korge/commit/4b28e49f04f3798b4e458c438c2514ed38ce76b1)
* [KorIO: Improves performance of Json decoding on JS](https://github.com/korlibs/korio/commit/a9ce16dde72929986f5005060ce8b076f61ba69e)
* [KorIO: Reduces allocations when decoding JSON](https://github.com/korlibs/korio/commit/974421cb5b0628356e2966243a8fa4fc7c493971)
* [KorIM: Optimizes readBitmap](https://github.com/korlibs/korim/commit/aa4f37d5884696613c1eb1fa006fdf599d454be5)
* [KorIM buffer vector rendering](https://github.com/korlibs/korim/compare/aa4f37d5884696613c1eb1fa006fdf599d454be5..34dfdd8eddad137aa132020e6ad909ded5970376)
* [KorGE dragonbones: Small optimizations](https://github.com/korlibs/korge/commit/20b1d905bf9cb0bc4f46c4fec00f072254041a63)
* [KorGE: ViewsForTesting fixes](https://github.com/korlibs/korge/commit/4b28e49f04f3798b4e458c438c2514ed38ce76b1)

### Fixes

* [KorGE: Fixes `runJs` on windows and linux](https://github.com/korlibs/korge/commit/e22f8e92bca783ae91f30a0497267844459f5b2b)
* [KorMA: Fixed BoundsBuilder with negative numbers](https://github.com/korlibs/korma/commit/336204b2e28fbf22b7f08a278ded98bcb6b17514)
* KorIO: Fixes Regression that prevented JS to read images and other resources directly from URLs [1](https://github.com/korlibs/korio/commit/b29f5517e1da8f22919aaf77a493341aafbdbc7a) [2](https://github.com/korlibs/korio/commit/95b00778703162e86653ec80be9393eb5fb06449) 
* [KorGW: JVM: Fixes window dimensions and mouse coordinates](https://github.com/korlibs/korui/commit/4d2643f9516ab7426e970c0b15567fa6b301c4d1)
* [KorGE: Misc ResourceProcessor fixes](https://github.com/korlibs/korge/commit/c21996a7fd6827df9e93a0306379b9b994be3f19)
* [KorGE plugin: Fix atlas packing](https://github.com/korlibs/korge/commit/2ebb52e1fa67a996d5555f661b802e6e508716d8)

## 1.1.0 (2019-02-11)
{:#v110}

### New features

* Supported resources processing at build time. Initially: Atlas, SWF and Lipsync
* Added `runJs` gradle task that starts a web server and opens a browser
* Gradle metadata is not required anymore
* Redistributed gradle tasks in korge-* categories

### Improvements

* [KorMA: Makes Angle comparable and adds additional methods](https://github.com/korlibs/korma/commit/5a4476d39ba06c56e7b97224d2b4e75f3c292002)
* [KorIO: tries to figure out resources folders in submodules in intellIJ](https://github.com/korlibs/korio/commit/4b0fcb941271b9724ee93475437ba1a8578b15bd)
* [KorIM: supports suggesting premultiplied when loading native images](https://github.com/korlibs/korim/commit/fa19c274578c2f8465145d78038dc33d1a0177fc)
* [KorGE: improved lipsync API](https://github.com/korlibs/korge/commit/b6fcbf899c00881d87090efc144db7e00e3c9f4a) [2](https://github.com/korlibs/korge/commit/a6cf0e82bd2ca89cdc61a9ec237a445b06ec8d92)
* [KorGE: prevent errors from crashing the application](https://github.com/korlibs/korge/commit/e00f67ef489abe7f7db1657f52277a9b7a55bed5)

### Fixes

* [KorIO: Fixed handing when listing local files on the JVM](https://github.com/korlibs/korio/commit/1d8547e54bae7a899d6195f282c6e63d157d358a)
* [KorIO: Fixed ObjectMapper with jvmFallback](https://github.com/korlibs/korio/commit/afe47716e9cf72d7f49eea2178210dd39a0309c8)
* [KorEV/KorGW: Support drag events on JavaScript](https://github.com/korlibs/korui/commit/1f84e9f2f15082ad0b681679fa17f6fab196fc45)
* [KorEV/KorGW: Support drag events on Windows Native](https://github.com/korlibs/korui/commit/db5900e2c6e525cb65d1464fc009d4157006876b)
* [KorGW: Several fixes on GameWindow including iOS (that stopped working) and other native targets](https://github.com/korlibs/korui/commit/5a8485031b4a1053dd0029276e83b971290b5a78)
* [KorGE plugin: Fixed generated Android build.gradle that was not properly escaping in windows](https://github.com/korlibs/korge/commit/6e48ed85d678a95eb3520ff9f69ab091fbd8d1da)

### Other

* [KorGE: Added View.tweenAsync](https://github.com/korlibs/korge/commit/a3faf4e04b7cc4da5ccc34331631083a016719dd)
* Added several more KorGE samples

## 1.0.3 (2019-02-03)
{:#v103}

### Fixes

* Fixed Bitmap32.hashCode that was causing everything to be super slow
* Fixed red-blue swapping on KorIM windows native decoder 
* Fixed KorGE gradle plugin extension icon property being immutable

## 1.0.0 (2019-02-01)
{:#v100}

First public version

