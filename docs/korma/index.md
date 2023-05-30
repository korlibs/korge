---
layout: default
title: "KorMA"
fa-icon: fa-calculator
children: /korma/
priority: 50
---

<img src="/i/logos/korma.svg" width="128" height="128" alt="KorMA" style="float:left; margin: 0 16px 16px 0;" />

Korma is a mathematic library for multiplatform Kotlin 1.3 mostly focused on geometry.

[https://github.com/korlibs/korma](https://github.com/korlibs/korma)

{% include stars.html project="korma" %}

[![Build Status](https://travis-ci.org/korlibs/korma.svg?branch=master)](https://travis-ci.org/korlibs/korma)
[![Maven Version](https://img.shields.io/github/tag/korlibs/korma.svg?style=flat&label=maven)](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22korma%22)

{% include toc_include.md %}

## Pages

{% include toc.html context="/korma/" description=true %}

{% include using_with_gradle.md name="korma" %}

## Clipper & poly2tri

```
// Additional funcionality using Clipper and poly2try code (with separate licenses):
// - https://github.com/korlibs/korma/blob/master/korma-shape/LICENSE
dependencies {
    implementation "com.soywiz.korlibs.korma:korma-shape:$kormaVersion"
}
```
