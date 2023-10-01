---
permalink: /templates/syntax/
group: templates
layout: default
title: Template Syntax
title_short: Syntax
description: Ths syntax of KorTE is similar to liquid and other template engines.
fa-icon: fas fa-code
priority: 10
---

{% raw %}

KorTE has two kind of markers:
* `{% block_marker %}`
* `{% block %}...{% endblock %}`
* `{{ expression_marker }}`.

Comment:
* `{# This is a comment #}`

Space trimming:

* Left: `{%- block_marker %}`
* Right: `{% block_marker -%}`
* Both: `{%- block_marker -%}`

Expressions:

* Binary operators: `+`, `-`, `*`, `/`, `%`, `**`, `&`, `|`, `^`, `AND`, `OR`, `&&`, `||`, `==`, `!=`, `<=`, `>=`, `<`, `>`, `<=>`, `in`, `?:`, `..`
* Unary operators: `+`, `-`, `~`, `!`, `NOT`
* Ternary operator: `?`, `:`
* String Literals: `"abc"` or `'abc'`
* Array Literals: `[1,2,3,4]`
* Object Literals: `{"k1": "v1", "k2": "v2"}`
* String Interpolation *(not implemented in 1.0.0)*: `"hello #{name}"`
* Array Access: `list[0]`, `map["key"]`
* Filter invoke: `expr|myfilter` or `expr|myfilter(arg1, arg2, ...)`
* Property Access: `myobj.key` (if key is a property or a suspend function it will be called)
* Function/Method call: `myobj.method(arg1, arg2...)` (if `method` is suspend will be called normally)

{% endraw %}
