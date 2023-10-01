---
permalink: /preferences/
group: reference
layout: default
title: "Preferences"
title_prefix: KorGE
fa-icon: fa-save
priority: 300
---

To have persistent preferences/settings that are persisted between game calls, similar to android's `SharedPreferences`,
you can either use files to store them, or use the NativeStorage functionality provided by KorGE.

## `applicationDataVfs`

`applicationDataVfs` is a `LocalVfs` that will point to a folder or virtual filesystem for storing
persistent files four our specific application/game.

So for example you can:

```kotlin
val myJsonString = applicationDataVfs["prefs.json"].readString()
applicationDataVfs["prefs.json"].writeString(myJsonString)
```

## NativeStorage

`NativeStorage` in its basics, acts as a `MutableMap<String, String>` that is automatically persisted.
You can get an instance with `views.storage`.

### Getting an instance

```kotlin
val storage: NativeStorage = views.storage
```

### Checking if a key is defined

```kotlin
storage.contains("key") //
"key" in storage
```

### Getting the value for a key

```kotlin
val value: String = storage["key"] // or KeyNotFoundException thrown
val value: String? = storage.getOrNull("key")
```

### Setting the value of a key

```kotlin
storage["hello"] = "world"
```

### Removing a pair key/value

```kotlin
storage.remove("key")
storage.removeAll() // This will clear all the preferences!
```

### Getting all the defined keys

```kotlin
val keys = storage.keys()
val map: Map<String, String?> = storage.toMap()
```

## NativeStorage StorageKey

`StorageKey` is a simplified way to store string and non-string values and access them easily.

```kotlin
val itemString = storage.itemString("keyString", default = "")
val itemBool = storage.itemBool("keyBool", default = false)
val itemInt = storage.itemInt("keyInt", default = 0)
val itemDouble = storage.itemDouble("keyDouble", default = 0.0)

itemDouble.isDefined // false
println(itemDouble.value) // 0.0
itemDouble.value = 10.0
println(itemDouble.value) // 10.0
itemDouble.isDefined // true
```

It can also be used as a delegated property:

```kotlin
var itemBool by storage.itemBool("keyBool", default = false)
itemBool // false
itemBool = true
itemBool // true
```
