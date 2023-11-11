---
permalink: /injector/
group: reference
layout: default
title: "Injector"
fa-icon: fa-puzzle-piece
priority: 970
artifact: 'com.soywiz.korge:korge-foundation'
package: korlibs.inject
---

Portable Kotlin Common package to do dependency injection.

## Creating a new injector

An injector allow to hold instances, singletons and class constructions, so it allows to access instances later.

```kotlin
val injector = Injector()
```

### Creating a child injector

A child injector, will be able to access/create instances created by parent and ancestor injectors,
but all the new mappings will be limited to the child injector.

```kotlin
val injector = injector.child()
```

## Mappings

Before getting or constructing instances, it is mandatory to map some instances, singletons or prototypes.

### Instances

If you want to save an already constructed instance for later usage, you can map an instance like this:

```kotlin
injector.mapInstance(myInstance)
// or
injector.mapInstance<MyInstanceClass>(myInstance)
```

Instances are useful for configurations.

### Singletons

If you want to construct a singleton, in a way that all its dependencies are resolved automatically, you can use a singleton.
A singleton will create a single instance per injector once, lazily when first requested.

```kotlin
injector.mapSingleton<MyClass> { MyClass(get(), get(), get(), ...) }
```

Depending on the number of constructor parameters, it is needed to provide the exact number of `get()`.
Singletons are useful for services.

### Prototypes

If you want to construct a new object every time a specific type instance is requested, you can map prototypes.
Similarly to singletons:

```kotlin
injector.mapPrototype<MyClass> { MyClass(get(), get(), get(), ...) }
```

## Getting instances

Once the injector has been configured, you can start to request instances.
If the requested class was mapped as an instance, the provided instance will be returned,
if the requested class was mapped as a singleton, a new instance will be created once, cached, and returned every time.
And if the requested class was mapped as a prototype, a new class will be constructed and returned every time.

```kotlin
val instanceOrThrow: MyClass = injector.get<MyClass>()
val nullable: MyClass? = injector.getOrNull<MyClass>()
```
