---
layout: default
title: Korlibs
fa-icon: fa-home
permalink: /
useMermaid: true
---

<img src="/i/logos/korlibs.svg" width="64" height="64" style="float:left;margin-right:16px;"/>

**Korlibs** is a set of Kotlin Common modern libraries to do full stack development aiming Kotlin 1.3.

It stands for **K**otlin c**OR**outines **LIB**raries. Though not all its library components have asynchronous parts.

It is suitable for building [backend servers or cli tools that run on JVM or Node.JS](/korio) and [desktop, web and mobile games](/korge), including [**emulators**](https://github.com/kpspemu/kpspemu), and [GUI applications](/korui).

It's philosophy is: simple, small, powerful, cohesive, enjoyable, portable, multiplatform and asynchronous.

Some of these libraries are tightly tied to provide an awesome experience.

<pre class="mermaid">
flowchart TD
kbignum["fa:fa-infinity kbignum"]
klock["fa:fa-clock klock"]
krypto["fa:fa-lock krypto"]
klogger["fa:fa-newspaper klogger"]
korte["fa:fa-align-justify korte"]
kds["fa:fa-table kds"]
korinject["fa:fa-network-wired korinject"]
kmem["fa:fa-memory kmem"]
korio["fa:fa-save korio"]
korau["fa:fa-music korau"]
korim["fa:fa-image korim"]
korma["fa:fa-calculator korma"]
korge["fa:fa-gamepad korge"]

kotlinx.coroutines --> korcoroutines

korcoroutines --> korio
klock --> korio
kds --> korio
kmem --> korio
krypto --> korio
klogger --> korio

kds --> korma
jna --> kmem

%% ktruth --> korim
korma --> korim
korio --> korim

korim --> korgw

korgw --> korge
korau --> korge
korinject --> korge
korte --> korge

korma --> korau
korio --> korau

click kbignum href "/kbignum/"
click kds href "/kds/"
click klock href "/klock/"
click kmem href "/kmem/"
click krypto href "/krypto/"
click klogger href "/klogger/"
click korma href "/korma/"
click korio href "/korio/"
click korau href "/korau/"
click korim href "/korim/"
click korinject href "/korinject/"
click korte href "/korte/"
click korge href "/korge/"

</pre>
