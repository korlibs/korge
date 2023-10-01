---
layout: default
title: "Testing"
title_prefix: Klock
description: "TimeProvider and testing"
fa-icon: fa-vial
priority: 40
---

Klock has utilities for providing time and there are some integrations for testing.

## TimeProvider

Sometimes you will need a source of time that can be mocked. Klock includes a `TimeProvider` interface with a default implementation using `DateTime`.

## Testing & Kotest

Kotest is a flexible and comprehensive testing tool for Kotlin with multiplatform support.
It supports Klock adding additional matchers. For a full list of Klock Kotest matchers, check this link:
<https://github.com/kotest/kotest/blob/master/doc/matchers.md>

And you can find a sample here: <https://github.com/kotest/kotest/tree/master/kotest-assertions/kotest-assertions-klock>

